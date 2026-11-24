package com.example.ml

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Base64
import android.util.Log
import com.example.domain.model.ExecutionDevice
import com.example.domain.model.HmrPrediction
import com.example.domain.skeleton.Mhr70Skeleton
import com.example.domain.smoothing.PoseFilter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max

/**
 * Kotlin + ONNX Runtime port of instanthmr/inference.py (InstantHMR).
 * Optimized for real-time mobile execution with multi-threaded XNNPACK, reusable scratch buffers,
 * optional mesh vertex skip (--no-mesh), and explicit NNAPI/XNNPACK fallback detection.
 */
class OnnxInstantHmrEngine(
    networkPath: File,
    bodyPath: File,
    device: ExecutionDevice
) : AutoCloseable {

    companion object {
        private const val TAG = "OnnxInstantHmrEngine"
        private val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
        private const val CROP_EXPAND = 1.2f
        private const val FOCAL_FALLBACK_DIAG = 1.05f
    }

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val networkSession: OrtSession
    private val bodySession: OrtSession

    val inputSize: Int
    val cliffFocal: Boolean
    val nCrops: Int
    val scales: FloatArray
    val faces: IntArray
    val requestedDevice: ExecutionDevice = device
    val actualNetworkDevice: ExecutionDevice
    val providerFallbackReason: String?
    val isProviderFallback: Boolean
        get() = actualNetworkDevice != requestedDevice
    val activeProviderLabel: String

    private val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    // Reusable scratch buffers for zero-GC single-frame hot path
    private var scratchCropBitmap: Bitmap? = null
    private var scratchCropCanvas: Canvas? = null
    private val scratchCropPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val scratchMatrix = Matrix()
    private var scratchArgb: IntArray = IntArray(0)
    private var scratchChannelFloats: FloatArray = FloatArray(0)
    private var scratchImgFloatBuf: FloatBuffer? = null
    private var scratchCliffFloatBuf: FloatBuffer? = null
    private val scratchMhr1Buf: FloatBuffer =
        ByteBuffer.allocateDirect(204 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val scratchShape1Buf: FloatBuffer =
        ByteBuffer.allocateDirect(45 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private data class SessionInitResult(
        val session: OrtSession,
        val actualDevice: ExecutionDevice,
        val fallbackReason: String?
    )

    init {
        val netInit = createNetworkSessionWithFallback(networkPath, device)
        networkSession = netInit.session
        actualNetworkDevice = netInit.actualDevice
        providerFallbackReason = netInit.fallbackReason

        val meta = networkSession.metadata.customMetadata
        inputSize = meta["image_size"]?.toIntOrNull() ?: 288
        cliffFocal = (meta["cliff_focal"] ?: "true") == "true"
        nCrops = meta["crops"]?.toIntOrNull() ?: 0
        scales = if (nCrops > 0 && meta["crop_scales"] != null) {
            meta["crop_scales"]!!.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        } else {
            floatArrayOf(1.0f)
        }

        // Enable multi-threaded XNNPACK on mhr-body.onnx as well (falls back to CPU if unavailable)
        val bodyInit = createBodySession(bodyPath, device)
        bodySession = bodyInit.session
        val bodyDeviceLabel = bodyInit.actualDevice.label

        val facesB64 = bodySession.metadata.customMetadata["mesh_faces"]
            ?: error("mhr-body.onnx missing 'mesh_faces' metadata")
        val rawBytes = Base64.decode(facesB64, Base64.DEFAULT)
        val intBuf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        faces = IntArray(intBuf.remaining())
        intBuf.get(faces)

        val graphTag = if (nCrops > 0) "Fit $nCrops" else "Net"
        activeProviderLabel = when {
            actualNetworkDevice != requestedDevice ->
                "Fallback ${actualNetworkDevice.label} (sem ${requestedDevice.label}) · $graphTag"
            actualNetworkDevice == ExecutionDevice.NNAPI ->
                "NNAPI (rede; malha $bodyDeviceLabel) · $graphTag"
            else ->
                "${actualNetworkDevice.label} · $graphTag"
        }
    }

    private fun createNetworkSessionWithFallback(
        networkPath: File,
        requested: ExecutionDevice
    ): SessionInitResult {
        val candidates = when (requested) {
            ExecutionDevice.NNAPI -> listOf(ExecutionDevice.NNAPI, ExecutionDevice.XNNPACK, ExecutionDevice.CPU)
            ExecutionDevice.XNNPACK -> listOf(ExecutionDevice.XNNPACK, ExecutionDevice.CPU)
            ExecutionDevice.CPU -> listOf(ExecutionDevice.CPU)
        }

        var firstFailure: String? = null
        for (candidate in candidates) {
            val opts = OrtSession.SessionOptions()
            try {
                opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                opts.setIntraOpNumThreads(numThreads)
                when (candidate) {
                    ExecutionDevice.NNAPI -> opts.addNnapi()
                    ExecutionDevice.XNNPACK -> opts.addXnnpack(mapOf("intra_op_num_threads" to numThreads.toString()))
                    ExecutionDevice.CPU -> Unit
                }
                val session = env.createSession(networkPath.absolutePath, opts)
                return SessionInitResult(
                    session = session,
                    actualDevice = candidate,
                    fallbackReason = if (candidate != requested) firstFailure else null
                )
            } catch (t: Throwable) {
                runCatching { opts.close() }
                val reason = t.message?.takeIf { it.isNotBlank() } ?: t::class.java.simpleName
                if (candidate == requested) {
                    firstFailure = "${candidate.label} falhou: $reason"
                }
                Log.w(TAG, "Execution provider ${candidate.label} unavailable: $reason", t)
                if (candidate == ExecutionDevice.CPU) throw t
            }
        }
        error("Failed to initialize ONNX network session")
    }

    private fun createBodySession(bodyPath: File, requested: ExecutionDevice): SessionInitResult {
        val preferXnnpack = requested != ExecutionDevice.CPU
        val candidates = if (preferXnnpack) {
            listOf(ExecutionDevice.XNNPACK, ExecutionDevice.CPU)
        } else {
            listOf(ExecutionDevice.CPU)
        }
        for (candidate in candidates) {
            val opts = OrtSession.SessionOptions()
            try {
                opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                opts.setIntraOpNumThreads(numThreads)
                if (candidate == ExecutionDevice.XNNPACK) {
                    opts.addXnnpack(mapOf("intra_op_num_threads" to numThreads.toString()))
                }
                val session = env.createSession(bodyPath.absolutePath, opts)
                return SessionInitResult(session = session, actualDevice = candidate, fallbackReason = null)
            } catch (t: Throwable) {
                runCatching { opts.close() }
                if (candidate == ExecutionDevice.CPU) throw t
            }
        }
        error("Failed to initialize ONNX body session")
    }

    fun focalPx(h: Int, w: Int, focalOverride: Float?): Float {
        if (focalOverride != null && focalOverride > 0f) return focalOverride
        val mult = if (cliffFocal) FOCAL_FALLBACK_DIAG else 1.0f
        return mult * hypot(h.toFloat(), w.toFloat())
    }

    fun predict(
        bitmap: Bitmap,
        boxes: List<FloatArray>,
        confidences: FloatArray,
        focalOverride: Float?,
        poseFilter: PoseFilter?,
        timestampSec: Float?,
        includeVertices: Boolean = true
    ): List<HmrPrediction> {
        if (boxes.isEmpty()) return emptyList()
        val w = bitmap.width
        val h = bitmap.height
        val f = focalPx(h, w, focalOverride)

        val netOut = if (nCrops > 0) {
            estimateFit(bitmap, boxes, f)
        } else {
            estimateNetwork(bitmap, boxes, f)
        }

        val filteredMhrParams = poseFilter?.filter(boxes, netOut.mhrParams, timestampSec) ?: netOut.mhrParams
        val bodyOut = runBodyGraph(filteredMhrParams, netOut.shapeParams, includeVertices = includeVertices)

        return boxes.indices.map { i ->
            HmrPrediction(
                bbox = boxes[i],
                confidence = confidences.getOrElse(i) { 1f },
                joints2d = netOut.joints2d[i],
                joints3dLocal = bodyOut.joints3d[i],
                camTrans = netOut.camTrans[i],
                mhrParams = filteredMhrParams[i],
                shapeParams = netOut.shapeParams[i],
                focalLength = f,
                imageWidth = w,
                imageHeight = h,
                verticesLocal = bodyOut.vertices[i],
                isOnnxFullMesh = includeVertices
            )
        }
    }

    private data class NetworkBatchResult(
        val mhrParams: Array<FloatArray>,
        val shapeParams: Array<FloatArray>,
        val camTrans: Array<FloatArray>,
        val joints2d: Array<FloatArray>
    )

    private data class BodyBatchResult(
        val joints3d: Array<FloatArray>,
        val vertices: Array<FloatArray>
    )

    private data class CropData(
        val pixelsRgb: ByteArray,
        val cliffCond: FloatArray,
        val squareBox: FloatArray
    )

    private fun ensureScratchCropBitmap(s: Int): Bitmap {
        val existing = scratchCropBitmap
        if (existing != null && !existing.isRecycled && existing.width == s && existing.height == s) {
            return existing
        }
        existing?.recycle()
        val created = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        scratchCropBitmap = created
        scratchCropCanvas = Canvas(created)
        return created
    }

    private fun estimateNetwork(bitmap: Bitmap, boxes: List<FloatArray>, focal: Float): NetworkBatchResult {
        val batch = boxes.size
        val crops = boxes.map { extractCrop(bitmap, it, 1.0f, focal) }
        val s = inputSize
        val hw = s * s

        val neededImgFloats = batch * 3 * hw
        val imgBuf = if (batch == 1) {
            val cached = scratchImgFloatBuf
            if (cached != null && cached.capacity() == neededImgFloats) {
                cached.clear()
                cached
            } else {
                ByteBuffer.allocateDirect(neededImgFloats * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
                    .also { scratchImgFloatBuf = it }
            }
        } else {
            ByteBuffer.allocateDirect(neededImgFloats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        }

        val neededCliffFloats = batch * 3
        val cliffBuf = if (batch == 1) {
            val cached = scratchCliffFloatBuf
            if (cached != null && cached.capacity() == neededCliffFloats) {
                cached.clear()
                cached
            } else {
                ByteBuffer.allocateDirect(neededCliffFloats * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
                    .also { scratchCliffFloatBuf = it }
            }
        } else {
            ByteBuffer.allocateDirect(neededCliffFloats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        }

        val channelBuf = if (scratchChannelFloats.size == 3 * hw) {
            scratchChannelFloats
        } else {
            FloatArray(3 * hw).also { scratchChannelFloats = it }
        }
        val inv255 = 1f / 255f
        val mean0 = IMAGENET_MEAN[0]; val mean1 = IMAGENET_MEAN[1]; val mean2 = IMAGENET_MEAN[2]
        val invStd0 = 1f / IMAGENET_STD[0]; val invStd1 = 1f / IMAGENET_STD[1]; val invStd2 = 1f / IMAGENET_STD[2]
        val hw2 = 2 * hw

        for (b in 0 until batch) {
            val rgb = crops[b].pixelsRgb
            var rgbIdx = 0
            for (p in 0 until hw) {
                val r = (rgb[rgbIdx].toInt() and 0xFF) * inv255
                val g = (rgb[rgbIdx + 1].toInt() and 0xFF) * inv255
                val bl = (rgb[rgbIdx + 2].toInt() and 0xFF) * inv255
                rgbIdx += 3
                channelBuf[p] = (r - mean0) * invStd0
                channelBuf[hw + p] = (g - mean1) * invStd1
                channelBuf[hw2 + p] = (bl - mean2) * invStd2
            }
            imgBuf.put(channelBuf)
            cliffBuf.put(crops[b].cliffCond)
        }
        imgBuf.rewind()
        cliffBuf.rewind()

        val imgTensor = OnnxTensor.createTensor(env, imgBuf, longArrayOf(batch.toLong(), 3, s.toLong(), s.toLong()))
        val cliffTensor = OnnxTensor.createTensor(env, cliffBuf, longArrayOf(batch.toLong(), 3))

        return imgTensor.use { tImg ->
            cliffTensor.use { tCliff ->
                val inputs = mapOf("image" to tImg, "cliff_cond" to tCliff)
                networkSession.run(inputs).use { res ->
                    val mhr = extractBatchFloats(res["mhr_params"].get() as OnnxTensor, batch, 204)
                    val shape = extractBatchFloats(res["shape_params"].get() as OnnxTensor, batch, 45)
                    val cam = extractBatchFloats(res["cam_trans"].get() as OnnxTensor, batch, 3)
                    val j2dRaw = extractBatchFloats(res["joints_2d"].get() as OnnxTensor, batch, 70 * 2)
                    val j2d = Array(batch) { b ->
                        val sq = crops[b].squareBox
                        val out = FloatArray(70 * 2)
                        for (j in 0 until 70) {
                            out[j * 2] = (j2dRaw[b][j * 2] + 1f) * 0.5f * sq[2] + sq[0]
                            out[j * 2 + 1] = (j2dRaw[b][j * 2 + 1] + 1f) * 0.5f * sq[3] + sq[1]
                        }
                        out
                    }
                    NetworkBatchResult(mhr, shape, cam, j2d)
                }
            }
        }
    }

    private fun estimateFit(bitmap: Bitmap, boxes: List<FloatArray>, focal: Float): NetworkBatchResult {
        val batch = boxes.size
        val w = bitmap.width
        val h = bitmap.height
        val camArr = floatArrayOf(focal, w * 0.5f, h * 0.5f)
        val s = inputSize
        val n = scales.size

        val mhrOut = Array(batch) { FloatArray(204) }
        val shapeOut = Array(batch) { FloatArray(45) }
        val camOut = Array(batch) { FloatArray(3) }
        val j2dOut = Array(batch) { FloatArray(70 * 2) }

        for (bIdx in boxes.indices) {
            val b = boxes[bIdx]
            val cx = (b[0] + b[2]) * 0.5f
            val cy = (b[1] + b[3]) * 0.5f
            val halfW = (b[2] - b[0]) * Mhr70Skeleton.DETECTOR_TO_GT_BOX * 0.5f
            val halfH = (b[3] - b[1]) * Mhr70Skeleton.DETECTOR_TO_GT_BOX * 0.5f
            val gtBox = floatArrayOf(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

            val crops = scales.map { scale -> extractCrop(bitmap, gtBox, scale, focal) }
            val imgBytes = ByteBuffer.allocateDirect(n * s * s * 3).order(ByteOrder.nativeOrder())
            val cliffBuf = ByteBuffer.allocateDirect(n * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val boxBuf = ByteBuffer.allocateDirect(n * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            val camBuf = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

            for (c in crops) {
                imgBytes.put(c.pixelsRgb)
                cliffBuf.put(c.cliffCond)
                boxBuf.put(c.squareBox)
            }
            camBuf.put(camArr)
            imgBytes.rewind()
            cliffBuf.rewind()
            boxBuf.rewind()
            camBuf.rewind()

            val imgTensor = OnnxTensor.createTensor(
                env, imgBytes, longArrayOf(n.toLong(), s.toLong(), s.toLong(), 3), OnnxJavaType.UINT8
            )
            val cliffTensor = OnnxTensor.createTensor(env, cliffBuf, longArrayOf(n.toLong(), 3))
            val boxTensor = OnnxTensor.createTensor(env, boxBuf, longArrayOf(n.toLong(), 4))
            val camTensor = OnnxTensor.createTensor(env, camBuf, longArrayOf(1, 3))

            imgTensor.use { tImg ->
                cliffTensor.use { tCliff ->
                    boxTensor.use { tBox ->
                        camTensor.use { tCam ->
                            val inputs = mapOf(
                                "image" to tImg,
                                "cliff_cond" to tCliff,
                                "crop_box" to tBox,
                                "cam" to tCam
                            )
                            networkSession.run(inputs).use { res ->
                                mhrOut[bIdx] = extractBatchFloats(res["mhr_params"].get() as OnnxTensor, 1, 204)[0]
                                shapeOut[bIdx] = extractBatchFloats(res["shape_params"].get() as OnnxTensor, 1, 45)[0]
                                camOut[bIdx] = extractBatchFloats(res["cam_trans"].get() as OnnxTensor, 1, 3)[0]
                                j2dOut[bIdx] = extractBatchFloats(res["joints_2d"].get() as OnnxTensor, 1, 70 * 2)[0]
                            }
                        }
                    }
                }
            }
        }
        return NetworkBatchResult(mhrOut, shapeOut, camOut, j2dOut)
    }

    private fun runBodyGraph(
        mhrParams: Array<FloatArray>,
        shapeParams: Array<FloatArray>,
        includeVertices: Boolean
    ): BodyBatchResult {
        val batch = mhrParams.size
        val mhrBuf = if (batch == 1) {
            scratchMhr1Buf.apply { clear() }
        } else {
            ByteBuffer.allocateDirect(batch * 204 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        }
        val shapeBuf = if (batch == 1) {
            scratchShape1Buf.apply { clear() }
        } else {
            ByteBuffer.allocateDirect(batch * 45 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        }
        for (i in 0 until batch) {
            mhrBuf.put(mhrParams[i])
            shapeBuf.put(shapeParams[i])
        }
        mhrBuf.rewind()
        shapeBuf.rewind()

        val mhrTensor = OnnxTensor.createTensor(env, mhrBuf, longArrayOf(batch.toLong(), 204))
        val shapeTensor = OnnxTensor.createTensor(env, shapeBuf, longArrayOf(batch.toLong(), 45))

        return mhrTensor.use { tMhr ->
            shapeTensor.use { tShape ->
                bodySession.run(mapOf("mhr_params" to tMhr, "shape_params" to tShape)).use { res ->
                    val j3dTensor = res["joints_3d"].get() as OnnxTensor
                    val verts = if (includeVertices) {
                        val vertTensor = res["vertices"].get() as OnnxTensor
                        val vCount = vertTensor.info.shape[1].toInt()
                        extractBatchFloats(vertTensor, batch, vCount * 3)
                    } else {
                        Array(batch) { FloatArray(0) }
                    }
                    BodyBatchResult(
                        joints3d = extractBatchFloats(j3dTensor, batch, 70 * 3),
                        vertices = verts
                    )
                }
            }
        }
    }

    private fun extractCrop(bitmap: Bitmap, bbox: FloatArray, scale: Float, focal: Float): CropData {
        val w = bitmap.width
        val h = bitmap.height
        val x1 = bbox[0].toDouble()
        val y1 = bbox[1].toDouble()
        val x2 = bbox[2].toDouble()
        val y2 = bbox[3].toDouble()
        val cx = (x1 + x2) * 0.5
        val cy = (y1 + y2) * 0.5
        val size = max(x2 - x1, y2 - y1) * scale

        val cliff = if (cliffFocal) {
            floatArrayOf(
                atan((cx - w * 0.5) / focal).toFloat(),
                atan((cy - h * 0.5) / focal).toFloat(),
                (size / focal).toFloat()
            )
        } else {
            floatArrayOf(
                (2.0 * cx / w - 1.0).toFloat(),
                (2.0 * cy / h - 1.0).toFloat(),
                (size / max(w, h)).toFloat()
            )
        }

        val side = size * CROP_EXPAND
        val ix1 = floor(cx - side * 0.5)
        val iy1 = floor(cy - side * 0.5)
        val ix2 = ceil(cx - side * 0.5 + side)
        val iy2 = ceil(cy - side * 0.5 + side)
        val cropW = max(1.0, ix2 - ix1)
        val cropH = max(1.0, iy2 - iy1)

        val s = inputSize
        val cropBitmap = ensureScratchCropBitmap(s)
        cropBitmap.eraseColor(0)
        val canvas = scratchCropCanvas!!
        scratchMatrix.reset()
        scratchMatrix.postTranslate(-ix1.toFloat(), -iy1.toFloat())
        scratchMatrix.postScale((s / cropW).toFloat(), (s / cropH).toFloat())
        canvas.drawBitmap(bitmap, scratchMatrix, scratchCropPaint)

        val hw = s * s
        val argb = if (scratchArgb.size == hw) scratchArgb else IntArray(hw).also { scratchArgb = it }
        cropBitmap.getPixels(argb, 0, s, 0, 0, s, s)

        val rgb = ByteArray(hw * 3)
        var dst = 0
        for (i in 0 until hw) {
            val px = argb[i]
            rgb[dst] = ((px shr 16) and 0xFF).toByte()
            rgb[dst + 1] = ((px shr 8) and 0xFF).toByte()
            rgb[dst + 2] = (px and 0xFF).toByte()
            dst += 3
        }

        return CropData(
            pixelsRgb = rgb,
            cliffCond = cliff,
            squareBox = floatArrayOf(ix1.toFloat(), iy1.toFloat(), cropW.toFloat(), cropH.toFloat())
        )
    }

    private fun extractBatchFloats(tensor: OnnxTensor, batch: Int, stride: Int): Array<FloatArray> {
        val buf: FloatBuffer = tensor.floatBuffer
        buf.rewind()
        return Array(batch) {
            FloatArray(stride).also { arr ->
                buf.get(arr, 0, stride)
            }
        }
    }

    override fun close() {
        scratchCropBitmap?.recycle()
        scratchCropBitmap = null
        scratchCropCanvas = null
        runCatching { networkSession.close() }
        runCatching { bodySession.close() }
    }
}

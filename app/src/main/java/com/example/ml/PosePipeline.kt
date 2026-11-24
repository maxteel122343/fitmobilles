package com.example.ml

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.model.FrameResult
import com.example.domain.model.HmrPrediction
import com.example.domain.model.PipelineConfig
import com.example.domain.model.PoseFilterKind
import com.example.domain.skeleton.Mhr70Skeleton
import com.example.domain.skeleton.ProceduralBodyMeshBuilder
import com.example.domain.smoothing.PoseFilter
import com.example.domain.smoothing.TemporalSmoother
import java.io.File
import kotlin.math.hypot

/**
 * Port of instanthmr/pipeline.py (PosePipeline).
 * Executes person detection with configurable detectorStride (using keypointBox tracking in between),
 * runs OnnxInstantHmrEngine when cached Hugging Face / imported .onnx weights are available,
 * or falls back cleanly to on-device ML Kit 3D Pose -> MHR70 + ProceduralBodyMeshBuilder.
 */
class PosePipeline(
    private val modelRepository: ModelRepository
) : AutoCloseable {

    private var streamDetector: PersonDetector? = null
    private var singleDetector: PersonDetector? = null
    private var onnxEngine: OnnxInstantHmrEngine? = null
    private var loadedSignature: String? = null

    private var poseFilter: PoseFilter? = null
    private var temporalSmoother: TemporalSmoother? = null

    private var frameIndex = 0
    private var trackedBoxes: List<FloatArray> = emptyList()
    private var trackedScores: FloatArray = FloatArray(0)
    private var lastFullBodyVisible: Boolean = true

    @Volatile
    var lastActiveProviderLabel: String? = null
        private set

    @Volatile
    var lastFallbackReason: String? = null
        private set

    fun resetTracking() {
        frameIndex = 0
        trackedBoxes = emptyList()
        trackedScores = FloatArray(0)
        lastFullBodyVisible = true
        poseFilter?.reset()
        temporalSmoother?.reset()
    }

    suspend fun processFrame(
        bitmap: Bitmap,
        config: PipelineConfig,
        isStream: Boolean,
        timestampSec: Float
    ): FrameResult {
        syncEngineAndFilters(config)
        val detector = getDetector(isStream)
        val engine = onnxEngine

        val t0 = System.nanoTime()
        val hasValidTracked = trackedBoxes.isNotEmpty() && trackedBoxes.all { b ->
            (b[2] - b[0]) >= 24f && (b[3] - b[1]) >= 24f
        }
        val runDetector = (frameIndex % config.detectorStride.coerceAtLeast(1) == 0) ||
            !hasValidTracked ||
            engine == null

        var fallbackPose: com.google.mlkit.vision.pose.Pose? = null
        if (runDetector) {
            val det = detector.detect(
                bitmap = bitmap,
                confidenceThreshold = config.detConfidence,
                maxPersons = config.maxPersons,
                needPoseFallback = (engine == null)
            )
            trackedBoxes = det.boxes
            trackedScores = det.scores
            fallbackPose = det.primaryPose
            lastFullBodyVisible = det.isFullBodyVisible
        }
        val t1 = System.nanoTime()

        val w = bitmap.width
        val h = bitmap.height
        val focal = config.customFocalPx ?: (1.05f * hypot(w.toFloat(), h.toFloat()))

        var faces = IntArray(0)
        var engineLabel = "ML Kit Preview"

        var persons: List<HmrPrediction> = if (engine != null) {
            faces = if (config.showMesh) engine.faces else IntArray(0)
            engineLabel = "${config.modelVariant.id} (${engine.activeProviderLabel})"
            engine.predict(
                bitmap = bitmap,
                boxes = trackedBoxes,
                confidences = trackedScores,
                focalOverride = config.customFocalPx,
                poseFilter = if (isStream) poseFilter else null,
                timestampSec = timestampSec,
                includeVertices = config.showMesh
            )
        } else {
            val mapped = fallbackPose?.let { Mhr70Skeleton.fromMlKitPose(it, w, h, focal) }
            if (mapped != null) {
                val proxy = if (config.showMesh) {
                    ProceduralBodyMeshBuilder.build(mapped.joints3dLocal)
                } else {
                    null
                }
                faces = proxy?.faces ?: IntArray(0)
                listOf(
                    HmrPrediction(
                        bbox = trackedBoxes.firstOrNull() ?: mapped.bbox,
                        confidence = mapped.confidence,
                        joints2d = mapped.joints2d,
                        joints3dLocal = mapped.joints3dLocal,
                        camTrans = mapped.camTrans,
                        mhrParams = FloatArray(204),
                        shapeParams = FloatArray(45),
                        focalLength = focal,
                        imageWidth = w,
                        imageHeight = h,
                        verticesLocal = proxy?.verticesLocal ?: FloatArray(0),
                        isOnnxFullMesh = false
                    )
                )
            } else {
                emptyList()
            }
        }

        if (isStream && config.smoothOutput) {
            val smoother = temporalSmoother ?: TemporalSmoother(betaScale = config.smoothBetaScale)
                .also { temporalSmoother = it }
            persons = smoother.smooth(persons, timestampSec)
        }
        val t2 = System.nanoTime()

        if (isStream) {
            if (persons.isNotEmpty()) {
                trackedBoxes = persons.map { Mhr70Skeleton.keypointBox(it.joints2d, w, h) }
                // Also evaluate full-body framing from 2D ankle/shoulder keypoints
                val j2 = persons.first().joints2d
                val noseY = j2[1]
                val lAnkY = j2[13 * 2 + 1]
                val rAnkY = j2[14 * 2 + 1]
                val lHipY = j2[9 * 2 + 1]
                val ankSpread = kotlin.math.max(lAnkY - lHipY, rAnkY - lHipY)
                if (runDetector) {
                    // keep detector's accurate visibility check, or check bounds
                    lastFullBodyVisible = lastFullBodyVisible && noseY >= 0f && ankSpread > 12f
                }
            } else {
                // Force immediate re-detection on next frame if tracking lost
                trackedBoxes = emptyList()
            }
        }

        val currentIdx = frameIndex++
        val detMs = (t1 - t0) / 1_000_000f
        val hmrMs = (t2 - t1) / 1_000_000f

        return FrameResult(
            persons = persons,
            faces = faces,
            detectorMs = detMs,
            hmrMs = hmrMs,
            engineLabel = engineLabel,
            frameIndex = currentIdx,
            timestampSec = timestampSec,
            isFullBodyVisible = lastFullBodyVisible
        )
    }

    private fun getDetector(isStream: Boolean): PersonDetector {
        return if (isStream) {
            streamDetector ?: PersonDetector(streamMode = true).also { streamDetector = it }
        } else {
            singleDetector ?: PersonDetector(streamMode = false).also { singleDetector = it }
        }
    }

    private fun syncEngineAndFilters(config: PipelineConfig) {
        val activeFiles: Pair<File, File>? = modelRepository.resolveActiveModels(
            variant = config.modelVariant,
            fitLevel = config.fitLevel,
            customOnnxPath = config.customOnnxPath
        )
        val sig = if (activeFiles != null) {
            "${activeFiles.first.absolutePath}|${activeFiles.second.absolutePath}|${config.executionDevice.id}"
        } else {
            null
        }

        if (sig != loadedSignature) {
            onnxEngine?.close()
            if (activeFiles != null) {
                val created = runCatching {
                    OnnxInstantHmrEngine(activeFiles.first, activeFiles.second, config.executionDevice)
                }
                onnxEngine = created.getOrNull()
                val err = created.exceptionOrNull()
                if (err != null) {
                    Log.w("PosePipeline", "Failed to initialize OnnxInstantHmrEngine", err)
                    lastActiveProviderLabel = "ML Kit Preview (erro ONNX)"
                    lastFallbackReason = err.message ?: err::class.java.simpleName
                } else {
                    lastActiveProviderLabel = onnxEngine?.activeProviderLabel
                    lastFallbackReason = onnxEngine?.providerFallbackReason
                }
            } else {
                onnxEngine = null
                lastActiveProviderLabel = "ML Kit Preview (sem pesos ONNX)"
                lastFallbackReason = null
            }
            loadedSignature = sig
        }

        if (config.poseFilter == PoseFilterKind.NONE) {
            poseFilter = null
        } else if (poseFilter?.kind != config.poseFilter) {
            poseFilter = PoseFilter(config.poseFilter)
        }

        if (!config.smoothOutput) {
            temporalSmoother = null
        }
    }

    override fun close() {
        streamDetector?.close()
        singleDetector?.close()
        onnxEngine?.close()
    }
}

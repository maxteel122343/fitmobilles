package com.example.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

data class DetectionOutput(
    val boxes: List<FloatArray>,
    val scores: FloatArray,
    val primaryPose: Pose?
)

/**
 * On-device multi-person detector backed by Google ML Kit Pose Detection + Object Detection.
 * When running in on-device preview mode (needPoseFallback = true), runs PoseDetector first
 * for lowest real-time latency and reliable full/upper-body detection.
 */
class PersonDetector(private val streamMode: Boolean) : AutoCloseable {

    private val objectDetector: ObjectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(
                if (streamMode) ObjectDetectorOptions.STREAM_MODE else ObjectDetectorOptions.SINGLE_IMAGE_MODE
            )
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val poseDetector: PoseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(
                if (streamMode) PoseDetectorOptions.STREAM_MODE else PoseDetectorOptions.SINGLE_IMAGE_MODE
            )
            .build()
    )

    suspend fun detect(
        bitmap: Bitmap,
        confidenceThreshold: Float,
        maxPersons: Int,
        needPoseFallback: Boolean
    ): DetectionOutput {
        val image = InputImage.fromBitmap(bitmap, 0)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val candidates = mutableListOf<Pair<FloatArray, Float>>()
        var pose: Pose? = null

        if (needPoseFallback) {
            pose = runCatching { poseDetector.process(image).await() }.getOrNull()
            addPoseCandidate(pose, w, h, candidates)
            if (candidates.isNotEmpty()) {
                return DetectionOutput(
                    boxes = listOf(candidates.first().first),
                    scores = floatArrayOf(candidates.first().second),
                    primaryPose = pose
                )
            }
        }

        val objects = runCatching { objectDetector.process(image).await() }.getOrDefault(emptyList())
        for (obj in objects) {
            val rect = obj.boundingBox
            val boxW = rect.width().toFloat()
            val boxH = rect.height().toFloat()
            if (boxW < 24f || boxH < 24f) continue

            val bestLabel = obj.labels.maxByOrNull { it.confidence }
            val conf = bestLabel?.confidence ?: 0.72f
            val aspect = boxH / max(1f, boxW)
            val areaFrac = (boxW * boxH) / max(1f, w * h)

            val looksLikePersonOrForeground = bestLabel == null ||
                bestLabel.text.equals("Fashion good", ignoreCase = true) ||
                bestLabel.text.equals("Person", ignoreCase = true) ||
                (aspect >= 0.8f && areaFrac >= 0.02f)

            if (looksLikePersonOrForeground && conf >= confidenceThreshold * 0.7f) {
                candidates.add(
                    floatArrayOf(
                        max(0f, rect.left.toFloat()),
                        max(0f, rect.top.toFloat()),
                        min(w, rect.right.toFloat()),
                        min(h, rect.bottom.toFloat())
                    ) to conf
                )
            }
        }

        if (pose == null && candidates.isEmpty()) {
            pose = runCatching { poseDetector.process(image).await() }.getOrNull()
            addPoseCandidate(pose, w, h, candidates)
        }

        val sorted = candidates.sortedByDescending { it.second }.take(maxPersons)
        return DetectionOutput(
            boxes = sorted.map { it.first },
            scores = FloatArray(sorted.size) { sorted[it].second },
            primaryPose = pose
        )
    }

    private fun addPoseCandidate(
        pose: Pose?,
        w: Float,
        h: Float,
        candidates: MutableList<Pair<FloatArray, Float>>
    ) {
        if (pose == null || pose.allPoseLandmarks.isEmpty()) return
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var sumConf = 0f
        var count = 0
        for (lm in pose.allPoseLandmarks) {
            if (lm.inFrameLikelihood < 0.12f) continue
            minX = min(minX, lm.position.x)
            minY = min(minY, lm.position.y)
            maxX = max(maxX, lm.position.x)
            maxY = max(maxY, lm.position.y)
            sumConf += lm.inFrameLikelihood
            count++
        }
        if (count >= 4) {
            val padX = (maxX - minX) * 0.14f
            val padY = (maxY - minY) * 0.10f
            val avgConf = (sumConf / count).coerceIn(0.15f, 0.99f)
            candidates.add(
                floatArrayOf(
                    max(0f, minX - padX),
                    max(0f, minY - padY),
                    min(w, maxX + padX),
                    min(h, maxY + padY)
                ) to avgConf
            )
        }
    }

    override fun close() {
        runCatching { objectDetector.close() }
        runCatching { poseDetector.close() }
    }
}

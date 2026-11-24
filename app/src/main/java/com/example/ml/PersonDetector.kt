package com.example.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

data class DetectionOutput(
    val boxes: List<FloatArray>,
    val scores: FloatArray,
    val primaryPose: Pose?,
    val isFullBodyVisible: Boolean = true
)

/**
 * On-device person detector prioritizing ML Kit PoseDetector for accurate human anatomy
 * and full-body bounding boxes (avoiding false positives from coarse object detection),
 * supplementing with ObjectDetector only for multi-person scenes (maxPersons > 1).
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
        val pose = runCatching { poseDetector.process(image).await() }.getOrNull()
        val fullBodyVisible = addPoseCandidate(pose, w, h, candidates)

        // Single-person fast path: PoseDetector already provides exact human full-body bounds
        if (candidates.isNotEmpty() && (maxPersons <= 1 || needPoseFallback)) {
            return DetectionOutput(
                boxes = listOf(candidates.first().first),
                scores = floatArrayOf(candidates.first().second),
                primaryPose = pose,
                isFullBodyVisible = fullBodyVisible
            )
        }

        // Multi-person or no-pose fallback: run ObjectDetector for additional people
        val objects = runCatching { objectDetector.process(image).await() }.getOrDefault(emptyList())
        for (obj in objects) {
            val rect = obj.boundingBox
            val boxW = rect.width().toFloat()
            val boxH = rect.height().toFloat()
            if (boxW < 28f || boxH < 28f) continue

            val bestLabel = obj.labels.maxByOrNull { it.confidence }
            val conf = bestLabel?.confidence ?: 0.72f
            val aspect = boxH / max(1f, boxW)
            val areaFrac = (boxW * boxH) / max(1f, w * h)

            val looksLikePerson = bestLabel == null ||
                bestLabel.text.equals("Fashion good", ignoreCase = true) ||
                bestLabel.text.equals("Person", ignoreCase = true) ||
                (aspect >= 1.15f && areaFrac >= 0.04f)

            if (looksLikePerson && conf >= confidenceThreshold * 0.75f) {
                val box = floatArrayOf(
                    max(0f, rect.left.toFloat()),
                    max(0f, rect.top.toFloat()),
                    min(w, rect.right.toFloat()),
                    min(h, rect.bottom.toFloat())
                )
                val overlapsPose = candidates.any { (pb, _) -> iou(pb, box) > 0.35f }
                if (!overlapsPose) {
                    candidates.add(box to conf)
                }
            }
        }

        val sorted = candidates.sortedByDescending { it.second }.take(maxPersons)
        return DetectionOutput(
            boxes = sorted.map { it.first },
            scores = FloatArray(sorted.size) { sorted[it].second },
            primaryPose = pose,
            isFullBodyVisible = if (pose != null && candidates.isNotEmpty()) fullBodyVisible else true
        )
    }

    private fun addPoseCandidate(
        pose: Pose?,
        w: Float,
        h: Float,
        candidates: MutableList<Pair<FloatArray, Float>>
    ): Boolean {
        if (pose == null || pose.allPoseLandmarks.isEmpty()) return true
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var sumConf = 0f
        var count = 0

        for (lm in pose.allPoseLandmarks) {
            if (lm.inFrameLikelihood < 0.15f) continue
            minX = min(minX, lm.position.x)
            minY = min(minY, lm.position.y)
            maxX = max(maxX, lm.position.x)
            maxY = max(maxY, lm.position.y)
            sumConf += lm.inFrameLikelihood
            count++
        }
        if (count < 5) return false

        // Check full-body visibility (shoulders + hips + at least one knee & ankle inside frame)
        val hasShoulders = hasVisibleLandmark(pose, PoseLandmark.LEFT_SHOULDER, w, h) ||
            hasVisibleLandmark(pose, PoseLandmark.RIGHT_SHOULDER, w, h)
        val hasHips = hasVisibleLandmark(pose, PoseLandmark.LEFT_HIP, w, h) ||
            hasVisibleLandmark(pose, PoseLandmark.RIGHT_HIP, w, h)
        val hasKnee = hasVisibleLandmark(pose, PoseLandmark.LEFT_KNEE, w, h) ||
            hasVisibleLandmark(pose, PoseLandmark.RIGHT_KNEE, w, h)
        val hasAnkle = hasVisibleLandmark(pose, PoseLandmark.LEFT_ANKLE, w, h) ||
            hasVisibleLandmark(pose, PoseLandmark.RIGHT_ANKLE, w, h)
        val isFullBody = hasShoulders && hasHips && hasKnee && hasAnkle

        // If lower legs aren't visible yet, extend bottom proportionally from torso height so crop isn't tight on chest
        if (hasShoulders && hasHips && !hasAnkle) {
            val lSh = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rSh = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val lHp = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val rHp = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val shY = ((lSh?.position?.y ?: rSh?.position?.y ?: minY) + (rSh?.position?.y ?: lSh?.position?.y ?: minY)) * 0.5f
            val hpY = ((lHp?.position?.y ?: rHp?.position?.y ?: maxY) + (rHp?.position?.y ?: lHp?.position?.y ?: maxY)) * 0.5f
            val torsoH = max(40f, hpY - shY)
            maxY = max(maxY, min(h, hpY + torsoH * 1.45f))
        }

        val padX = max(12f, (maxX - minX) * 0.16f)
        val padY = max(12f, (maxY - minY) * 0.10f)
        val avgConf = (sumConf / count).coerceIn(0.15f, 0.99f)
        candidates.add(
            floatArrayOf(
                max(0f, minX - padX),
                max(0f, minY - padY),
                min(w, maxX + padX),
                min(h, maxY + padY)
            ) to avgConf
        )
        return isFullBody
    }

    private fun hasVisibleLandmark(pose: Pose, type: Int, w: Float, h: Float): Boolean {
        val lm = pose.getPoseLandmark(type) ?: return false
        return lm.inFrameLikelihood >= 0.25f &&
            lm.position.x in 0f..w &&
            lm.position.y in 0f..h
    }

    private fun iou(a: FloatArray, b: FloatArray): Float {
        val ix1 = max(a[0], b[0])
        val iy1 = max(a[1], b[1])
        val ix2 = min(a[2], b[2])
        val iy2 = min(a[3], b[3])
        val inter = max(0f, ix2 - ix1) * max(0f, iy2 - iy1)
        if (inter <= 0f) return 0f
        val areaA = max(1f, (a[2] - a[0]) * (a[3] - a[1]))
        val areaB = max(1f, (b[2] - b[0]) * (b[3] - b[1]))
        return inter / (areaA + areaB - inter)
    }

    override fun close() {
        runCatching { objectDetector.close() }
        runCatching { poseDetector.close() }
    }
}

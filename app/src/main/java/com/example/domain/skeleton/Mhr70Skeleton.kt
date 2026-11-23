package com.example.domain.skeleton

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Exact MHR70 joint ordering and skeleton connectivity ported from instanthmr/skeleton.py.
 */
object Mhr70Skeleton {
    const val NUM_JOINTS = 70
    const val DETECTOR_TO_GT_BOX = 1f / 0.866f

    val JOINT_NAMES: List<String> = listOf(
        "nose", "left_eye", "right_eye", "left_ear", "right_ear",
        "left_shoulder", "right_shoulder",
        "left_elbow", "right_elbow",
        "left_hip", "right_hip",
        "left_knee", "right_knee",
        "left_ankle", "right_ankle",
        "left_big_toe_tip", "left_small_toe_tip", "left_heel",
        "right_big_toe_tip", "right_small_toe_tip", "right_heel",
        "right_thumb_tip", "right_thumb_first_joint", "right_thumb_second_joint", "right_thumb_third_joint",
        "right_index_tip", "right_index_first_joint", "right_index_second_joint", "right_index_third_joint",
        "right_middle_tip", "right_middle_first_joint", "right_middle_second_joint", "right_middle_third_joint",
        "right_ring_tip", "right_ring_first_joint", "right_ring_second_joint", "right_ring_third_joint",
        "right_pinky_tip", "right_pinky_first_joint", "right_pinky_second_joint", "right_pinky_third_joint",
        "right_wrist",
        "left_thumb_tip", "left_thumb_first_joint", "left_thumb_second_joint", "left_thumb_third_joint",
        "left_index_tip", "left_index_first_joint", "left_index_second_joint", "left_index_third_joint",
        "left_middle_tip", "left_middle_first_joint", "left_middle_second_joint", "left_middle_third_joint",
        "left_ring_tip", "left_ring_first_joint", "left_ring_second_joint", "left_ring_third_joint",
        "left_pinky_tip", "left_pinky_first_joint", "left_pinky_second_joint", "left_pinky_third_joint",
        "left_wrist",
        "left_olecranon", "right_olecranon",
        "left_cubital_fossa", "right_cubital_fossa",
        "left_acromion", "right_acromion",
        "neck"
    )

    val SKELETON_EDGES: List<Pair<Int, Int>> = listOf(
        // Head <-> neck
        0 to 69,
        // Torso
        69 to 5, 69 to 6,
        5 to 9, 6 to 10, 9 to 10,
        // Arms
        5 to 7, 7 to 62,
        6 to 8, 8 to 41,
        // Legs
        9 to 11, 11 to 13, 13 to 17,
        10 to 12, 12 to 14, 14 to 20,
        // Left hand fan
        62 to 42, 62 to 46, 62 to 50, 62 to 54, 62 to 58,
        // Right hand fan
        41 to 21, 41 to 25, 41 to 29, 41 to 33, 41 to 37
    )

    // BODY_KEYPOINTS = list(range(21)) + [41, 62] + list(range(63, 70))
    val BODY_KEYPOINTS: IntArray = ((0..20).toList() + listOf(41, 62) + (63..69).toList()).toIntArray()

    /**
     * Port of keypoint_box() from instanthmr/pipeline.py.
     */
    fun keypointBox(joints2d: FloatArray, w: Int, h: Int, pad: Float = 0.15f): FloatArray {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (idx in BODY_KEYPOINTS) {
            val x = joints2d[idx * 2]
            val y = joints2d[idx * 2 + 1]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val halfW = (maxX - minX) * (0.5f + pad) / DETECTOR_TO_GT_BOX
        val halfH = (maxY - minY) * (0.5f + pad) / DETECTOR_TO_GT_BOX
        return floatArrayOf(
            max(0f, cx - halfW),
            max(0f, cy - halfH),
            min(w.toFloat(), cx + halfW),
            min(h.toFloat(), cy + halfH)
        )
    }

    /**
     * Converts ML Kit 33 pose landmarks into metric MHR70 2D and 3D coordinates for immediate
     * real-time on-device tracking prior to downloading Hugging Face ONNX weights.
     */
    fun fromMlKitPose(
        pose: Pose,
        imageWidth: Int,
        imageHeight: Int,
        focalLength: Float
    ): MlKitMappedPose? {
        val rawLS = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rawRS = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rawLH = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rawRH = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rawNose = pose.getPoseLandmark(PoseLandmark.NOSE)

        val lShoulder = rawLS ?: rawRS ?: return null
        val rShoulder = rawRS ?: rawLS ?: return null
        val nose = rawNose ?: lShoulder
        val lHip = rawLH ?: rawRH ?: lShoulder
        val rHip = rawRH ?: rawLH ?: rShoulder

        val j2d = FloatArray(NUM_JOINTS * 2)
        val j3d = FloatArray(NUM_JOINTS * 3)

        fun setJoint(mhrIdx: Int, x2: Float, y2: Float, x3: Float, y3: Float, z3: Float) {
            j2d[mhrIdx * 2] = x2
            j2d[mhrIdx * 2 + 1] = y2
            j3d[mhrIdx * 3] = x3
            j3d[mhrIdx * 3 + 1] = y3
            j3d[mhrIdx * 3 + 2] = z3
        }

        fun copyLandmark(mhrIdx: Int, mlKitType: Int, fallback: PoseLandmark) {
            val lm = pose.getPoseLandmark(mlKitType) ?: fallback
            val p2 = lm.position
            val p3 = lm.position3D
            setJoint(mhrIdx, p2.x, p2.y, p3.x, p3.y, p3.z)
        }

        copyLandmark(0, PoseLandmark.NOSE, nose)
        copyLandmark(1, PoseLandmark.LEFT_EYE, nose)
        copyLandmark(2, PoseLandmark.RIGHT_EYE, nose)
        copyLandmark(3, PoseLandmark.LEFT_EAR, nose)
        copyLandmark(4, PoseLandmark.RIGHT_EAR, nose)
        copyLandmark(5, PoseLandmark.LEFT_SHOULDER, lShoulder)
        copyLandmark(6, PoseLandmark.RIGHT_SHOULDER, rShoulder)
        copyLandmark(7, PoseLandmark.LEFT_ELBOW, lShoulder)
        copyLandmark(8, PoseLandmark.RIGHT_ELBOW, rShoulder)
        copyLandmark(9, PoseLandmark.LEFT_HIP, lHip)
        copyLandmark(10, PoseLandmark.RIGHT_HIP, rHip)
        copyLandmark(11, PoseLandmark.LEFT_KNEE, lHip)
        copyLandmark(12, PoseLandmark.RIGHT_KNEE, rHip)
        copyLandmark(13, PoseLandmark.LEFT_ANKLE, lHip)
        copyLandmark(14, PoseLandmark.RIGHT_ANKLE, rHip)
        copyLandmark(15, PoseLandmark.LEFT_FOOT_INDEX, lHip)
        copyLandmark(16, PoseLandmark.LEFT_FOOT_INDEX, lHip)
        copyLandmark(17, PoseLandmark.LEFT_HEEL, lHip)
        copyLandmark(18, PoseLandmark.RIGHT_FOOT_INDEX, rHip)
        copyLandmark(19, PoseLandmark.RIGHT_FOOT_INDEX, rHip)
        copyLandmark(20, PoseLandmark.RIGHT_HEEL, rHip)

        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST) ?: rShoulder
        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: lShoulder
        setJoint(41, rWrist.position.x, rWrist.position.y, rWrist.position3D.x, rWrist.position3D.y, rWrist.position3D.z)
        setJoint(62, lWrist.position.x, lWrist.position.y, lWrist.position3D.x, lWrist.position3D.y, lWrist.position3D.z)

        val rThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB) ?: rWrist
        val rIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX) ?: rWrist
        val rPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY) ?: rWrist
        fillHandFan(j2d, j3d, 21, rWrist, rThumb, rIndex, rPinky)

        val lThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB) ?: lWrist
        val lIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX) ?: lWrist
        val lPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY) ?: lWrist
        fillHandFan(j2d, j3d, 42, lWrist, lThumb, lIndex, lPinky)

        // Neck (69)
        val neckX2 = (j2d[5 * 2] + j2d[6 * 2]) * 0.5f * 0.85f + j2d[0] * 0.15f
        val neckY2 = (j2d[5 * 2 + 1] + j2d[6 * 2 + 1]) * 0.5f * 0.85f + j2d[1] * 0.15f
        val neckX3 = (j3d[5 * 3] + j3d[6 * 3]) * 0.5f * 0.85f + j3d[0] * 0.15f
        val neckY3 = (j3d[5 * 3 + 1] + j3d[6 * 3 + 1]) * 0.5f * 0.85f + j3d[1] * 0.15f
        val neckZ3 = (j3d[5 * 3 + 2] + j3d[6 * 3 + 2]) * 0.5f * 0.85f + j3d[2] * 0.15f
        setJoint(69, neckX2, neckY2, neckX3, neckY3, neckZ3)

        // Olecranon/cubital/acromion approximations
        setJoint(63, j2d[7 * 2], j2d[7 * 2 + 1], j3d[7 * 3], j3d[7 * 3 + 1], j3d[7 * 3 + 2])
        setJoint(64, j2d[8 * 2], j2d[8 * 2 + 1], j3d[8 * 3], j3d[8 * 3 + 1], j3d[8 * 3 + 2])
        setJoint(65, j2d[7 * 2], j2d[7 * 2 + 1], j3d[7 * 3], j3d[7 * 3 + 1], j3d[7 * 3 + 2])
        setJoint(66, j2d[8 * 2], j2d[8 * 2 + 1], j3d[8 * 3], j3d[8 * 3 + 1], j3d[8 * 3 + 2])
        setJoint(67, j2d[5 * 2], j2d[5 * 2 + 1], j3d[5 * 3], j3d[5 * 3 + 1], j3d[5 * 3 + 2])
        setJoint(68, j2d[6 * 2], j2d[6 * 2 + 1], j3d[6 * 3], j3d[6 * 3 + 1], j3d[6 * 3 + 2])

        // Normalize ML Kit position3D (which uses image-pixel scale) to metric scale (~0.52m torso)
        // centered at mid-hip so ProceduralBodyMeshBuilder and 3D viewport always render accurate proportions.
        val hipCx3 = (j3d[9 * 3] + j3d[10 * 3]) * 0.5f
        val hipCy3 = (j3d[9 * 3 + 1] + j3d[10 * 3 + 1]) * 0.5f
        val hipCz3 = (j3d[9 * 3 + 2] + j3d[10 * 3 + 2]) * 0.5f
        val shoulderCx3 = (j3d[5 * 3] + j3d[6 * 3]) * 0.5f
        val shoulderCy3 = (j3d[5 * 3 + 1] + j3d[6 * 3 + 1]) * 0.5f
        val shoulderCz3 = (j3d[5 * 3 + 2] + j3d[6 * 3 + 2]) * 0.5f

        val rawTorso3d = sqrt(
            (shoulderCx3 - hipCx3) * (shoulderCx3 - hipCx3) +
                (shoulderCy3 - hipCy3) * (shoulderCy3 - hipCy3) +
                (shoulderCz3 - hipCz3) * (shoulderCz3 - hipCz3)
        )
        val rawShoulderSpan3d = sqrt(
            (j3d[5 * 3] - j3d[6 * 3]) * (j3d[5 * 3] - j3d[6 * 3]) +
                (j3d[5 * 3 + 1] - j3d[6 * 3 + 1]) * (j3d[5 * 3 + 1] - j3d[6 * 3 + 1]) +
                (j3d[5 * 3 + 2] - j3d[6 * 3 + 2]) * (j3d[5 * 3 + 2] - j3d[6 * 3 + 2])
        )
        val metricScale = if (rawTorso3d > 0.05f) {
            0.52f / rawTorso3d
        } else if (rawShoulderSpan3d > 0.05f) {
            0.38f / rawShoulderSpan3d
        } else {
            1.0f
        }

        for (i in 0 until NUM_JOINTS) {
            j3d[i * 3] = (j3d[i * 3] - hipCx3) * metricScale
            j3d[i * 3 + 1] = (j3d[i * 3 + 1] - hipCy3) * metricScale
            j3d[i * 3 + 2] = (j3d[i * 3 + 2] - hipCz3) * metricScale
        }

        // Estimate camera-space translation using apparent 2D torso scale vs 0.52m reference
        val hipCx2 = (j2d[9 * 2] + j2d[10 * 2]) * 0.5f
        val hipCy2 = (j2d[9 * 2 + 1] + j2d[10 * 2 + 1]) * 0.5f
        val dx2 = neckX2 - hipCx2
        val dy2 = neckY2 - hipCy2
        val torsoPx = max(24f, sqrt(dx2 * dx2 + dy2 * dy2))
        val zCam = (focalLength * 0.52f / torsoPx).coerceIn(1.2f, 8.0f)
        val xCam = (hipCx2 - imageWidth * 0.5f) * zCam / focalLength
        val yCam = (hipCy2 - imageHeight * 0.5f) * zCam / focalLength

        val bbox = keypointBox(j2d, imageWidth, imageHeight, pad = 0.12f)
        val conf = ((lShoulder.inFrameLikelihood + rShoulder.inFrameLikelihood + lHip.inFrameLikelihood + rHip.inFrameLikelihood) / 4f)
            .coerceIn(0.15f, 0.99f)

        return MlKitMappedPose(
            bbox = bbox,
            confidence = conf,
            joints2d = j2d,
            joints3dLocal = j3d,
            camTrans = floatArrayOf(xCam, yCam, zCam)
        )
    }

    private fun fillHandFan(
        j2d: FloatArray,
        j3d: FloatArray,
        startIdx: Int,
        wrist: PoseLandmark,
        thumb: PoseLandmark,
        index: PoseLandmark,
        pinky: PoseLandmark
    ) {
        val tips = arrayOf(thumb, index, index, pinky, pinky)
        val spread = floatArrayOf(1.0f, 1.0f, 1.05f, 1.0f, 0.95f)
        for (finger in 0 until 5) {
            val target = tips[finger]
            val s = spread[finger]
            for (seg in 0 until 4) {
                val frac = (1f - seg * 0.25f) * s
                val idx = startIdx + finger * 4 + seg
                j2d[idx * 2] = wrist.position.x + (target.position.x - wrist.position.x) * frac
                j2d[idx * 2 + 1] = wrist.position.y + (target.position.y - wrist.position.y) * frac
                j3d[idx * 3] = wrist.position3D.x + (target.position3D.x - wrist.position3D.x) * frac
                j3d[idx * 3 + 1] = wrist.position3D.y + (target.position3D.y - wrist.position3D.y) * frac
                j3d[idx * 3 + 2] = wrist.position3D.z + (target.position3D.z - wrist.position3D.z) * frac
            }
        }
    }

    data class MlKitMappedPose(
        val bbox: FloatArray,
        val confidence: Float,
        val joints2d: FloatArray,
        val joints3dLocal: FloatArray,
        val camTrans: FloatArray
    )
}

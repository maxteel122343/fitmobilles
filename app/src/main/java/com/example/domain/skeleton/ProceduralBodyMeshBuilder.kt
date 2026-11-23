package com.example.domain.skeleton

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates an anatomical 3D body proxy mesh from MHR70 local 3D joints when
 * the 18,439-vertex ONNX body graph (`mhr-body.onnx`) has not yet been downloaded.
 */
object ProceduralBodyMeshBuilder {
    private const val RINGS = 10

    data class MeshData(
        val verticesLocal: FloatArray,
        val faces: IntArray
    )

    fun build(joints3dLocal: FloatArray): MeshData {
        val verts = ArrayList<Float>(1600 * 3)
        val faces = ArrayList<Int>(3000 * 3)

        fun j(idx: Int): FloatArray = floatArrayOf(
            joints3dLocal[idx * 3],
            joints3dLocal[idx * 3 + 1],
            joints3dLocal[idx * 3 + 2]
        )

        fun mid(a: FloatArray, b: FloatArray) = floatArrayOf(
            (a[0] + b[0]) * 0.5f,
            (a[1] + b[1]) * 0.5f,
            (a[2] + b[2]) * 0.5f
        )

        // Torso & Pelvis
        val lShoulder = j(5)
        val rShoulder = j(6)
        val lHip = j(9)
        val rHip = j(10)
        val neck = j(69)
        val nose = j(0)

        val chestCenter = mid(lShoulder, rShoulder)
        val pelvisCenter = mid(lHip, rHip)
        val shoulderWidth = dist(lShoulder, rShoulder).coerceIn(0.24f, 0.55f)
        val hipWidth = dist(lHip, rHip).coerceIn(0.18f, 0.45f)

        // Torso tapered limb
        addTaperedLimb(verts, faces, pelvisCenter, chestCenter, hipWidth * 0.52f, shoulderWidth * 0.52f, 0.65f)
        // Neck & Head
        addTaperedLimb(verts, faces, chestCenter, neck, 0.065f, 0.055f, 1.0f)
        addEllipsoid(verts, faces, floatArrayOf(nose[0], (nose[1] + neck[1]) * 0.5f - 0.03f, nose[2]), 0.095f, 0.125f, 0.105f)

        // Left & Right Arms
        val lElbow = j(7)
        val rElbow = j(8)
        val lWrist = j(62)
        val rWrist = j(41)

        addTaperedLimb(verts, faces, lShoulder, lElbow, 0.058f, 0.046f, 1.0f)
        addTaperedLimb(verts, faces, lElbow, lWrist, 0.044f, 0.032f, 1.0f)
        addEllipsoid(verts, faces, lWrist, 0.035f, 0.05f, 0.025f)

        addTaperedLimb(verts, faces, rShoulder, rElbow, 0.058f, 0.046f, 1.0f)
        addTaperedLimb(verts, faces, rElbow, rWrist, 0.044f, 0.032f, 1.0f)
        addEllipsoid(verts, faces, rWrist, 0.035f, 0.05f, 0.025f)

        // Left & Right Legs
        val lKnee = j(11)
        val rKnee = j(12)
        val lAnkle = j(13)
        val rAnkle = j(14)
        val lToe = j(15)
        val rToe = j(18)

        addTaperedLimb(verts, faces, lHip, lKnee, 0.088f, 0.062f, 1.0f)
        addTaperedLimb(verts, faces, lKnee, lAnkle, 0.060f, 0.040f, 1.0f)
        addTaperedLimb(verts, faces, lAnkle, lToe, 0.042f, 0.034f, 0.7f)

        addTaperedLimb(verts, faces, rHip, rKnee, 0.088f, 0.062f, 1.0f)
        addTaperedLimb(verts, faces, rKnee, rAnkle, 0.060f, 0.040f, 1.0f)
        addTaperedLimb(verts, faces, rAnkle, rToe, 0.042f, 0.034f, 0.7f)

        return MeshData(verts.toFloatArray(), faces.toIntArray())
    }

    private fun addTaperedLimb(
        verts: MutableList<Float>,
        faces: MutableList<Int>,
        p0: FloatArray,
        p1: FloatArray,
        r0: Float,
        r1: Float,
        depthScale: Float
    ) {
        val dir = floatArrayOf(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2])
        val len = sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2])
        if (len < 1e-4f) return
        val axis = floatArrayOf(dir[0] / len, dir[1] / len, dir[2] / len)

        // Orthonormal basis around axis
        val ref = if (kotlin.math.abs(axis[2]) < 0.9f) floatArrayOf(0f, 0f, 1f) else floatArrayOf(1f, 0f, 0f)
        val u = normalize(cross(ref, axis))
        val v = normalize(cross(axis, u))

        val baseIdx = verts.size / 3
        val stacks = 4
        for (s in 0..stacks) {
            val t = s.toFloat() / stacks
            val cx = p0[0] + dir[0] * t
            val cy = p0[1] + dir[1] * t
            val cz = p0[2] + dir[2] * t
            val r = r0 + (r1 - r0) * t
            for (k in 0 until RINGS) {
                val theta = (2.0 * PI * k / RINGS).toFloat()
                val ct = cos(theta) * r
                val st = sin(theta) * r * depthScale
                verts.add(cx + u[0] * ct + v[0] * st)
                verts.add(cy + u[1] * ct + v[1] * st)
                verts.add(cz + u[2] * ct + v[2] * st)
            }
        }
        for (s in 0 until stacks) {
            val row0 = baseIdx + s * RINGS
            val row1 = baseIdx + (s + 1) * RINGS
            for (k in 0 until RINGS) {
                val next = (k + 1) % RINGS
                faces.add(row0 + k)
                faces.add(row1 + k)
                faces.add(row1 + next)

                faces.add(row0 + k)
                faces.add(row1 + next)
                faces.add(row0 + next)
            }
        }
    }

    private fun addEllipsoid(
        verts: MutableList<Float>,
        faces: MutableList<Int>,
        center: FloatArray,
        rx: Float,
        ry: Float,
        rz: Float
    ) {
        val baseIdx = verts.size / 3
        val latSteps = 6
        val lonSteps = RINGS
        for (i in 0..latSteps) {
            val phi = (PI * i / latSteps).toFloat()
            val sinP = sin(phi)
            val cosP = cos(phi)
            for (j in 0 until lonSteps) {
                val theta = (2.0 * PI * j / lonSteps).toFloat()
                verts.add(center[0] + rx * sinP * cos(theta))
                verts.add(center[1] + ry * cosP)
                verts.add(center[2] + rz * sinP * sin(theta))
            }
        }
        for (i in 0 until latSteps) {
            val r0 = baseIdx + i * lonSteps
            val r1 = baseIdx + (i + 1) * lonSteps
            for (j in 0 until lonSteps) {
                val next = (j + 1) % lonSteps
                faces.add(r0 + j)
                faces.add(r1 + j)
                faces.add(r1 + next)

                faces.add(r0 + j)
                faces.add(r1 + next)
                faces.add(r0 + next)
            }
        }
    }

    private fun dist(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return max(1e-4f, sqrt(dx * dx + dy * dy + dz * dz))
    }

    private fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    )

    private fun normalize(v: FloatArray): FloatArray {
        val len = max(1e-6f, sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]))
        return floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
    }
}

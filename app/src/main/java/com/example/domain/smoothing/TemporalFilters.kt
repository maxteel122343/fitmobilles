package com.example.domain.smoothing

import com.example.domain.model.HmrPrediction
import com.example.domain.model.PoseFilterKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Exact Kotlin port of instanthmr/smoothing.py:
 * - PoseFilter (one-euro and alpha-beta on mhr_params[3..68] with angle wrapping)
 * - TemporalSmoother (1 Euro filter on joints_2d, joints_3d_local, vertices_local, cam_trans)
 * - IoU-based multi-person track assignment
 */
private fun wrapAngle(a: Double): Double {
    val twoPi = 2.0 * PI
    var r = (a + PI) % twoPi
    if (r < 0.0) r += twoPi
    return r - PI
}

private fun alpha(cutoff: Double, dt: Double): Double {
    val denom = 1.0 + 1.0 / (2.0 * PI * max(cutoff, 1e-6) * dt)
    return 1.0 / denom
}

class OneEuroVectorFilter(
    private val minCutoff: Double,
    private val beta: Double,
    private val dCutoff: Double = 1.0,
    private val angles: Boolean = false
) {
    private var x: DoubleArray? = null
    private var dx: DoubleArray? = null
    private var lastT: Double = 0.0

    fun filter(input: FloatArray, t: Double): FloatArray {
        val currentX = x
        val currentDx = dx
        if (currentX == null || currentDx == null || currentX.size != input.size) {
            x = DoubleArray(input.size) { input[it].toDouble() }
            dx = DoubleArray(input.size)
            lastT = t
            return input.copyOf()
        }
        val dt = if (t > lastT) (t - lastT) else (1.0 / 30.0)
        lastT = t
        val aD = alpha(dCutoff, dt)
        val out = FloatArray(input.size)
        for (i in input.indices) {
            var r = input[i].toDouble() - currentX[i]
            if (angles) r = wrapAngle(r)
            currentDx[i] = aD * (r / dt) + (1.0 - aD) * currentDx[i]
            val cutoff = minCutoff + beta * abs(currentDx[i])
            currentX[i] = currentX[i] + alpha(cutoff, dt) * r
            out[i] = currentX[i].toFloat()
        }
        return out
    }
}

class AlphaBetaAngleFilter(
    private val a: Double = 0.8,
    private val b: Double = 0.005
) {
    private var x: DoubleArray? = null
    private var v: DoubleArray? = null

    fun filter(input: FloatArray): FloatArray {
        val currentX = x
        val currentV = v
        if (currentX == null || currentV == null || currentX.size != input.size) {
            x = DoubleArray(input.size) { input[it].toDouble() }
            v = DoubleArray(input.size)
            return input.copyOf()
        }
        val out = FloatArray(input.size)
        for (i in input.indices) {
            val pred = currentX[i] + currentV[i]
            val r = wrapAngle(input[i].toDouble() - pred)
            currentX[i] = pred + a * r
            currentV[i] = currentV[i] + b * r
            out[i] = currentX[i].toFloat()
        }
        return out
    }
}

private fun boxIou(a: FloatArray, b: FloatArray): Float {
    val iw = max(0f, min(a[2], b[2]) - max(a[0], b[0]))
    val ih = max(0f, min(a[3], b[3]) - max(a[1], b[1]))
    val inter = iw * ih
    val areaA = (a[2] - a[0]) * (a[3] - a[1])
    val areaB = (b[2] - b[0]) * (b[3] - b[1])
    val union = areaA + areaB - inter
    return if (union > 0f) inter / union else 0f
}

class IoUTracks<T>(
    private val iouThreshold: Float = 0.3f,
    private val maxAge: Int = 5,
    private val createState: () -> T
) {
    private data class Track<T>(var box: FloatArray, var age: Int = 0, val state: T)

    private val tracks = mutableMapOf<Int, Track<T>>()
    private var nextId = 0

    fun assign(boxes: List<FloatArray>): List<T> {
        val pairs = ArrayList<Triple<Float, Int, Int>>(boxes.size * tracks.size)
        for (i in boxes.indices) {
            for ((tid, tr) in tracks) {
                pairs.add(Triple(boxIou(boxes[i], tr.box), i, tid))
            }
        }
        pairs.sortByDescending { it.first }

        val assignedIds = IntArray(boxes.size) { -1 }
        val usedTrackIds = HashSet<Int>()
        for ((score, boxIdx, tid) in pairs) {
            if (score >= iouThreshold && assignedIds[boxIdx] == -1 && !usedTrackIds.contains(tid)) {
                assignedIds[boxIdx] = tid
                usedTrackIds.add(tid)
            }
        }
        for (i in boxes.indices) {
            if (assignedIds[i] == -1) {
                val id = nextId++
                assignedIds[i] = id
                tracks[id] = Track(boxes[i].copyOf(), 0, createState())
            } else {
                val tr = tracks[assignedIds[i]]!!
                tr.box = boxes[i].copyOf()
                tr.age = 0
            }
        }
        val activeSet = assignedIds.toHashSet()
        val toRemove = mutableListOf<Int>()
        for ((tid, tr) in tracks) {
            if (tid !in activeSet) {
                tr.age += 1
                if (tr.age > maxAge) toRemove.add(tid)
            }
        }
        for (tid in toRemove) tracks.remove(tid)

        return assignedIds.map { tracks[it]!!.state }
    }

    fun reset() {
        tracks.clear()
        nextId = 0
    }
}

class PoseFilter(val kind: PoseFilterKind) {
    private class State(
        var oneEuro: OneEuroVectorFilter? = null,
        var alphaBeta: AlphaBetaAngleFilter? = null
    )

    private val tracks = IoUTracks { State() }
    private var clock = 0.0

    fun filter(boxes: List<FloatArray>, mhrParamsBatch: Array<FloatArray>, timestampSec: Float?): Array<FloatArray> {
        if (kind == PoseFilterKind.NONE) return mhrParamsBatch
        val t = if (timestampSec != null) {
            timestampSec.toDouble()
        } else {
            clock += 1.0 / 30.0
            clock
        }
        val states = tracks.assign(boxes)
        return Array(mhrParamsBatch.size) { i ->
            val out = mhrParamsBatch[i].copyOf()
            if (out.size >= 68) {
                val slice = out.copyOfRange(3, 68)
                val filtered = when (kind) {
                    PoseFilterKind.ONE_EURO -> {
                        val f = states[i].oneEuro ?: OneEuroVectorFilter(
                            minCutoff = 12.0, beta = 0.5, dCutoff = 1.0, angles = true
                        ).also { states[i].oneEuro = it }
                        f.filter(slice, t)
                    }
                    PoseFilterKind.ALPHA_BETA -> {
                        val f = states[i].alphaBeta ?: AlphaBetaAngleFilter(a = 0.8, b = 0.005)
                            .also { states[i].alphaBeta = it }
                        f.filter(slice)
                    }
                    PoseFilterKind.NONE -> slice
                }
                System.arraycopy(filtered, 0, out, 3, 65)
            }
            out
        }
    }

    fun reset() {
        tracks.reset()
        clock = 0.0
    }
}

class TemporalSmoother(
    private val betaScale: Float = 1.0f,
    iouThreshold: Float = 0.3f,
    maxAge: Int = 5
) {
    private class State {
        val filters = HashMap<String, OneEuroVectorFilter>()
    }

    private val tracks = IoUTracks(iouThreshold, maxAge) { State() }

    fun smooth(persons: List<HmrPrediction>, timestampSec: Float): List<HmrPrediction> {
        if (persons.isEmpty()) return emptyList()
        val states = tracks.assign(persons.map { it.bbox })
        val t = timestampSec.toDouble()
        return persons.mapIndexed { idx, p ->
            val map = states[idx].filters
            val j2 = map.getOrPut("joints_2d") { OneEuroVectorFilter(1.0, 0.01 * betaScale) }
                .filter(p.joints2d, t)
            val j3 = map.getOrPut("joints_3d_local") { OneEuroVectorFilter(1.0, 4.0 * betaScale) }
                .filter(p.joints3dLocal, t)
            val v3 = map.getOrPut("vertices_local") { OneEuroVectorFilter(1.0, 4.0 * betaScale) }
                .filter(p.verticesLocal, t)
            val cam = map.getOrPut("cam_trans") { OneEuroVectorFilter(0.6, 2.0 * betaScale) }
                .filter(p.camTrans, t)
            p.copyWith(joints2d = j2, joints3dLocal = j3, verticesLocal = v3, camTrans = cam)
        }
    }

    fun reset() {
        tracks.reset()
    }
}

package com.example.domain.fitness

import com.example.domain.model.HmrPrediction
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class FormStatus(val label: String) {
    GOOD("Ótima Forma"),
    CAUTION("Atenção"),
    FAULT("Corrigir")
}

enum class RepPhase(val label: String) {
    READY("Pronto"),
    ECCENTRIC("Descendo"),
    BOTTOM("Pico / Fundo"),
    CONCENTRIC("Subindo")
}

data class ExerciseFeedback(
    val exercise: ExerciseDefinition,
    val repCount: Int,
    val phase: RepPhase,
    val primaryAngleDeg: Float,
    val secondaryAngleDeg: Float,
    val primaryLabel: String,
    val secondaryLabel: String,
    val formStatus: FormStatus,
    val formCue: String,
    val justCompletedRep: Boolean,
    val autoDetectedName: String? = null
)

/**
 * Port of Good-GYM `exercise_counters.py` rep-counting & biomechanical form evaluation,
 * adapted directly to InstantHMR's MHR70 keypoints (`joints2d` and `joints3dCam`).
 *
 * MHR70 keypoint indices used:
 * - Nose: 0, Neck: 69
 * - Shoulders: L=5, R=6
 * - Elbows: L=7, R=8
 * - Hips: L=9, R=10
 * - Knees: L=11, R=12
 * - Ankles: L=13, R=14
 * - Wrists: R=41, L=62
 */
class GoodGymRepCounter {

    private var repCount = 0
    private var stageDown = false
    private var minAngleInRep = 180f
    private var maxAngleInRep = 0f
    private var lastCue = "Posicione o corpo inteiro na câmera"
    private var lastStatus = FormStatus.GOOD

    // Auto-classification smoothing buffer
    private val recentAngles = ArrayDeque<FloatArray>(15)
    private var activeExerciseId: String = "squat"

    fun reset(exerciseId: String? = null) {
        repCount = 0
        stageDown = false
        minAngleInRep = 180f
        maxAngleInRep = 0f
        recentAngles.clear()
        if (exerciseId != null) activeExerciseId = exerciseId
        lastCue = "Pronto para iniciar"
        lastStatus = FormStatus.GOOD
    }

    fun evaluate(
        person: HmrPrediction,
        selectedExerciseId: String,
        autoDetect: Boolean
    ): ExerciseFeedback {
        val j3 = person.joints3dCam
        val j2 = person.joints2d

        // Compute core biomechanical joint angles from 3D camera-space MHR70 joints
        // (falls back cleanly to 2D if 3D norm collapses)
        val lKnee = angle3DOr2D(j3, j2, 9, 11, 13)
        val rKnee = angle3DOr2D(j3, j2, 10, 12, 14)
        val avgKnee = (lKnee + rKnee) * 0.5f
        val minKnee = min(lKnee, rKnee)

        val lElbow = angle3DOr2D(j3, j2, 5, 7, 62)
        val rElbow = angle3DOr2D(j3, j2, 6, 8, 41)
        val avgElbow = (lElbow + rElbow) * 0.5f
        val minElbow = min(lElbow, rElbow)

        val lHip = angle3DOr2D(j3, j2, 5, 9, 11)
        val rHip = angle3DOr2D(j3, j2, 6, 10, 12)
        val avgHip = (lHip + rHip) * 0.5f

        val lShoulderAbd = angle3DOr2D(j3, j2, 9, 5, 7)
        val rShoulderAbd = angle3DOr2D(j3, j2, 10, 6, 8)
        val avgShoulderAbd = (lShoulderAbd + rShoulderAbd) * 0.5f

        val torsoLeanDeg = torsoInclinationFromVerticalDeg(j3, j2)
        val bodyAlignmentDeg = bodyPlankAngleDeg(j3, j2)

        val resolvedExercise = if (autoDetect) {
            val detected = classifyExerciseAuto(
                avgKnee = avgKnee,
                kneeDiff = abs(lKnee - rKnee),
                avgElbow = avgElbow,
                torsoLeanDeg = torsoLeanDeg,
                avgShoulderAbd = avgShoulderAbd
            )
            if (detected != activeExerciseId) {
                activeExerciseId = detected
                stageDown = false
                minAngleInRep = 180f
                maxAngleInRep = 0f
            }
            ExerciseCatalog.findById(activeExerciseId)
        } else {
            activeExerciseId = selectedExerciseId
            ExerciseCatalog.findById(selectedExerciseId)
        }

        val (primary, secondary, pLabel, sLabel) = when (resolvedExercise.driver) {
            TrackingDriver.SQUAT_KNEE -> Quad(avgKnee, torsoLeanDeg, "Joelho", "Tronco")
            TrackingDriver.LUNGE_KNEE -> Quad(minKnee, torsoLeanDeg, "Joelho Frente", "Tronco")
            TrackingDriver.PUSHUP_ELBOW -> Quad(avgElbow, bodyAlignmentDeg, "Cotovelo", "Alinhamento")
            TrackingDriver.CURL_ELBOW -> Quad(minElbow, avgShoulderAbd, "Cotovelo", "Ombro")
            TrackingDriver.HIP_HINGE -> Quad(avgHip, torsoLeanDeg, "Quadril", "Inclinação")
            TrackingDriver.SHOULDER_PRESS -> Quad(avgElbow, avgShoulderAbd, "Cotovelo", "Elevação")
            TrackingDriver.LATERAL_RAISE -> Quad(avgShoulderAbd, avgElbow, "Abdução", "Cotovelo")
            TrackingDriver.CORE_FLEXION -> Quad(avgHip, torsoLeanDeg, "Flexão", "Tronco")
        }

        val downThresh = resolvedExercise.downThresholdDeg
        val upThresh = resolvedExercise.upThresholdDeg
        val inverted = resolvedExercise.driver == TrackingDriver.LATERAL_RAISE

        var justRep = false
        if (!inverted) {
            minAngleInRep = min(minAngleInRep, primary)
            if (!stageDown && primary <= downThresh) {
                stageDown = true
            } else if (stageDown && primary >= upThresh) {
                stageDown = false
                repCount += 1
                justRep = true
                minAngleInRep = 180f
            }
        } else {
            maxAngleInRep = max(maxAngleInRep, primary)
            if (!stageDown && primary >= upThresh) {
                stageDown = true
            } else if (stageDown && primary <= downThresh) {
                stageDown = false
                repCount += 1
                justRep = true
                maxAngleInRep = 0f
            }
        }

        val phase = when {
            stageDown && ((!inverted && primary <= downThresh + 8f) || (inverted && primary >= upThresh - 8f)) -> RepPhase.BOTTOM
            stageDown -> RepPhase.CONCENTRIC
            (!inverted && primary < upThresh - 10f) || (inverted && primary > downThresh + 10f) -> RepPhase.ECCENTRIC
            else -> RepPhase.READY
        }

        val (status, cue) = evaluateForm(
            driver = resolvedExercise.driver,
            primary = primary,
            secondary = secondary,
            stageDown = stageDown,
            justRep = justRep,
            downThresh = downThresh,
            upThresh = upThresh
        )
        lastStatus = status
        lastCue = cue

        return ExerciseFeedback(
            exercise = resolvedExercise,
            repCount = repCount,
            phase = phase,
            primaryAngleDeg = primary,
            secondaryAngleDeg = secondary,
            primaryLabel = pLabel,
            secondaryLabel = sLabel,
            formStatus = status,
            formCue = cue,
            justCompletedRep = justRep,
            autoDetectedName = if (autoDetect) resolvedExercise.namePt else null
        )
    }

    private fun evaluateForm(
        driver: TrackingDriver,
        primary: Float,
        secondary: Float,
        stageDown: Boolean,
        justRep: Boolean,
        downThresh: Float,
        upThresh: Float
    ): Pair<FormStatus, String> {
        if (justRep) {
            return FormStatus.GOOD to "Boa repetição! Controle mantido."
        }
        return when (driver) {
            TrackingDriver.SQUAT_KNEE -> when {
                secondary > 42f -> FormStatus.FAULT to "Peito muito inclinado (${secondary.toInt()}°) — erga o tronco"
                stageDown && primary <= downThresh -> FormStatus.GOOD to "Profundidade ideal (${primary.toInt()}°) — suba firme"
                primary in (downThresh + 1f)..122f -> FormStatus.CAUTION to "Desça mais um pouco para 90°"
                else -> FormStatus.GOOD to "Postura alinhada — controle a descida"
            }
            TrackingDriver.LUNGE_KNEE -> when {
                secondary > 30f -> FormStatus.FAULT to "Mantenha o tronco reto (${secondary.toInt()}°)"
                stageDown && primary <= downThresh -> FormStatus.GOOD to "Excelente amplitude de passada!"
                primary in (downThresh + 1f)..128f -> FormStatus.CAUTION to "Abaixe o joelho de trás mais perto do solo"
                else -> FormStatus.GOOD to "Equilíbrio estável"
            }
            TrackingDriver.PUSHUP_ELBOW -> when {
                secondary < 148f -> FormStatus.FAULT to "Quadril caindo/elevado (${secondary.toInt()}°) — contraia o core"
                stageDown && primary <= downThresh -> FormStatus.GOOD to "Amplitude completa (${primary.toInt()}°) — empurre!"
                primary in (downThresh + 1f)..122f -> FormStatus.CAUTION to "Desça o peito mais próximo do chão"
                else -> FormStatus.GOOD to "Prancha firme — desça devagar"
            }
            TrackingDriver.CURL_ELBOW -> when {
                secondary > 34f -> FormStatus.FAULT to "Cotovelo avançando (${secondary.toInt()}°) — trave junto ao corpo"
                stageDown && primary <= downThresh -> FormStatus.GOOD to "Contração máxima no bíceps!"
                primary in 60f..115f -> FormStatus.CAUTION to "Suba até o pico de contração"
                else -> FormStatus.GOOD to "Extensão controlada"
            }
            TrackingDriver.HIP_HINGE -> when {
                primary < 75f -> FormStatus.CAUTION to "Cuidado para não arredondar a lombar"
                stageDown -> FormStatus.GOOD to "Alongamento posterior ideal — estenda o quadril"
                else -> FormStatus.GOOD to "Coluna neutra"
            }
            TrackingDriver.SHOULDER_PRESS -> when {
                primary > upThresh - 5f -> FormStatus.GOOD to "Extensão completa acima da cabeça!"
                secondary < 50f -> FormStatus.CAUTION to "Eleve os cotovelos na linha das orelhas"
                else -> FormStatus.GOOD to "Controle a descida até a linha do queixo"
            }
            TrackingDriver.LATERAL_RAISE -> when {
                primary > 105f -> FormStatus.FAULT to "Não passe da linha dos ombros"
                primary >= upThresh -> FormStatus.GOOD to "Altura ideal de abdução!"
                else -> FormStatus.GOOD to "Mantenha leve flexão de cotovelo"
            }
            TrackingDriver.CORE_FLEXION -> when {
                primary <= downThresh -> FormStatus.GOOD to "Contração abdominal forte!"
                else -> FormStatus.GOOD to "Expire na subida mantendo o controle"
            }
        }
    }

    private fun classifyExerciseAuto(
        avgKnee: Float,
        kneeDiff: Float,
        avgElbow: Float,
        torsoLeanDeg: Float,
        avgShoulderAbd: Float
    ): String {
        recentAngles.addLast(floatArrayOf(avgKnee, kneeDiff, avgElbow, torsoLeanDeg, avgShoulderAbd))
        if (recentAngles.size > 15) recentAngles.removeFirst()

        val meanTorso = recentAngles.map { it[3] }.average().toFloat()
        val kneeRange = (recentAngles.maxOf { it[0] } - recentAngles.minOf { it[0] })
        val elbowRange = (recentAngles.maxOf { it[2] } - recentAngles.minOf { it[2] })
        val meanKneeDiff = recentAngles.map { it[1] }.average().toFloat()
        val minKneeSeen = recentAngles.minOf { it[0] }
        val minElbowSeen = recentAngles.minOf { it[2] }

        return when {
            meanTorso > 50f && (elbowRange > 10f || minElbowSeen < 135f) -> "push_up"
            (kneeRange > 18f || minKneeSeen < 132f) && meanKneeDiff > 22f -> "lunge"
            kneeRange > 18f || (minKneeSeen < 130f && meanTorso < 48f) -> "squat"
            (elbowRange > 20f || minElbowSeen < 105f) && meanTorso < 35f && avgKnee > 142f -> "bicep_curl"
            else -> activeExerciseId
        }
    }

    private fun angle3DOr2D(j3: FloatArray, j2: FloatArray, aIdx: Int, bIdx: Int, cIdx: Int): Float {
        val ax = j3[aIdx * 3]; val ay = j3[aIdx * 3 + 1]; val az = j3[aIdx * 3 + 2]
        val bx = j3[bIdx * 3]; val by = j3[bIdx * 3 + 1]; val bz = j3[bIdx * 3 + 2]
        val cx = j3[cIdx * 3]; val cy = j3[cIdx * 3 + 1]; val cz = j3[cIdx * 3 + 2]

        val v1x = ax - bx; val v1y = ay - by; val v1z = az - bz
        val v2x = cx - bx; val v2y = cy - by; val v2z = cz - bz
        val n1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
        val n2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
        if (n1 > 1e-4f && n2 > 1e-4f) {
            val cosTheta = ((v1x * v2x + v1y * v2y + v1z * v2z) / (n1 * n2)).coerceIn(-1f, 1f)
            return (acos(cosTheta) * 180f / PI.toFloat())
        }

        // 2D fallback
        val ux = j2[aIdx * 2] - j2[bIdx * 2]
        val uy = j2[aIdx * 2 + 1] - j2[bIdx * 2 + 1]
        val vx = j2[cIdx * 2] - j2[bIdx * 2]
        val vy = j2[cIdx * 2 + 1] - j2[bIdx * 2 + 1]
        val un = max(1e-4f, sqrt(ux * ux + uy * uy))
        val vn = max(1e-4f, sqrt(vx * vx + vy * vy))
        val cos2 = ((ux * vx + uy * vy) / (un * vn)).coerceIn(-1f, 1f)
        return (acos(cos2) * 180f / PI.toFloat())
    }

    private fun torsoInclinationFromVerticalDeg(j3: FloatArray, j2: FloatArray): Float {
        val sx = (j3[5 * 3] + j3[6 * 3]) * 0.5f
        val sy = (j3[5 * 3 + 1] + j3[6 * 3 + 1]) * 0.5f
        val sz = (j3[5 * 3 + 2] + j3[6 * 3 + 2]) * 0.5f
        val hx = (j3[9 * 3] + j3[10 * 3]) * 0.5f
        val hy = (j3[9 * 3 + 1] + j3[10 * 3 + 1]) * 0.5f
        val hz = (j3[9 * 3 + 2] + j3[10 * 3 + 2]) * 0.5f

        val dx = sx - hx
        val dy = sy - hy // negative in Y-down upright posture
        val dz = sz - hz
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len > 1e-4f) {
            // Vertical up vector in Y-down camera coords is (0, -1, 0)
            val cosUp = (-dy / len).coerceIn(-1f, 1f)
            return acos(cosUp) * 180f / PI.toFloat()
        }
        val dx2 = (j2[5 * 2] + j2[6 * 2]) * 0.5f - (j2[9 * 2] + j2[10 * 2]) * 0.5f
        val dy2 = (j2[5 * 2 + 1] + j2[6 * 2 + 1]) * 0.5f - (j2[9 * 2 + 1] + j2[10 * 2 + 1]) * 0.5f
        val len2 = max(1e-4f, sqrt(dx2 * dx2 + dy2 * dy2))
        return acos((-dy2 / len2).coerceIn(-1f, 1f)) * 180f / PI.toFloat()
    }

    private fun bodyPlankAngleDeg(j3: FloatArray, j2: FloatArray): Float {
        val sx = (j3[5 * 3] + j3[6 * 3]) * 0.5f
        val sy = (j3[5 * 3 + 1] + j3[6 * 3 + 1]) * 0.5f
        val sz = (j3[5 * 3 + 2] + j3[6 * 3 + 2]) * 0.5f
        val hx = (j3[9 * 3] + j3[10 * 3]) * 0.5f
        val hy = (j3[9 * 3 + 1] + j3[10 * 3 + 1]) * 0.5f
        val hz = (j3[9 * 3 + 2] + j3[10 * 3 + 2]) * 0.5f
        val ax = (j3[13 * 3] + j3[14 * 3]) * 0.5f
        val ay = (j3[13 * 3 + 1] + j3[14 * 3 + 1]) * 0.5f
        val az = (j3[13 * 3 + 2] + j3[14 * 3 + 2]) * 0.5f

        val v1x = sx - hx; val v1y = sy - hy; val v1z = sz - hz
        val v2x = ax - hx; val v2y = ay - hy; val v2z = az - hz
        val n1 = max(1e-4f, sqrt(v1x * v1x + v1y * v1y + v1z * v1z))
        val n2 = max(1e-4f, sqrt(v2x * v2x + v2y * v2y + v2z * v2z))
        val cosT = ((v1x * v2x + v1y * v2y + v1z * v2z) / (n1 * n2)).coerceIn(-1f, 1f)
        val deg = acos(cosT) * 180f / PI.toFloat()
        if (deg > 1f) return deg
        return angle3DOr2D(j3, j2, 5, 9, 13)
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

package com.example.domain.model

enum class SubscriptionTier(
    val id: String,
    val displayName: String,
    val priceLabel: String,
    val tagline: String
) {
    BASIC(
        id = "basic",
        displayName = "Plano Básico",
        priceLabel = "Grátis",
        tagline = "4 exercícios principais + 16 essenciais, câmera ao vivo e contador de reps"
    ),
    PREMIUM(
        id = "premium",
        displayName = "FitVerso Pro",
        priceLabel = "R$ 29,90/mês",
        tagline = "100 exercícios, detecção Auto, Avatar 3D HD, Coach IA por voz e exportação JSON/CSV"
    );

    companion object {
        fun fromId(id: String): SubscriptionTier = entries.firstOrNull { it.id == id } ?: BASIC
    }
}

enum class MeasurementUnit(val id: String, val label: String) {
    METRIC("metric", "Métrico"),
    IMPERIAL("imperial", "Imperial");

    companion object {
        fun fromId(id: String): MeasurementUnit = entries.firstOrNull { it.id == id } ?: METRIC
    }
}

@kotlinx.serialization.Serializable
data class WorkoutHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val dateLabel: String,
    val exerciseId: String,
    val exerciseName: String,
    val reps: Int,
    val durationSec: Int,
    val setsCompleted: Int = 1,
    val avgFormScorePct: Int = 92
)

data class UserFitnessProfile(
    val userName: String = "Atleta",
    val tier: SubscriptionTier = SubscriptionTier.BASIC,
    val totalLifetimeReps: Int = 0,
    val workoutsCompleted: Int = 0,
    val dailyGoalReps: Int = 60,
    val streakDays: Int = 1,
    val weeklyGoalDays: Int = 3,
    val remindersEnabled: Boolean = true,
    val unitSystem: MeasurementUnit = MeasurementUnit.METRIC,
    val defaultFrontCamera: Boolean = false
)

enum class ModelVariant(
    val id: String,
    val displayName: String,
    val paramsMillions: Float,
    val baseOnnxName: String,
    val emdb1MmFit0: Float,
    val emdb1MmFit1: Float,
    val emdb1MmFit2: Float,
    val pw3dMmFit0: Float,
    val pw3dMmFit1: Float,
    val pw3dMmFit2: Float,
    val npuMsFit0: Float,
    val npuMsFit1: Float,
    val npuMsFit2: Float,
    val commercialAllowed: Boolean,
    val licenseLabel: String
) {
    NANO(
        id = "nano",
        displayName = "InstantHMR Nano",
        paramsMillions = 20.0f,
        baseOnnxName = "instanthmr-nano.onnx",
        emdb1MmFit0 = 61.7f, emdb1MmFit1 = 57.2f, emdb1MmFit2 = 55.9f,
        pw3dMmFit0 = 46.6f, pw3dMmFit1 = 44.5f, pw3dMmFit2 = 44.1f,
        npuMsFit0 = 4.60f, npuMsFit1 = 5.49f, npuMsFit2 = 8.73f,
        commercialAllowed = false,
        licenseLabel = "SAM License (Non-Commercial)"
    ),
    TINY(
        id = "tiny",
        displayName = "InstantHMR Tiny",
        paramsMillions = 20.3f,
        baseOnnxName = "instanthmr-tiny.onnx",
        emdb1MmFit0 = 58.0f, emdb1MmFit1 = 53.8f, emdb1MmFit2 = 52.6f,
        pw3dMmFit0 = 44.0f, pw3dMmFit1 = 42.4f, pw3dMmFit2 = 42.0f,
        npuMsFit0 = 4.84f, npuMsFit1 = 5.84f, npuMsFit2 = 9.20f,
        commercialAllowed = false,
        licenseLabel = "SAM License (Non-Commercial)"
    ),
    SMALL(
        id = "small",
        displayName = "InstantHMR Small",
        paramsMillions = 24.4f,
        baseOnnxName = "instanthmr-small.onnx",
        emdb1MmFit0 = 53.3f, emdb1MmFit1 = 49.2f, emdb1MmFit2 = 48.1f,
        pw3dMmFit0 = 41.7f, pw3dMmFit1 = 40.3f, pw3dMmFit2 = 40.0f,
        npuMsFit0 = 5.38f, npuMsFit1 = 6.37f, npuMsFit2 = 10.36f,
        commercialAllowed = false,
        licenseLabel = "SAM License (Non-Commercial)"
    ),
    MEDIUM(
        id = "medium",
        displayName = "InstantHMR Medium",
        paramsMillions = 32.2f,
        baseOnnxName = "instanthmr-medium.onnx",
        emdb1MmFit0 = 48.8f, emdb1MmFit1 = 45.2f, emdb1MmFit2 = 44.4f,
        pw3dMmFit0 = 40.0f, pw3dMmFit1 = 39.4f, pw3dMmFit2 = 39.3f,
        npuMsFit0 = 5.49f, npuMsFit1 = 6.40f, npuMsFit2 = 10.22f,
        commercialAllowed = false,
        licenseLabel = "SAM License (Non-Commercial)"
    ),
    MEDIUM_COMMERCIAL(
        id = "medium-commercial",
        displayName = "Medium Commercial",
        paramsMillions = 32.2f,
        baseOnnxName = "instanthmr-medium-commercial.onnx",
        emdb1MmFit0 = 72.2f, emdb1MmFit1 = 68.8f, emdb1MmFit2 = 68.3f,
        pw3dMmFit0 = 50.7f, pw3dMmFit1 = 47.7f, pw3dMmFit2 = 47.5f,
        npuMsFit0 = 5.49f, npuMsFit1 = 6.40f, npuMsFit2 = 10.22f,
        commercialAllowed = true,
        licenseLabel = "SAM License (Commercial OK)"
    );

    fun onnxFilename(fit: Int): String = when (fit) {
        1, 2 -> baseOnnxName.replace(".onnx", "-fit$fit.onnx")
        else -> baseOnnxName
    }

    fun emdb1ForFit(fit: Int): Float = when (fit) {
        1 -> emdb1MmFit1
        2 -> emdb1MmFit2
        else -> emdb1MmFit0
    }

    fun pw3dForFit(fit: Int): Float = when (fit) {
        1 -> pw3dMmFit1
        2 -> pw3dMmFit2
        else -> pw3dMmFit0
    }

    fun npuMsForFit(fit: Int): Float = when (fit) {
        1 -> npuMsFit1
        2 -> npuMsFit2
        else -> npuMsFit0
    }

    companion object {
        const val HF_REPO_ID = "momolesang/InstantHMR"
        const val BODY_ONNX = "mhr-body.onnx"
        fun fromId(id: String): ModelVariant = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

enum class ExecutionDevice(val id: String, val label: String) {
    CPU("cpu", "CPU"),
    XNNPACK("xnnpack", "XNNPACK"),
    NNAPI("nnapi", "NNAPI (NPU/DSP)");

    companion object {
        fun fromId(id: String): ExecutionDevice = entries.firstOrNull { it.id == id } ?: XNNPACK
    }
}

enum class PoseFilterKind(val id: String, val label: String) {
    NONE("none", "Off"),
    ONE_EURO("one-euro", "One-Euro"),
    ALPHA_BETA("alpha-beta", "Alpha-Beta");

    companion object {
        fun fromId(id: String): PoseFilterKind = entries.firstOrNull { it.id == id } ?: ONE_EURO
    }
}

enum class InputSourceMode(val label: String) {
    PHOTO("Foto"),
    VIDEO("Vídeo"),
    CAMERA("Câmera")
}

enum class ViewportMode(val label: String) {
    SPLIT("Split"),
    TWO_D("2D"),
    THREE_D("3D")
}

data class PipelineConfig(
    val modelVariant: ModelVariant = ModelVariant.MEDIUM,
    val fitLevel: Int = 2,
    val executionDevice: ExecutionDevice = ExecutionDevice.XNNPACK,
    val maxPersons: Int = 4,
    val detConfidence: Float = 0.5f,
    val detectorStride: Int = 1,
    val customFocalPx: Float? = null,
    val poseFilter: PoseFilterKind = PoseFilterKind.ONE_EURO,
    val smoothOutput: Boolean = false,
    val smoothBetaScale: Float = 1.0f,
    val showMesh: Boolean = true,
    val meshAlpha: Float = 0.42f,
    val customOnnxPath: String? = null,
    val customOnnxName: String? = null,
    val showDiagnosticHud: Boolean = false
)

data class HmrPrediction(
    val bbox: FloatArray,
    val confidence: Float,
    val joints2d: FloatArray,
    val joints3dLocal: FloatArray,
    val camTrans: FloatArray,
    val mhrParams: FloatArray,
    val shapeParams: FloatArray,
    val focalLength: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val verticesLocal: FloatArray,
    val isOnnxFullMesh: Boolean
) {
    val joints3dCam: FloatArray
        get() = FloatArray(joints3dLocal.size) { i ->
            joints3dLocal[i] + camTrans[i % 3]
        }

    val verticesCam: FloatArray
        get() = FloatArray(verticesLocal.size) { i ->
            verticesLocal[i] + camTrans[i % 3]
        }

    fun copyWith(
        joints2d: FloatArray = this.joints2d,
        joints3dLocal: FloatArray = this.joints3dLocal,
        verticesLocal: FloatArray = this.verticesLocal,
        camTrans: FloatArray = this.camTrans
    ): HmrPrediction = copy(
        joints2d = joints2d,
        joints3dLocal = joints3dLocal,
        verticesLocal = verticesLocal,
        camTrans = camTrans
    )
}

data class FrameResult(
    val persons: List<HmrPrediction>,
    val faces: IntArray,
    val detectorMs: Float,
    val hmrMs: Float,
    val engineLabel: String,
    val frameIndex: Int,
    val timestampSec: Float
) {
    val totalMs: Float get() = detectorMs + hmrMs
    val fps: Float get() = if (totalMs > 0f) 1000f / totalMs else 0f
}

data class TimingSample(
    val frameIndex: Int,
    val detectorMs: Float,
    val hmrMs: Float
)

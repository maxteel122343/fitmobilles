package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AiCoachRepository
import com.example.data.CoachMessage
import com.example.data.FrameExporter
import com.example.data.HybridMultiProviderAiCoachRepository
import com.example.data.SettingsDataStore
import com.example.data.WorkoutSnapshotRecorder
import com.example.domain.fitness.ExerciseCatalog
import com.example.domain.fitness.ExerciseDefinition
import com.example.domain.fitness.ExerciseFeedback
import com.example.domain.fitness.FormStatus
import com.example.domain.fitness.GoodGymRepCounter
import com.example.domain.model.AiProvidersConfig
import com.example.domain.model.FrameResult
import com.example.domain.model.InputSourceMode
import com.example.domain.model.MeasurementUnit
import com.example.domain.model.ModelVariant
import com.example.domain.model.PipelineConfig
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.TimingSample
import com.example.domain.model.UserFitnessProfile
import com.example.domain.model.ViewportMode
import com.example.domain.model.WorkoutHistoryEntry
import com.example.ml.CachedModelInfo
import com.example.ml.ModelDownloadState
import com.example.ml.ModelRepository
import com.example.ml.PosePipeline
import com.example.voice.VoiceCoachManager
import com.example.web.LocalWebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class VideoPlaybackState(
    val uri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

data class ExerciseTeachUiState(
    val exerciseId: String = "",
    val isLoadingText: Boolean = false,
    val explanation: String? = null,
    val textSourceLabel: String? = null,
    val isLoadingImage: Boolean = false,
    val generatedImage: Bitmap? = null,
    val imageSourceLabel: String? = null,
    val errorMessage: String? = null
)

data class StudioUiState(
    val sourceMode: InputSourceMode = InputSourceMode.PHOTO,
    val viewportMode: ViewportMode = ViewportMode.SPLIT,
    val currentBitmap: Bitmap? = null,
    val frameWidth: Int = 1,
    val frameHeight: Int = 1,
    val frameResult: FrameResult? = null,
    val timingHistory: List<TimingSample> = emptyList(),
    val isProcessing: Boolean = false,
    val statusBanner: String? = null,
    val activeSampleIndex: Int = 0,
    val useFrontCamera: Boolean = false,
    val videoState: VideoPlaybackState = VideoPlaybackState(),
    val isSelectedVariantCached: Boolean = false,
    val cachedModels: List<CachedModelInfo> = emptyList(),
    // Live workout session telemetry
    val selectedExerciseId: String = "squat",
    val autoDetectExercise: Boolean = false,
    val exerciseFeedback: ExerciseFeedback? = null,
    val totalSessionReps: Int = 0,
    val currentSetNumber: Int = 1,
    val totalPlannedSets: Int = 3,
    val workoutElapsedSec: Int = 0,
    val isWorkoutPaused: Boolean = false,
    val isRecordingSnapshots: Boolean = true,
    val snapshotCount: Int = 0,
    val voiceFeedbackEnabled: Boolean = true,
    val coachMessages: List<CoachMessage> = listOf(
        CoachMessage(
            id = "welcome",
            fromUser = false,
            text = "Olá! Sou o Coach do FitVerso. Escolha entre ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios ou configure suas APIs (Gemini, DeepSeek, ChatGPT, Voz e Imagem) para coaching personalizado.",
            providerTag = "FitVerso Coach",
            isPlaceholder = true
        )
    ),
    val isCoachThinking: Boolean = false,
    val paywallFeaturePrompt: String? = null
)

class InstantHmrViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsDataStore = SettingsDataStore(appContext)
    val modelRepository = ModelRepository(appContext)
    private val pipeline = PosePipeline(modelRepository)

    private val repCounter = GoodGymRepCounter()
    private val snapshotRecorder = WorkoutSnapshotRecorder()
    private val aiCoachRepository: AiCoachRepository = HybridMultiProviderAiCoachRepository()
    private val voiceManager = VoiceCoachManager(appContext)
    private val localWebServer = LocalWebServer(appContext)

    private val _config = MutableStateFlow(PipelineConfig())
    val config: StateFlow<PipelineConfig> = _config.asStateFlow()

    private val _aiConfig = MutableStateFlow(AiProvidersConfig())
    val aiConfig: StateFlow<AiProvidersConfig> = _aiConfig.asStateFlow()

    private val _teachState = MutableStateFlow(ExerciseTeachUiState())
    val teachState: StateFlow<ExerciseTeachUiState> = _teachState.asStateFlow()

    private val _webServerRunning = MutableStateFlow(false)
    val webServerRunning: StateFlow<Boolean> = _webServerRunning.asStateFlow()

    private val _webServerUrls = MutableStateFlow<List<String>>(emptyList())
    val webServerUrls: StateFlow<List<String>> = _webServerUrls.asStateFlow()

    private val _userProfile = MutableStateFlow(UserFitnessProfile())
    val userProfile: StateFlow<UserFitnessProfile> = _userProfile.asStateFlow()

    private val _workoutHistory = MutableStateFlow<List<WorkoutHistoryEntry>>(emptyList())
    val workoutHistory: StateFlow<List<WorkoutHistoryEntry>> = _workoutHistory.asStateFlow()

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val downloadState: StateFlow<ModelDownloadState> = modelRepository.downloadState
    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val voiceStatus: StateFlow<String?> = voiceManager.voiceStatus

    private val cameraBusy = AtomicBoolean(false)
    private var cameraStartNs = 0L
    private var sessionStartNs = System.nanoTime()
    private var videoJob: Job? = null
    private var photoJob: Job? = null
    private var workoutTimerJob: Job? = null

    private var formGoodSamples = 0
    private var formTotalSamples = 0

    init {
        viewModelScope.launch {
            settingsDataStore.configFlow.collectLatest { loaded ->
                _config.value = loaded
                refreshCacheStatus(loaded)
                if (_uiState.value.sourceMode == InputSourceMode.PHOTO && _uiState.value.currentBitmap != null) {
                    reprocessCurrentStillFrame(loaded)
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.aiConfigFlow.collectLatest { loadedAi ->
                _aiConfig.value = loadedAi
            }
        }
        viewModelScope.launch {
            settingsDataStore.profileFlow.collectLatest { profile ->
                _userProfile.value = profile
                _uiState.update { it.copy(useFrontCamera = profile.defaultFrontCamera) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.historyFlow.collectLatest { entries ->
                _workoutHistory.value = entries
            }
        }
        startWorkoutTimer()
        loadSamplePhoto(0)
    }

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _uiState.value
                val active = cur.sourceMode == InputSourceMode.CAMERA ||
                    (cur.sourceMode == InputSourceMode.VIDEO && cur.videoState.isPlaying)
                if (active && !cur.isWorkoutPaused) {
                    _uiState.update { it.copy(workoutElapsedSec = it.workoutElapsedSec + 1) }
                }
            }
        }
    }

    fun isProUser(): Boolean = _userProfile.value.tier == SubscriptionTier.PREMIUM

    fun setSubscriptionTier(tier: SubscriptionTier) {
        viewModelScope.launch {
            settingsDataStore.setSubscriptionTier(tier)
            _uiState.update {
                it.copy(
                    paywallFeaturePrompt = null,
                    statusBanner = if (tier == SubscriptionTier.PREMIUM) {
                        "FitVerso Pro ativado! Todos os ${ExerciseCatalog.ALL_EXERCISES.size}+ exercícios e recursos liberados."
                    } else {
                        "Você está usando o Plano Básico (Gratuito)."
                    }
                )
            }
        }
    }

    fun saveAiProvidersConfig(updated: AiProvidersConfig) {
        _aiConfig.value = updated
        viewModelScope.launch {
            settingsDataStore.saveAiConfig(updated)
            _uiState.update {
                it.copy(
                    statusBanner = "APIs salvas: ${updated.textProvider.displayName} · ${updated.voiceProvider.displayName} · ${updated.imageProvider.displayName}"
                )
            }
        }
    }

    fun testVoiceWithConfig(cfg: AiProvidersConfig = _aiConfig.value) {
        voiceManager.speakWithConfig(
            text = "Olá! Voz do Coach FitVerso configurada com sucesso.",
            aiConfig = cfg,
            force = true,
            onError = { msg -> _uiState.update { it.copy(statusBanner = msg) } }
        )
    }

    fun openExerciseTeaching(exercise: ExerciseDefinition) {
        _teachState.value = ExerciseTeachUiState(
            exerciseId = exercise.id,
            explanation = buildString {
                appendLine("Passo a passo biomecânico — ${exercise.namePt}:")
                exercise.resolvedSteps().forEach { appendLine(it) }
                appendLine()
                append("Erro comum: ${exercise.commonMistake}")
            },
            textSourceLabel = "Guia Biomecânico FitVerso"
        )
    }

    fun teachExerciseWithAi(exercise: ExerciseDefinition) {
        viewModelScope.launch {
            _teachState.update {
                it.copy(
                    exerciseId = exercise.id,
                    isLoadingText = true,
                    errorMessage = null
                )
            }
            val (explanation, sourceLabel) = aiCoachRepository.teachExerciseCorrectForm(_aiConfig.value, exercise)
            _teachState.update {
                if (it.exerciseId != exercise.id) it
                else it.copy(
                    isLoadingText = false,
                    explanation = explanation,
                    textSourceLabel = sourceLabel
                )
            }
        }
    }

    fun generateExerciseImageWithAi(exercise: ExerciseDefinition) {
        viewModelScope.launch {
            _teachState.update {
                it.copy(
                    exerciseId = exercise.id,
                    isLoadingImage = true,
                    errorMessage = null
                )
            }
            val result = aiCoachRepository.generateExerciseVisual(appContext, _aiConfig.value, exercise)
            result.onSuccess { (bmp, label) ->
                _teachState.update {
                    if (it.exerciseId != exercise.id) it
                    else it.copy(
                        isLoadingImage = false,
                        generatedImage = bmp,
                        imageSourceLabel = label
                    )
                }
            }.onFailure { err ->
                _teachState.update {
                    if (it.exerciseId != exercise.id) it
                    else it.copy(
                        isLoadingImage = false,
                        errorMessage = err.message ?: "Falha ao gerar ilustração"
                    )
                }
            }
        }
    }

    fun speakExerciseGuide(text: String) {
        voiceManager.speakWithConfig(
            text = text,
            aiConfig = _aiConfig.value,
            force = true,
            onError = { msg -> _uiState.update { it.copy(statusBanner = msg) } }
        )
    }

    fun toggleWebServer() {
        if (localWebServer.isRunning) {
            localWebServer.stop()
            _webServerRunning.value = false
            _webServerUrls.value = emptyList()
            _uiState.update { it.copy(statusBanner = "Servidor Web local parado") }
        } else {
            val res = localWebServer.start()
            res.onSuccess { urls ->
                _webServerRunning.value = true
                _webServerUrls.value = urls
                _uiState.update { it.copy(statusBanner = "FitVerso Web ativo em ${urls.firstOrNull() ?: "http://127.0.0.1:8765"}") }
            }.onFailure { err ->
                _webServerRunning.value = false
                _uiState.update { it.copy(statusBanner = "Erro ao iniciar servidor Web: ${err.message}") }
            }
        }
    }

    fun exportWebAppHtml(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = localWebServer.readWebAssetBytes()
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    } ?: error("Não foi possível gravar o arquivo HTML")
                }
            }
            _uiState.update {
                it.copy(
                    statusBanner = res.fold(
                        onSuccess = { "fitverso_web.html exportado! Abra no navegador do PC ou celular." },
                        onFailure = { e -> "Falha ao exportar HTML: ${e.message}" }
                    )
                )
            }
        }
    }

    fun exportProjectZip(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    var totalBytes = 0L
                    appContext.assets.open("fitverso_projeto_completo.zip").use { input ->
                        appContext.contentResolver.openOutputStream(uri)?.use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                totalBytes += read
                            }
                            output.flush()
                        } ?: error("Não foi possível criar o arquivo ZIP")
                    }
                    totalBytes
                }
            }
            _uiState.update {
                it.copy(
                    statusBanner = res.fold(
                        onSuccess = { bytes ->
                            val mb = String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
                            "Projeto completo salvo em ZIP ($mb)!"
                        },
                        onFailure = { e -> "Falha ao exportar ZIP do projeto: ${e.message}" }
                    )
                )
            }
        }
    }

    fun updateProfilePreferences(
        userName: String? = null,
        weeklyGoalDays: Int? = null,
        remindersEnabled: Boolean? = null,
        unitSystem: MeasurementUnit? = null,
        defaultFrontCamera: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsDataStore.updateProfilePreferences(
                userName = userName,
                weeklyGoalDays = weeklyGoalDays,
                remindersEnabled = remindersEnabled,
                unitSystem = unitSystem,
                defaultFrontCamera = defaultFrontCamera
            )
            if (defaultFrontCamera != null) {
                _uiState.update { it.copy(useFrontCamera = defaultFrontCamera) }
            }
        }
    }

    fun clearAllHistoryData() {
        viewModelScope.launch {
            settingsDataStore.clearAllWorkoutHistory()
            _uiState.update {
                it.copy(
                    totalSessionReps = 0,
                    statusBanner = "Histórico de treinos apagado"
                )
            }
        }
    }

    fun requestPaywall(featureLabel: String) {
        _uiState.update { it.copy(paywallFeaturePrompt = featureLabel) }
    }

    fun dismissPaywall() {
        _uiState.update { it.copy(paywallFeaturePrompt = null) }
    }

    fun selectExercise(exerciseId: String, autoDetect: Boolean = false) {
        if (!isProUser() && !ExerciseCatalog.isIncludedInBasic(exerciseId)) {
            requestPaywall(ExerciseCatalog.findById(exerciseId).namePt)
            return
        }
        repCounter.reset(exerciseId)
        _uiState.update {
            it.copy(
                selectedExerciseId = exerciseId,
                autoDetectExercise = autoDetect,
                isWorkoutPaused = false,
                statusBanner = if (autoDetect) "Detecção automática ativada" else "Treino: ${ExerciseCatalog.findById(exerciseId).namePt}"
            )
        }
        val cur = _uiState.value.frameResult
        if (cur != null) evaluateFitnessMetrics(cur, recordSnapshot = false)
    }

    fun toggleAutoDetectExercise() {
        val next = !_uiState.value.autoDetectExercise
        repCounter.reset(_uiState.value.selectedExerciseId)
        _uiState.update {
            it.copy(
                autoDetectExercise = next,
                statusBanner = if (next) "Detecção automática ativada" else "Modo manual ativado"
            )
        }
    }

    fun toggleWorkoutPause() {
        _uiState.update { cur ->
            val nextPaused = !cur.isWorkoutPaused
            cur.copy(
                isWorkoutPaused = nextPaused,
                statusBanner = if (nextPaused) "Treino pausado" else "Treino retomado"
            )
        }
    }

    fun resetRepCounter() {
        repCounter.reset(_uiState.value.selectedExerciseId)
        _uiState.update { it.copy(exerciseFeedback = it.exerciseFeedback?.copy(repCount = 0)) }
    }

    fun endAndSaveWorkout() {
        val cur = _uiState.value
        val activeEx = cur.exerciseFeedback?.exercise ?: ExerciseCatalog.findById(cur.selectedExerciseId)
        val repsDone = max(cur.exerciseFeedback?.repCount ?: 0, cur.totalSessionReps)
        val effectiveReps = max(repsDone, 1)
        val duration = max(cur.workoutElapsedSec, 15)
        val scorePct = if (formTotalSamples > 0) {
            ((formGoodSamples.toFloat() / formTotalSamples) * 100f).roundToInt().coerceIn(60, 100)
        } else {
            92
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val entry = WorkoutHistoryEntry(
            id = System.currentTimeMillis().toString(),
            timestampMs = System.currentTimeMillis(),
            dateLabel = dateStr,
            exerciseId = activeEx.id,
            exerciseName = activeEx.namePt,
            reps = effectiveReps,
            durationSec = duration,
            setsCompleted = cur.currentSetNumber,
            avgFormScorePct = scorePct
        )

        viewModelScope.launch {
            settingsDataStore.recordCompletedWorkout(entry)
            repCounter.reset(cur.selectedExerciseId)
            formGoodSamples = 0
            formTotalSamples = 0
            _uiState.update {
                it.copy(
                    exerciseFeedback = it.exerciseFeedback?.copy(repCount = 0),
                    totalSessionReps = 0,
                    workoutElapsedSec = 0,
                    currentSetNumber = 1,
                    isWorkoutPaused = false,
                    statusBanner = "Treino encerrado e salvo: ${activeEx.namePt} · $effectiveReps reps"
                )
            }
        }
    }

    fun toggleVoiceFeedback() {
        val next = !_uiState.value.voiceFeedbackEnabled
        if (!next) voiceManager.stopSpeaking()
        _uiState.update { it.copy(voiceFeedbackEnabled = next) }
    }

    fun toggleSnapshotRecording() {
        _uiState.update { it.copy(isRecordingSnapshots = !it.isRecordingSnapshots) }
    }

    fun clearSnapshots() {
        snapshotRecorder.clear()
        _uiState.update { it.copy(snapshotCount = 0, statusBanner = "Snapshots limpos") }
    }

    fun exportSnapshotsJson(uri: Uri) {
        viewModelScope.launch {
            val res = snapshotRecorder.exportJson(appContext, uri)
            _uiState.update {
                it.copy(
                    statusBanner = res.fold(
                        onSuccess = { n -> "Exportados $n snapshots em JSON" },
                        onFailure = { "Falha ao exportar JSON" }
                    )
                )
            }
        }
    }

    fun exportSnapshotsCsv(uri: Uri) {
        viewModelScope.launch {
            val res = snapshotRecorder.exportCsv(appContext, uri)
            _uiState.update {
                it.copy(
                    statusBanner = res.fold(
                        onSuccess = { n -> "Exportados $n snapshots em CSV" },
                        onFailure = { "Falha ao exportar CSV" }
                    )
                )
            }
        }
    }

    fun sendCoachMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _uiState.value.isCoachThinking) return
        val userMsg = CoachMessage(id = System.nanoTime().toString(), fromUser = true, text = trimmed)
        _uiState.update {
            it.copy(coachMessages = it.coachMessages + userMsg, isCoachThinking = true)
        }
        viewModelScope.launch {
            val cur = _uiState.value
            val currentAi = _aiConfig.value
            val reply = aiCoachRepository.askCoach(
                aiConfig = currentAi,
                userPrompt = trimmed,
                latestFeedback = cur.exerciseFeedback,
                totalSessionReps = cur.totalSessionReps,
                snapshotCount = cur.snapshotCount,
                focusExercise = ExerciseCatalog.findById(cur.selectedExerciseId)
            )
            _uiState.update {
                it.copy(coachMessages = it.coachMessages + reply, isCoachThinking = false)
            }
            if (_uiState.value.voiceFeedbackEnabled) {
                voiceManager.speakWithConfig(
                    text = reply.text,
                    aiConfig = currentAi,
                    force = true,
                    onError = { msg -> _uiState.update { st -> st.copy(statusBanner = msg) } }
                )
            }
        }
    }

    fun startVoiceCoachListening() {
        if (!isProUser()) {
            requestPaywall("Conversação por Voz com Coach IA")
            return
        }
        voiceManager.startListening(
            onResult = { spoken -> sendCoachMessage(spoken) },
            onError = { err -> _uiState.update { it.copy(statusBanner = err) } }
        )
    }

    fun stopVoiceCoachListening() {
        voiceManager.stopListening()
    }

    fun updateConfig(transform: (PipelineConfig) -> PipelineConfig) {
        val updated = transform(_config.value)
        _config.value = updated
        refreshCacheStatus(updated)
        viewModelScope.launch {
            settingsDataStore.saveConfig(updated)
        }
        if (_uiState.value.sourceMode == InputSourceMode.PHOTO && _uiState.value.currentBitmap != null) {
            reprocessCurrentStillFrame(updated)
        }
    }

    fun setViewportMode(mode: ViewportMode) {
        _uiState.update { it.copy(viewportMode = mode) }
    }

    fun setSourceMode(mode: InputSourceMode) {
        if (_uiState.value.sourceMode == mode) return
        stopVideoLoop()
        pipeline.resetTracking()
        cameraStartNs = System.nanoTime()
        _uiState.update {
            it.copy(
                sourceMode = mode,
                timingHistory = emptyList(),
                isWorkoutPaused = false,
                statusBanner = null
            )
        }
        if (mode == InputSourceMode.PHOTO && _uiState.value.currentBitmap == null) {
            loadSamplePhoto(_uiState.value.activeSampleIndex)
        }
    }

    fun toggleCameraLens() {
        pipeline.resetTracking()
        _uiState.update { it.copy(useFrontCamera = !it.useFrontCamera, timingHistory = emptyList()) }
    }

    fun loadSamplePhoto(index: Int) {
        stopVideoLoop()
        val resId = if (index == 1) R.drawable.img_sample_dancer else R.drawable.img_sample_runner
        _uiState.update { it.copy(sourceMode = InputSourceMode.PHOTO, activeSampleIndex = index) }
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusBanner = null) }
            val bmp = withContext(Dispatchers.IO) { decodeSampleBitmap(resId) }
            processStillBitmap(bmp)
        }
    }

    fun loadPhotoUri(uri: Uri) {
        stopVideoLoop()
        _uiState.update { it.copy(sourceMode = InputSourceMode.PHOTO, activeSampleIndex = -1) }
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusBanner = null) }
            val bmp = withContext(Dispatchers.IO) { decodeScaledUriBitmap(uri, maxSide = 1280) }
            if (bmp != null) {
                processStillBitmap(bmp)
            } else {
                _uiState.update { it.copy(isProcessing = false, statusBanner = "Não foi possível abrir a foto") }
            }
        }
    }

    private fun reprocessCurrentStillFrame(cfg: PipelineConfig) {
        val bmp = _uiState.value.currentBitmap ?: return
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val res = withContext(Dispatchers.Default) {
                pipeline.resetTracking()
                pipeline.processFrame(bmp, cfg, isStream = false, timestampSec = 0f)
            }
            pushFrameResult(bmp, res, recordSnapshot = false)
        }
    }

    private suspend fun processStillBitmap(bmp: Bitmap) {
        val res = withContext(Dispatchers.Default) {
            pipeline.resetTracking()
            pipeline.processFrame(bmp, _config.value, isStream = false, timestampSec = 0f)
        }
        pushFrameResult(bmp, res, recordSnapshot = true)
    }

    fun loadVideoUri(uri: Uri) {
        stopVideoLoop()
        pipeline.resetTracking()
        _uiState.update {
            it.copy(
                sourceMode = InputSourceMode.VIDEO,
                timingHistory = emptyList(),
                statusBanner = null,
                videoState = VideoPlaybackState(uri = uri, isPlaying = true, positionMs = 0L, durationMs = 0L)
            )
        }
        startVideoLoop(uri, startMs = 0L, autoPlay = true)
    }

    fun toggleVideoPlayPause() {
        val vs = _uiState.value.videoState
        val uri = vs.uri ?: return
        if (vs.isPlaying) {
            stopVideoLoop()
            _uiState.update { it.copy(videoState = it.videoState.copy(isPlaying = false)) }
        } else {
            val restartFrom = if (vs.positionMs >= vs.durationMs - 80L) 0L else vs.positionMs
            startVideoLoop(uri, startMs = restartFrom, autoPlay = true)
        }
    }

    fun seekVideoTo(positionMs: Long) {
        val vs = _uiState.value.videoState
        val uri = vs.uri ?: return
        val wasPlaying = vs.isPlaying
        stopVideoLoop()
        _uiState.update { it.copy(videoState = it.videoState.copy(positionMs = positionMs, isPlaying = wasPlaying)) }
        startVideoLoop(uri, startMs = positionMs, autoPlay = wasPlaying)
    }

    private fun stopVideoLoop() {
        videoJob?.cancel()
        videoJob = null
    }

    private fun startVideoLoop(uri: Uri, startMs: Long, autoPlay: Boolean) {
        videoJob?.cancel()
        videoJob = viewModelScope.launch(Dispatchers.Default) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(appContext, uri)
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 5000L
                _uiState.update { it.copy(videoState = it.videoState.copy(durationMs = durMs, isPlaying = autoPlay)) }

                var posMs = startMs.coerceIn(0L, durMs)
                val stepMs = 45L

                while (isActive) {
                    val tStart = System.nanoTime()
                    val rawFrame = retriever.getFrameAtTime(posMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                    if (rawFrame != null) {
                        val scaled = scaleBitmapIfNeeded(rawFrame, maxSide = 720)
                        if (scaled !== rawFrame) rawFrame.recycle()
                        val res = pipeline.processFrame(
                            bitmap = scaled,
                            config = _config.value,
                            isStream = true,
                            timestampSec = posMs / 1000f
                        )
                        val feedback = evaluateFitnessMetrics(res, recordSnapshot = true)
                        _uiState.update { cur ->
                            val nextHist = (cur.timingHistory + TimingSample(res.frameIndex, res.detectorMs, res.hmrMs))
                                .takeLast(60)
                            val newTotal = cur.totalSessionReps + if (feedback?.justCompletedRep == true) 1 else 0
                            cur.copy(
                                currentBitmap = scaled,
                                frameWidth = scaled.width,
                                frameHeight = scaled.height,
                                frameResult = res,
                                timingHistory = nextHist,
                                exerciseFeedback = feedback,
                                snapshotCount = snapshotRecorder.count,
                                totalSessionReps = newTotal,
                                currentSetNumber = (newTotal / 12) + 1,
                                isProcessing = false,
                                videoState = cur.videoState.copy(positionMs = posMs, durationMs = durMs)
                            )
                        }
                    }
                    if (!autoPlay) break
                    posMs += stepMs
                    if (posMs > durMs) {
                        posMs = 0L
                        pipeline.resetTracking()
                    }
                    val elapsedMs = (System.nanoTime() - tStart) / 1_000_000L
                    delay((stepMs - elapsedMs).coerceAtLeast(1L))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusBanner = "Erro no vídeo: ${e.message ?: "formato não suportado"}",
                        videoState = it.videoState.copy(isPlaying = false)
                    )
                }
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    fun canAcceptCameraFrame(): Boolean =
        _uiState.value.sourceMode == InputSourceMode.CAMERA &&
            !_uiState.value.isWorkoutPaused &&
            !cameraBusy.get()

    fun reportCameraError(message: String) {
        _uiState.update { it.copy(statusBanner = message) }
    }

    fun analyzeCameraBitmap(bitmap: Bitmap) {
        if (_uiState.value.sourceMode != InputSourceMode.CAMERA || _uiState.value.isWorkoutPaused) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        if (!cameraBusy.compareAndSet(false, true)) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }

        if (cameraStartNs == 0L) cameraStartNs = System.nanoTime()
        val tSec = (System.nanoTime() - cameraStartNs) / 1_000_000_000f

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val res = pipeline.processFrame(
                    bitmap = bitmap,
                    config = _config.value,
                    isStream = true,
                    timestampSec = tSec
                )
                val feedback = evaluateFitnessMetrics(res, recordSnapshot = true)
                _uiState.update { cur ->
                    if (cur.sourceMode != InputSourceMode.CAMERA) return@update cur
                    val nextHist = (cur.timingHistory + TimingSample(res.frameIndex, res.detectorMs, res.hmrMs))
                        .takeLast(60)
                    val newTotal = cur.totalSessionReps + if (feedback?.justCompletedRep == true) 1 else 0
                    cur.copy(
                        frameWidth = bitmap.width,
                        frameHeight = bitmap.height,
                        frameResult = res,
                        timingHistory = nextHist,
                        exerciseFeedback = feedback,
                        snapshotCount = snapshotRecorder.count,
                        totalSessionReps = newTotal,
                        currentSetNumber = (newTotal / 12) + 1,
                        isProcessing = false
                    )
                }
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
                cameraBusy.set(false)
            }
        }
    }

    private fun evaluateFitnessMetrics(res: FrameResult, recordSnapshot: Boolean): ExerciseFeedback? {
        val primary = res.persons.firstOrNull() ?: return null
        val cur = _uiState.value
        val feedback = repCounter.evaluate(
            person = primary,
            selectedExerciseId = cur.selectedExerciseId,
            autoDetect = cur.autoDetectExercise
        )
        formTotalSamples++
        if (feedback.formStatus == FormStatus.GOOD) formGoodSamples++

        if (recordSnapshot && cur.isRecordingSnapshots) {
            val elapsed = (System.nanoTime() - sessionStartNs) / 1_000_000_000f
            snapshotRecorder.recordIfDue(primary, feedback, elapsed)
        }
        if (cur.voiceFeedbackEnabled && cur.sourceMode != InputSourceMode.PHOTO) {
            if (feedback.justCompletedRep) {
                voiceManager.speakRep(feedback.repCount)
            } else if (feedback.formStatus == FormStatus.FAULT) {
                voiceManager.speakCue(feedback.formCue)
            }
        }
        return feedback
    }

    fun downloadModel(variant: ModelVariant, fitLevel: Int) {
        viewModelScope.launch {
            val res = modelRepository.downloadVariantAndBody(variant, fitLevel)
            res.onSuccess {
                updateConfig { it.copy(modelVariant = variant, fitLevel = fitLevel, customOnnxPath = null, customOnnxName = null) }
                _uiState.update { it.copy(statusBanner = "Modelo carregado: ${variant.onnxFilename(fitLevel)}") }
            }.onFailure { err ->
                _uiState.update { it.copy(statusBanner = err.message ?: "Falha no download") }
            }
        }
    }

    fun cancelDownload() {
        modelRepository.cancelDownload()
    }

    fun importCustomOnnx(uri: Uri) {
        viewModelScope.launch {
            val res = modelRepository.importCustomOnnx(uri)
            res.onSuccess { file ->
                updateConfig { it.copy(customOnnxPath = file.absolutePath, customOnnxName = file.name) }
                _uiState.update { it.copy(statusBanner = "Importado ${file.name}") }
            }.onFailure { err ->
                _uiState.update { it.copy(statusBanner = err.message ?: "Falha na importação") }
            }
        }
    }

    fun deleteCachedModel(file: File) {
        modelRepository.deleteModel(file)
        if (_config.value.customOnnxPath == file.absolutePath) {
            updateConfig { it.copy(customOnnxPath = null, customOnnxName = null) }
        } else {
            refreshCacheStatus(_config.value)
        }
    }

    fun exportFrameToObj(uri: Uri) {
        val result = _uiState.value.frameResult ?: return
        viewModelScope.launch {
            val res = FrameExporter.exportObj(appContext, uri, result)
            _uiState.update {
                it.copy(statusBanner = if (res.isSuccess) "Mesh 3D .obj exportada" else "Falha ao exportar .obj")
            }
        }
    }

    fun exportFrameToJson(uri: Uri) {
        val result = _uiState.value.frameResult ?: return
        viewModelScope.launch {
            val res = FrameExporter.exportJson(appContext, uri, result)
            _uiState.update {
                it.copy(statusBanner = if (res.isSuccess) "Frame MHR .json exportado" else "Falha ao exportar .json")
            }
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun refreshCacheStatus(cfg: PipelineConfig = _config.value) {
        val ready = modelRepository.resolveActiveModels(cfg.modelVariant, cfg.fitLevel, cfg.customOnnxPath) != null
        val cached = modelRepository.listCachedModels()
        _uiState.update { it.copy(isSelectedVariantCached = ready, cachedModels = cached) }
    }

    private fun pushFrameResult(bmp: Bitmap, res: FrameResult, recordSnapshot: Boolean) {
        val feedback = evaluateFitnessMetrics(res, recordSnapshot)
        _uiState.update { cur ->
            val nextHist = (cur.timingHistory + TimingSample(res.frameIndex, res.detectorMs, res.hmrMs)).takeLast(60)
            cur.copy(
                currentBitmap = bmp,
                frameWidth = bmp.width,
                frameHeight = bmp.height,
                frameResult = res,
                timingHistory = nextHist,
                exerciseFeedback = feedback,
                snapshotCount = snapshotRecorder.count,
                isProcessing = false
            )
        }
    }

    private fun decodeSampleBitmap(@DrawableRes resId: Int): Bitmap {
        val raw = BitmapFactory.decodeResource(appContext.resources, resId)
        return scaleBitmapIfNeeded(raw, maxSide = 1080)
    }

    private fun decodeScaledUriBitmap(uri: Uri, maxSide: Int): Bitmap? {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            val longSide = max(opts.outWidth, opts.outHeight)
            var sample = 1
            while (longSide / sample > maxSide * 2) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null
            scaleBitmapIfNeeded(decoded, maxSide)
        }.getOrNull()
    }

    private fun scaleBitmapIfNeeded(src: Bitmap, maxSide: Int): Bitmap {
        val longSide = max(src.width, src.height)
        if (longSide <= maxSide) return src
        val ratio = maxSide.toFloat() / longSide.toFloat()
        val w = max(1, (src.width * ratio).roundToLong().toInt())
        val h = max(1, (src.height * ratio).roundToLong().toInt())
        val m = Matrix().apply { postScale(w.toFloat() / src.width, h.toFloat() / src.height) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    override fun onCleared() {
        super.onCleared()
        workoutTimerJob?.cancel()
        stopVideoLoop()
        voiceManager.release()
        localWebServer.release()
        pipeline.close()
    }
}

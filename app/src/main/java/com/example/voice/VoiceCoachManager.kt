package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.domain.model.AiProvidersConfig
import com.example.domain.model.VoiceAiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class VoiceCoachManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cloudSpeechJob: Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceStatus = MutableStateFlow<String?>(null)
    val voiceStatus: StateFlow<String?> = _voiceStatus.asStateFlow()

    private var lastCueSpokenMs = 0L

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ptBr = Locale.Builder().setLanguage("pt").setRegion("BR").build()
                val res = tts?.setLanguage(ptBr)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setSpeechRate(1.05f)
                ttsReady = true
            }
        }
    }

    fun speakCue(text: String, force: Boolean = false) {
        if (!ttsReady) return
        val now = System.currentTimeMillis()
        if (!force && now - lastCueSpokenMs < 3500L) return
        lastCueSpokenMs = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue_$now")
    }

    fun speakWithConfig(
        text: String,
        aiConfig: AiProvidersConfig,
        force: Boolean = true,
        onError: ((String) -> Unit)? = null
    ) {
        val cleanText = text.trim().take(1200)
        if (cleanText.isBlank()) return

        val provider = aiConfig.voiceProvider
        val key = aiConfig.voiceApiKey.trim()

        if (provider == VoiceAiProvider.DEVICE_NATIVE ||
            provider == VoiceAiProvider.GEMINI_VOICE ||
            key.isBlank()
        ) {
            speakCue(cleanText, force = force)
            return
        }

        cloudSpeechJob?.cancel()
        cloudSpeechJob = scope.launch {
            _voiceStatus.value = "Gerando áudio (${provider.displayName})..."
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = when (provider) {
                        VoiceAiProvider.OPENAI_TTS -> fetchOpenAiTts(aiConfig, key, cleanText)
                        VoiceAiProvider.ELEVENLABS -> fetchElevenLabsTts(aiConfig, key, cleanText)
                        else -> ByteArray(0)
                    }
                    if (bytes.isEmpty()) error("Áudio vazio retornado pela API de Voz")
                    val outFile = File(context.cacheDir, "fitverso_voice_${System.currentTimeMillis()}.mp3")
                    outFile.writeBytes(bytes)
                    outFile
                }
            }
            _voiceStatus.value = null
            result.onSuccess { audioFile ->
                playAudioFile(audioFile)
            }.onFailure { err ->
                onError?.invoke("Voz Cloud falhou (${err.message}); usando voz nativa.")
                speakCue(cleanText, force = true)
            }
        }
    }

    private fun fetchOpenAiTts(cfg: AiProvidersConfig, apiKey: String, text: String): ByteArray {
        val url = cfg.voiceBaseUrl.ifBlank { VoiceAiProvider.OPENAI_TTS.defaultBaseUrl }
        val model = cfg.voiceModel.ifBlank { VoiceAiProvider.OPENAI_TTS.defaultModel }
        val voice = cfg.voiceId.ifBlank { "alloy" }
        val body = JSONObject()
            .put("model", model)
            .put("voice", voice)
            .put("input", text)
            .put("response_format", "mp3")
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("OpenAI TTS HTTP ${resp.code}")
            return resp.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun fetchElevenLabsTts(cfg: AiProvidersConfig, apiKey: String, text: String): ByteArray {
        val base = cfg.voiceBaseUrl.ifBlank { VoiceAiProvider.ELEVENLABS.defaultBaseUrl }.trimEnd('/')
        val voiceId = cfg.voiceId.ifBlank { VoiceAiProvider.ELEVENLABS.defaultVoiceId }
        val model = cfg.voiceModel.ifBlank { VoiceAiProvider.ELEVENLABS.defaultModel }
        val body = JSONObject()
            .put("text", text)
            .put("model_id", model)
        val req = Request.Builder()
            .url("$base/$voiceId")
            .addHeader("xi-api-key", apiKey)
            .addHeader("Accept", "audio/mpeg")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("ElevenLabs HTTP ${resp.code}")
            return resp.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun playAudioFile(file: File) {
        runCatching {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    runCatching { file.delete() }
                }
                prepare()
                start()
            }
        }
    }

    fun speakRep(repCount: Int) {
        if (!ttsReady) return
        lastCueSpokenMs = System.currentTimeMillis()
        tts?.speak("$repCount", TextToSpeech.QUEUE_FLUSH, null, "rep_$repCount")
    }

    fun stopSpeaking() {
        cloudSpeechJob?.cancel()
        runCatching { tts?.stop() }
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Reconhecimento de voz indisponível neste dispositivo")
            return
        }
        stopListening()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _voiceStatus.value = "Ouvindo..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
                _voiceStatus.value = null
            }
            override fun onError(error: Int) {
                _isListening.value = false
                _voiceStatus.value = null
                onError("Não entendi o áudio (código $error). Tente novamente.")
            }
            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _voiceStatus.value = null
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotBlank()) onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) _voiceStatus.value = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        _isListening.value = false
        _voiceStatus.value = null
        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    fun release() {
        stopListening()
        stopSpeaking()
        scope.cancel()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
    }
}

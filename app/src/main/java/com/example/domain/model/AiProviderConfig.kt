package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextAiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val requiresKey: Boolean = true
) {
    OFFLINE_COACH(
        id = "offline",
        displayName = "Coach Local (Offline)",
        defaultBaseUrl = "",
        defaultModel = "fitverso-biomech-v1",
        suggestedModels = listOf("fitverso-biomech-v1"),
        requiresKey = false
    ),
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash",
        suggestedModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-2.5-flash-lite")
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI ChatGPT",
        defaultBaseUrl = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        suggestedModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "o4-mini")
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/chat/completions",
        defaultModel = "deepseek-chat",
        suggestedModels = listOf("deepseek-chat", "deepseek-reasoner")
    ),
    CUSTOM(
        id = "custom",
        displayName = "API Custom (OpenAI-Compatível)",
        defaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "meta-llama/llama-3.3-70b-instruct",
        suggestedModels = listOf("meta-llama/llama-3.3-70b-instruct", "qwen/qwen-2.5-72b-instruct", "llama3.2")
    )
}

@Serializable
enum class VoiceAiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val defaultVoiceId: String,
    val suggestedModels: List<String>,
    val suggestedVoices: List<String>,
    val requiresKey: Boolean = true
) {
    DEVICE_NATIVE(
        id = "device_native",
        displayName = "Voz Nativa Android (Grátis)",
        defaultBaseUrl = "",
        defaultModel = "pt-BR-local",
        defaultVoiceId = "pt-BR",
        suggestedModels = listOf("pt-BR-local"),
        suggestedVoices = listOf("pt-BR", "en-US"),
        requiresKey = false
    ),
    OPENAI_TTS(
        id = "openai_tts",
        displayName = "OpenAI Audio TTS",
        defaultBaseUrl = "https://api.openai.com/v1/audio/speech",
        defaultModel = "tts-1",
        defaultVoiceId = "alloy",
        suggestedModels = listOf("tts-1", "tts-1-hd", "gpt-4o-mini-tts"),
        suggestedVoices = listOf("alloy", "nova", "echo", "onyx", "shimmer")
    ),
    ELEVENLABS(
        id = "elevenlabs",
        displayName = "ElevenLabs TTS",
        defaultBaseUrl = "https://api.elevenlabs.io/v1/text-to-speech",
        defaultModel = "eleven_multilingual_v2",
        defaultVoiceId = "21m00Tcm4TlvDq8ikWAM",
        suggestedModels = listOf("eleven_multilingual_v2", "eleven_flash_v2_5", "eleven_turbo_v2_5"),
        suggestedVoices = listOf("21m00Tcm4TlvDq8ikWAM", "pNInz6obpgDQGcFmaJgB", "EXAVITQu4vr4xnSDxMaL")
    ),
    GEMINI_VOICE(
        id = "gemini_tts",
        displayName = "Google Gemini Audio",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash",
        defaultVoiceId = "Kore",
        suggestedModels = listOf("gemini-2.5-flash", "gemini-2.5-flash-preview-tts"),
        suggestedVoices = listOf("Kore", "Puck", "Aoede", "Charon")
    )
}

@Serializable
enum class ImageAiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val requiresKey: Boolean = true
) {
    LOCAL_DIAGRAM(
        id = "local_diagram",
        displayName = "Diagrama Biomecânico Local",
        defaultBaseUrl = "",
        defaultModel = "fitverso-pose-guide",
        suggestedModels = listOf("fitverso-pose-guide"),
        requiresKey = false
    ),
    GEMINI_IMAGE(
        id = "gemini_image",
        displayName = "Google Gemini Image / Imagen",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash-image",
        suggestedModels = listOf("gemini-2.5-flash-image", "imagen-3.0-generate-002")
    ),
    OPENAI_IMAGE(
        id = "openai_image",
        displayName = "OpenAI Images (GPT-Image / DALL·E)",
        defaultBaseUrl = "https://api.openai.com/v1/images/generations",
        defaultModel = "gpt-image-1",
        suggestedModels = listOf("gpt-image-1", "dall-e-3")
    ),
    CUSTOM_IMAGE(
        id = "custom_image",
        displayName = "API Custom de Imagem",
        defaultBaseUrl = "https://api.together.xyz/v1/images/generations",
        defaultModel = "black-forest-labs/FLUX.1-schnell",
        suggestedModels = listOf("black-forest-labs/FLUX.1-schnell", "stabilityai/sdxl")
    )
}

@Serializable
data class AiProvidersConfig(
    // Texto (Coach & Explicação dos Exercícios)
    val textProvider: TextAiProvider = TextAiProvider.OFFLINE_COACH,
    val textModel: String = TextAiProvider.OFFLINE_COACH.defaultModel,
    val textApiKey: String = "",
    val textBaseUrl: String = TextAiProvider.GEMINI.defaultBaseUrl,
    // Voz (TTS / Fala)
    val voiceProvider: VoiceAiProvider = VoiceAiProvider.DEVICE_NATIVE,
    val voiceModel: String = VoiceAiProvider.DEVICE_NATIVE.defaultModel,
    val voiceId: String = VoiceAiProvider.DEVICE_NATIVE.defaultVoiceId,
    val voiceApiKey: String = "",
    val voiceBaseUrl: String = VoiceAiProvider.OPENAI_TTS.defaultBaseUrl,
    // Geração de Imagem (Ilustração da forma correta)
    val imageProvider: ImageAiProvider = ImageAiProvider.LOCAL_DIAGRAM,
    val imageModel: String = ImageAiProvider.LOCAL_DIAGRAM.defaultModel,
    val imageApiKey: String = "",
    val imageBaseUrl: String = ImageAiProvider.OPENAI_IMAGE.defaultBaseUrl
)

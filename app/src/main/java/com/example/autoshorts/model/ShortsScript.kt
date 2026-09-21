package com.example.autoshorts.model

import androidx.compose.ui.graphics.Color

data class ShortsScript(
    val hook: String = "",
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val language: String = "en",
    val tone: String = "tech",
    val durationSec: Int = 35,
    val narration: String = "",
    val visualBeats: List<VisualBeat> = emptyList()
)

data class VisualBeat(
    val t: Float = 0f,
    val desc: String = "",
    val sfx: String = "whoosh"
)

enum class ToneMode(
    val id: String,
    val label: String,
    val rate: Float, // multiplier, e.g. 1.11f for +11%
    val pitch: Float, // multiplier, e.g. 1.03f for +3Hz
    val volumeGainDb: Float,
    val description: String
) {
    KIDS("kids", "Kids", 1.05f, 1.15f, 0.8f, "Warm, cheerful, wide-eyed wonder, playful rhythm"),
    TECH("tech", "Tech", 1.11f, 1.03f, 0.5f, "High-stakes, energetic, zero filler"),
    BUSINESS("business", "Business", 1.09f, 1.00f, 0.5f, "Authority + curiosity, shocking numbers"),
    STORY("story", "Story", 1.07f, 1.05f, 0.4f, "Intimate, vivid, cinematic, present tense");

    companion object {
        fun fromId(id: String): ToneMode = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TECH
    }
}

enum class SupportedLanguage(val code: String, val displayName: String, val voiceName: String) {
    EN("en", "English (US)", "Christopher (Neural)"),
    EN_UK("en-uk", "English (UK)", "Ryan (Neural)"),
    HI("hi", "Hindi", "Madhur (Neural)"),
    MR("mr", "Marathi", "Manohar (Neural)"),
    ES("es", "Spanish", "Alvaro (Neural)"),
    JA("ja", "Japanese", "Keita (Neural)"),
    DE("de", "German", "Conrad (Neural)"),
    FR("fr", "French", "Henri (Neural)"),
    PT("pt", "Portuguese", "Antonio (Neural)"),
    AR("ar", "Arabic", "Shakir (Neural)");

    companion object {
        fun fromCode(code: String): SupportedLanguage = entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

data class AudioMasteringConfig(
    val targetLufs: Double = -14.0,
    val truePeak: Double = -1.5,
    val lra: Double = 11.0,
    val highPassHz: Int = 80,
    val musicDuckDb: Double = -22.0,
    val musicRiseDb: Double = -14.0
)

enum class KenBurnsDirection {
    IN, OUT, LEFT, RIGHT
}

enum class PipelineStatus {
    IDLE,
    PICKING_TOPIC,
    GENERATING_SCRIPT,
    SYNTHESIZING_VOICE,
    MASTERING_AUDIO,
    ASSEMBLING_TIMELINE,
    COMPLETED,
    ERROR
}

data class PipelineLogEntry(
    val timestamp: String,
    val message: String,
    val isError: Boolean = false,
    val isSuccess: Boolean = false
)

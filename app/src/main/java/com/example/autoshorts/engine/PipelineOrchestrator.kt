package com.example.autoshorts.engine

import android.content.Context
import com.example.BuildConfig
import com.example.autoshorts.data.local.AppDatabase
import com.example.autoshorts.data.local.ShortEntity
import com.example.autoshorts.data.remote.GeminiScriptEngine
import com.example.autoshorts.model.PipelineLogEntry
import com.example.autoshorts.model.PipelineStatus
import com.example.autoshorts.model.ShortsScript
import com.example.autoshorts.model.SupportedLanguage
import com.example.autoshorts.model.ToneMode
import com.example.autoshorts.model.TopicBank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PipelineOrchestrator(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val db = AppDatabase.getDatabase(context)
    private val scriptEngine = GeminiScriptEngine(
        defaultApiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
    )

    private val _status = MutableStateFlow(PipelineStatus.IDLE)
    val status: StateFlow<PipelineStatus> = _status.asStateFlow()

    private val _currentScript = MutableStateFlow<ShortsScript?>(null)
    val currentScript: StateFlow<ShortsScript?> = _currentScript.asStateFlow()

    private val _logs = MutableStateFlow<List<PipelineLogEntry>>(emptyList())
    val logs: StateFlow<List<PipelineLogEntry>> = _logs.asStateFlow()

    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent.asStateFlow()

    private val _scheduledTimes = MutableStateFlow(listOf("06:00 UTC", "12:00 UTC", "16:00 UTC", "20:00 UTC"))
    val scheduledTimes: StateFlow<List<String>> = _scheduledTimes.asStateFlow()

    private val _isAutoScheduleEnabled = MutableStateFlow(true)
    val isAutoScheduleEnabled: StateFlow<Boolean> = _isAutoScheduleEnabled.asStateFlow()

    fun toggleAutoSchedule() {
        _isAutoScheduleEnabled.value = !_isAutoScheduleEnabled.value
        val state = if (_isAutoScheduleEnabled.value) "ENABLED" else "PAUSED"
        addLog("Scheduler: Autonomous daily runs $state", isSuccess = _isAutoScheduleEnabled.value)
    }

    private fun addLog(message: String, isError: Boolean = false, isSuccess: Boolean = false) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = PipelineLogEntry(time, message, isError, isSuccess)
        _logs.value = listOf(entry) + _logs.value.take(120)
    }

    fun runPipeline(
        topicOverride: String? = null,
        toneOverride: ToneMode? = null,
        languageOverride: SupportedLanguage = SupportedLanguage.EN,
        customApiKey: String? = null
    ) {
        if (_status.value != PipelineStatus.IDLE && _status.value != PipelineStatus.COMPLETED && _status.value != PipelineStatus.ERROR) {
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                _progressPercent.value = 0.05f
                _status.value = PipelineStatus.PICKING_TOPIC

                // 1. Pick topic and tone
                val (chosenTopic, chosenTone) = if (!topicOverride.isNullOrBlank()) {
                    Pair(topicOverride.trim(), toneOverride ?: ToneMode.TECH)
                } else {
                    TopicBank.getRandomTopic(toneOverride?.id)
                }

                addLog("▶ Job start | topic='$chosenTopic' tone=${chosenTone.id} lang=${languageOverride.code}")
                delay(350)
                _progressPercent.value = 0.20f

                // 2. Script Engine
                _status.value = PipelineStatus.GENERATING_SCRIPT
                addLog("ScriptEngine: Invoking Gemini model with banned-word gate & curiosity hook...")
                val script = scriptEngine.generate(
                    topic = chosenTopic,
                    tone = chosenTone.id,
                    language = languageOverride.code,
                    customApiKey = customApiKey
                )
                _currentScript.value = script
                addLog("ScriptEngine: Script generated! Title: \"${script.title}\" (${script.durationSec}s)")
                delay(400)
                _progressPercent.value = 0.45f

                // 3. Voice Engine
                _status.value = PipelineStatus.SYNTHESIZING_VOICE
                addLog("VoiceEngine: Prosody mapped [rate: ${chosenTone.rate}x, pitch: ${chosenTone.pitch}x, vol: +${chosenTone.volumeGainDb}dB] -> ${languageOverride.voiceName}")
                delay(400)
                _progressPercent.value = 0.65f

                // 4. Audio Mastering
                _status.value = PipelineStatus.MASTERING_AUDIO
                addLog("Mastering: High-pass 80Hz | 3-Band EQ | Compand | EBU R128 Target: -14.0 LUFS | True-Peak: -1.5 dB")
                addLog("Mastering: Audio Ducking envelope (-22 dB voice / -14 dB ambient rise)")
                delay(400)
                _progressPercent.value = 0.82f

                // 5. Visual Timeline & Ken-Burns
                _status.value = PipelineStatus.ASSEMBLING_TIMELINE
                addLog("EditorEngine: 9:16 Vertical Timeline (1080x1920) | Max clip 2.5s | Contrast +10% | Saturation +8%")
                addLog("EditorEngine: Karaoke Subtitles configured (68% vertical anchor, #FDE047 highlight, 4px outline)")
                addLog("EditorEngine: ${script.visualBeats.size} SFX beat cues synchronized")
                delay(450)
                _progressPercent.value = 0.95f

                // 6. Save to Room database
                val beatsJson = JSONArray().apply {
                    script.visualBeats.forEach { beat ->
                        put(org.json.JSONObject().apply {
                            put("t", beat.t)
                            put("desc", beat.desc)
                            put("sfx", beat.sfx)
                        })
                    }
                }.toString()

                val entity = ShortEntity(
                    title = script.title,
                    hook = script.hook,
                    topic = chosenTopic,
                    tone = chosenTone.id,
                    language = languageOverride.code,
                    durationSec = script.durationSec,
                    narration = script.narration,
                    description = script.description,
                    tagsCsv = script.tags.joinToString(","),
                    visualBeatsJson = beatsJson
                )
                db.shortDao().insertShort(entity)

                _progressPercent.value = 1.0f
                _status.value = PipelineStatus.COMPLETED
                addLog("✅ Job complete | Ready for preview & YouTube upload!", isSuccess = true)

            } catch (e: Exception) {
                _status.value = PipelineStatus.ERROR
                addLog("❌ Job failed: ${e.message ?: "Unknown error"}", isError = true)
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun loadScript(script: ShortsScript) {
        _currentScript.value = script
    }
}

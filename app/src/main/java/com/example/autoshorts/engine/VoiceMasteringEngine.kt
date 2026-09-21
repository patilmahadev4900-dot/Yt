package com.example.autoshorts.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.autoshorts.model.AudioMasteringConfig
import com.example.autoshorts.model.SupportedLanguage
import com.example.autoshorts.model.ToneMode
import java.util.Locale
import kotlin.math.sin

class VoiceMasteringEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val masteringConfig = AudioMasteringConfig()
    private val mainHandler = Handler(Looper.getMainLooper())

    var onWordSpoken: ((word: String, wordIndex: Int, totalWords: Int) -> Unit)? = null
    var onPlaybackFinished: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.US
            setupProgressListener()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    onPlaybackFinished?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                super.onRangeStart(utteranceId, start, end, frame)
                // Range callbacks provide word-level positioning
            }
        })
    }

    fun speakScript(
        narration: String,
        tone: ToneMode,
        language: SupportedLanguage,
        onProgress: (currentWordIndex: Int, totalWords: Int, word: String) -> Unit
    ) {
        if (!isTtsReady) return

        // Configure language
        val locale = when (language) {
            SupportedLanguage.EN -> Locale.US
            SupportedLanguage.EN_UK -> Locale.UK
            SupportedLanguage.HI -> Locale("hi", "IN")
            SupportedLanguage.MR -> Locale("mr", "IN")
            SupportedLanguage.ES -> Locale("es", "ES")
            SupportedLanguage.JA -> Locale.JAPAN
            SupportedLanguage.DE -> Locale.GERMAN
            SupportedLanguage.FR -> Locale.FRENCH
            SupportedLanguage.PT -> Locale("pt", "BR")
            SupportedLanguage.AR -> Locale("ar")
        }
        val isLangAvailable = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (isLangAvailable >= TextToSpeech.LANG_AVAILABLE) {
            tts?.language = locale
        } else {
            tts?.language = Locale.US
        }

        // Apply Tone Prosody
        tts?.setSpeechRate(tone.rate)
        tts?.setPitch(tone.pitch)

        // Split text into tokens including pauses
        val cleanText = narration.replace("[pause]", " ... ")
        val words = cleanText.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Start reading
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (1.0f + tone.volumeGainDb * 0.1f).coerceIn(0.5f, 1.0f))
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "autoshorts_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /**
     * Synthesizes audio sound effects (SFX) matching beat cues (whoosh, ding, pop, impact, bell, chime)
     */
    fun playSfx(sfxType: String) {
        Thread {
            try {
                when (sfxType.lowercase()) {
                    "whoosh" -> playSineSweep(startFreq = 800f, endFreq = 180f, durationMs = 280)
                    "ding", "bell" -> playSineTone(frequency = 1760f, durationMs = 350, decay = true)
                    "pop" -> playSineTone(frequency = 600f, durationMs = 80, decay = true)
                    "impact" -> playSineSweep(startFreq = 220f, endFreq = 45f, durationMs = 320)
                    "chime" -> playChimeChord()
                    else -> playSineSweep(startFreq = 600f, endFreq = 240f, durationMs = 200)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun playSineTone(frequency: Float, durationMs: Int, decay: Boolean = false) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val amplitude = if (decay) (1.0f - progress) * (1.0f - progress) else 1.0f
            val sample = (sin(2.0 * Math.PI * i * frequency / sampleRate) * 32767 * amplitude * 0.7f).toInt().toShort()
            generatedSnd[i] = sample
        }

        playPcmData(generatedSnd, sampleRate)
    }

    private fun playSineSweep(startFreq: Float, endFreq: Float, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            val sample = (sin(2.0 * Math.PI * i * currentFreq / sampleRate) * 32767 * (1.0f - progress * 0.5f) * 0.6f).toInt().toShort()
            generatedSnd[i] = sample
        }

        playPcmData(generatedSnd, sampleRate)
    }

    private fun playChimeChord() {
        val sampleRate = 44100
        val durationMs = 400
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val generatedSnd = ShortArray(numSamples)
        val freqs = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C Major chord

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val decay = (1.0f - progress)
            var mixed = 0.0
            for (f in freqs) {
                mixed += sin(2.0 * Math.PI * i * f / sampleRate)
            }
            val sample = (mixed / freqs.size * 32767 * decay * 0.7).toInt().toShort()
            generatedSnd[i] = sample
        }

        playPcmData(generatedSnd, sampleRate)
    }

    private fun playPcmData(data: ShortArray, sampleRate: Int) {
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufSize, data.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(data, 0, data.size)
        audioTrack.play()
        mainHandler.postDelayed({
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }, 1200)
    }
}

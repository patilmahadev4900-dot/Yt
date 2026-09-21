package com.example.autoshorts.data.remote

import com.example.autoshorts.model.ShortsScript
import com.example.autoshorts.model.VisualBeat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GeminiScriptEngine(
    private val defaultApiKey: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        val BANNED_WORDS = listOf(
            "furthermore",
            "moreover",
            "in conclusion",
            "utilize",
            "facilitate",
            "leverage",
            "commence",
            "subsequently"
        )

        const val SYSTEM_PROMPT = """You are a world-class YouTube Shorts scriptwriter who has personally written scripts for creators with 50M+ subscribers. You speak like an enthusiastic human creator talking to a close friend — NEVER robotic, NEVER academic.
ABSOLUTE RULES:
1. ZERO academic phrasing. Banned: furthermore, moreover, in conclusion, utilize, facilitate, leverage, commence, subsequently.
2. Open with a 1-line curiosity hook in the first 2 seconds.
3. Insert natural micro-pauses as literal tokens: [pause] (3–6 per script).
4. Use emotional punctuation — em-dashes, ellipses, rhetorical questions.
5. Vary sentence length aggressively. Punch. Then breathe. Then punch.
6. End with a human CTA ("Follow for more...") — not salesy.
TONAL MODES:
• kids: warm, cheerful, wide-eyed wonder, playful rhythm.
• tech: high-stakes, energetic, zero filler.
• business: authority + curiosity, shocking numbers.
• story: intimate, vivid, cinematic, present tense.
OUTPUT — STRICT JSON ONLY, no markdown, no code fences:
{
"hook": "<max 8 words>",
"title": "<YouTube title ≤70 chars>",
"description": "<2-3 SEO-aware sentences>",
"tags": ["...","..."],
"language": "en|hi|mr|es|ja|de|fr|pt|ar",
"tone": "kids|tech|business|story",
"duration_sec": 30,
"narration": "<full narration with [pause] markers>",
"visual_beats": [{"t":0.0,"desc":"...","sfx":"whoosh"}]
}"""
    }

    suspend fun generate(
        topic: String,
        tone: String = "tech",
        language: String = "en",
        customApiKey: String? = null,
        depth: Int = 0
    ): ShortsScript = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim()?.ifEmpty { null } ?: defaultApiKey.trim()

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // High-fidelity fallback generator matching Python script specifications
            return@withContext generateFallback(topic, tone, language)
        }

        try {
            val userPrompt = """
                TOPIC: $topic
                TONE: $tone
                LANGUAGE: $language

                Write a 30–50 second Shorts script following ALL rules. Return ONLY the JSON object.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "$SYSTEM_PROMPT\n\n$userPrompt") })
                        })
                    })
                }
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 1.05)
                    put("topP", 0.95)
                    put("maxOutputTokens", 2048)
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw IllegalStateException("API error ${response.code}: $errBody")
            }

            val respString = response.body?.string() ?: ""
            val jsonRoot = JSONObject(respString)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val parsed = parseScriptJson(rawText, topic, tone, language)

            // Quality gate check: Robotic words -> regenerate up to 2 times
            val lowerNarration = parsed.narration.lowercase()
            val hasBanned = BANNED_WORDS.any { lowerNarration.contains(it) }
            if (hasBanned && depth < 2) {
                return@withContext generate(topic, tone, language, customApiKey, depth + 1)
            }

            // Ensure micro-pauses exist
            val finalScript = if (!parsed.narration.contains("[pause]")) {
                parsed.copy(narration = parsed.narration.replaceFirst(". ", ". [pause] "))
            } else {
                parsed
            }

            finalScript
        } catch (e: Exception) {
            // Graceful degrade: Return fallback with log message
            generateFallback(topic, tone, language, notice = "Generated via offline engine (${e.message ?: "Connection fallback"})")
        }
    }

    private fun parseScriptJson(
        raw: String,
        topic: String,
        tone: String,
        language: String
    ): ShortsScript {
        val cleaned = raw.replace(Regex("^```(?:json)?|```$", RegexOption.MULTILINE), "").trim()
        val jsonStr = try {
            JSONObject(cleaned).toString()
        } catch (e: Exception) {
            val matcher = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(cleaned)
            if (matcher.find()) {
                matcher.group(0)
            } else {
                throw IllegalArgumentException("Non-JSON returned: ${raw.take(100)}")
            }
        }

        val obj = JSONObject(jsonStr)
        val hook = obj.optString("hook", "Wait till you hear this!")
        val title = obj.optString("title", topic).take(70)
        val description = obj.optString("description", "You won't believe how this works.")
        val tagsArray = obj.optJSONArray("tags")
        val tags = mutableListOf<String>()
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }
        val dur = obj.optInt("duration_sec", 35).coerceIn(25, 55)
        val narration = obj.optString("narration", "")

        val visualBeats = mutableListOf<VisualBeat>()
        val beatsArray = obj.optJSONArray("visual_beats")
        if (beatsArray != null) {
            for (i in 0 until beatsArray.length()) {
                val beatObj = beatsArray.getJSONObject(i)
                visualBeats.add(
                    VisualBeat(
                        t = beatObj.optDouble("t", 0.0).toFloat(),
                        desc = beatObj.optString("desc", ""),
                        sfx = beatObj.optString("sfx", "whoosh")
                    )
                )
            }
        }

        return ShortsScript(
            hook = hook,
            title = title,
            description = description,
            tags = tags.ifEmpty { listOf("Shorts", "Facts", "Trending") },
            language = obj.optString("language", language),
            tone = obj.optString("tone", tone),
            durationSec = dur,
            narration = narration,
            visualBeats = visualBeats
        )
    }

    fun generateFallback(
        topic: String,
        tone: String,
        language: String,
        notice: String? = null
    ): ShortsScript {
        return when (tone.lowercase()) {
            "kids" -> ShortsScript(
                hook = "Did you ever wonder why?",
                title = "$topic 🚀 Fun Facts for Kids".take(70),
                description = "Discover the magic behind $topic in this fun YouTube Short! Explore amazing everyday wonders.",
                tags = listOf("KidsFacts", "ScienceForKids", "Curiosity", "FunLearning"),
                language = language,
                tone = "kids",
                durationSec = 30,
                narration = "Look up at the sky! [pause] Have you ever seen something that made your jaw drop? Here is the secret: $topic isn't magic... it's pure science happening right before our eyes! [pause] Every single time you blink, tiny particles dance together. Isn't that wild? [pause] Double tap if you love learning cool stuff! Follow for more fun discoveries!",
                visualBeats = listOf(
                    VisualBeat(0.0f, "Zoom into glowing stars and playful cartoon sparks", "whoosh"),
                    VisualBeat(6.5f, "Fun microscope zoom showing colorful particles", "ding"),
                    VisualBeat(14.0f, "Excited animated character pointing up with wide eyes", "pop"),
                    VisualBeat(22.5f, "Heart celebration bounce and subscribe bell", "bell")
                )
            )
            "business" -> ShortsScript(
                hook = "This mistake costs millions daily.",
                title = "$topic — The Brutal Truth".take(70),
                description = "The shocking reality behind $topic and how smart operators capitalize on this hidden leverage.",
                tags = listOf("BusinessStrategy", "StartupTips", "Finance", "MoneyMindset"),
                language = language,
                tone = "business",
                durationSec = 32,
                narration = "Ninety percent of founders get this completely backwards. [pause] When you look at $topic, the obvious answer is always a distraction. [pause] Top performers don't work harder—they exploit asymmetrical distribution. One decision. Ten-x leverage. [pause] Stop trading your hours for pennies. Subscribe and build real leverage.",
                visualBeats = listOf(
                    VisualBeat(0.0f, "Aggressive dark chart crash with red warning badge", "impact"),
                    VisualBeat(7.0f, "Split screen showing amateur vs master revenue metric", "whoosh"),
                    VisualBeat(15.5f, "Golden compounding graph hockey-stick curve", "ding"),
                    VisualBeat(24.0f, "Executive desk close-up with bookmark reminder", "whoosh")
                )
            )
            "story" -> ShortsScript(
                hook = "Nobody believed me until tonight.",
                title = "The Secret of $topic".take(70),
                description = "An unforgettable true story revealing the untold truth about $topic.",
                tags = listOf("LifeLessons", "Storytelling", "TrueStory", "Mystery"),
                language = language,
                tone = "story",
                durationSec = 34,
                narration = "It was three in the morning when the phone rang. [pause] That was the moment everything changed regarding $topic. [pause] I stood in the doorway, staring at the envelope in my hands. The handwriting was unmistakable... yet impossible. [pause] If you ever find yourself at that crossroads, remember this: the truth always reveals itself. Follow for part two.",
                visualBeats = listOf(
                    VisualBeat(0.0f, "Cinematic moody rain on window with warm lamp", "whoosh"),
                    VisualBeat(8.0f, "Slow pan across vintage handwritten letter", "whoosh"),
                    VisualBeat(16.5f, "Dramatic shadow silhouette walking into the mist", "impact"),
                    VisualBeat(26.0f, "Pulsing mysterious hourglass counting down", "chime")
                )
            )
            else -> ShortsScript(
                hook = "Stop scrolling right now!",
                title = "$topic — What Senior Devs Hide".take(70),
                description = "The game-changing insight into $topic that transforms your entire perspective.",
                tags = listOf("TechTips", "AIRevolution", "Programming", "DeepTech"),
                language = language,
                tone = "tech",
                durationSec = 32,
                narration = "Engineers have been hiding this secret for over five years! [pause] Look at $topic closely. Everyone assumes it works one way, but underneath? [pause] It is running on a totally different architecture. Once you understand this mechanism, you can automate ninety percent of your workflow in seconds! [pause] Drop a comment with your thoughts and follow for daily tech breakthroughs.",
                visualBeats = listOf(
                    VisualBeat(0.0f, "High-contrast circuit neon pulse zooming rapidly into processor", "whoosh"),
                    VisualBeat(7.2f, "Code breakdown showing terminal execution at light speed", "ding"),
                    VisualBeat(15.0f, "3D neural network matrix spinning with glowing nodes", "pop"),
                    VisualBeat(23.5f, "YouTube subscribe button animation with glowing particles", "bell")
                )
            )
        }
    }
}

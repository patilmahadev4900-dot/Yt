package com.example

import com.example.autoshorts.data.remote.GeminiScriptEngine
import com.example.autoshorts.model.ToneMode
import com.example.autoshorts.model.TopicBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun topicBank_returnsValidTopicAndTone() {
    val (topic, tone) = TopicBank.getRandomTopic("kids")
    assertNotNull(topic)
    assertTrue(topic.isNotBlank())
    assertEquals(ToneMode.KIDS, tone)
  }

  @Test
  fun fallbackScriptEngine_conformsToShortsRules() {
    val engine = GeminiScriptEngine()
    val script = engine.generateFallback(
      topic = "Why your phone battery dies so fast",
      tone = "tech",
      language = "en"
    )

    // Verify Title length <= 70 chars per Python script rule
    assertTrue("Title must be <= 70 chars", script.title.length <= 70)
    assertTrue("Hook must not be empty", script.hook.isNotBlank())
    assertTrue("Narration must have [pause] tokens", script.narration.contains("[pause]"))

    // Verify no banned words
    val lower = script.narration.lowercase()
    for (banned in GeminiScriptEngine.BANNED_WORDS) {
      assertFalse("Narration must not contain banned word: $banned", lower.contains(banned))
    }

    // Verify visual beats are present
    assertTrue("Visual beats should be populated", script.visualBeats.isNotEmpty())
  }
}


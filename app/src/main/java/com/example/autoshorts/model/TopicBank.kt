package com.example.autoshorts.model

import kotlin.random.Random

object TopicBank {
    val bank: Map<String, List<String>> = mapOf(
        "kids" to listOf(
            "Why do stars twinkle at night?",
            "How do baby birds learn to fly?",
            "The secret life of honeybees",
            "Why is the ocean blue and salty?",
            "What happens inside a volcano before it erupts?"
        ),
        "tech" to listOf(
            "The AI breakthrough nobody is talking about",
            "Why your phone battery dies so fast",
            "3 coding tricks senior devs never share",
            "Quantum computers explained in 30 seconds",
            "The dark truth about how Wi-Fi signals travel"
        ),
        "business" to listOf(
            "How a $0 side hustle made $10k in 30 days",
            "The pricing mistake that kills 90% of startups",
            "Why boring businesses make the most money",
            "The psychological trick behind Costco's floor plan",
            "Why luxury brands burn their unsold clothes"
        ),
        "story" to listOf(
            "The night a stranger changed my life",
            "What I learned losing everything at 25",
            "The letter I never sent",
            "The secret message hidden in an old pocket watch",
            "How a missed flight saved a traveler's future"
        )
    )

    fun getRandomTopic(tone: String? = null): Pair<String, ToneMode> {
        val selectedToneStr = if (tone != null && bank.containsKey(tone)) {
            tone
        } else {
            bank.keys.toList().random()
        }
        val topics = bank[selectedToneStr] ?: bank["tech"]!!
        val topic = topics.random()
        val toneMode = ToneMode.fromId(selectedToneStr)
        return Pair(topic, toneMode)
    }
}

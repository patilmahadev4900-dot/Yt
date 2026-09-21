package com.example.autoshorts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_shorts")
data class ShortEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val hook: String,
    val topic: String,
    val tone: String,
    val language: String,
    val durationSec: Int,
    val narration: String,
    val description: String,
    val tagsCsv: String,
    val visualBeatsJson: String,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

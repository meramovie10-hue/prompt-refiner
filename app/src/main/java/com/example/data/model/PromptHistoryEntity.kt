package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_history")
data class PromptHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPrompt: String,
    val refinedPrompt: String,
    val presetId: String,
    val presetName: String,
    val hasReferenceImage: Boolean,
    val referenceImageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "FLOW IMAGE",
    val qaChecksPassed: Int = 11,
    val isFavorite: Boolean = false
)

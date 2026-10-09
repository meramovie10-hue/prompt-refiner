package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val closeUpPath: String? = null,
    val outfitPath: String? = null,
    val sheetPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

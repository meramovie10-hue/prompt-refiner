package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_props",
    foreignKeys = [ForeignKey(
        entity = CharacterEntity::class,
        parentColumns = ["id"],
        childColumns = ["characterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("characterId")]
)
data class PropEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    val name: String,
    val description: String = "",
    val imagePath: String? = null
)

package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IdentityPreset
import com.example.data.model.PromptHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {

    // --- History Operations ---

    @Query("SELECT * FROM prompt_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PromptHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: PromptHistoryEntity): Long

    @Delete
    suspend fun deleteHistory(item: PromptHistoryEntity)

    @Query("DELETE FROM prompt_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM prompt_history")
    suspend fun clearAllHistory()

    @Update
    suspend fun updateHistory(item: PromptHistoryEntity)

    // --- Presets Operations ---

    @Query("SELECT * FROM identity_presets ORDER BY name ASC")
    fun getAllCustomPresets(): Flow<List<IdentityPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: IdentityPreset)

    @Delete
    suspend fun deletePreset(preset: IdentityPreset)

    @Query("SELECT * FROM identity_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): IdentityPreset?
}

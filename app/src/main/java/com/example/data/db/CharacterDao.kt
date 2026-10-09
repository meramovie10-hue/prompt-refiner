package com.example.data.db

import androidx.room.*
import com.example.data.model.CharacterEntity
import com.example.data.model.PropEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: Long): CharacterEntity?

    @Insert suspend fun insert(c: CharacterEntity): Long
    @Update suspend fun update(c: CharacterEntity)
    @Delete suspend fun delete(c: CharacterEntity)

    @Query("SELECT * FROM character_props WHERE characterId = :cid")
    fun observeProps(cid: Long): Flow<List<PropEntity>>

    @Insert suspend fun insertProp(p: PropEntity): Long
    @Update suspend fun updateProp(p: PropEntity)
    @Delete suspend fun deleteProp(p: PropEntity)
}

package com.auravoice.tts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationHistoryDao {
    @Query("SELECT * FROM generation_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<GenerationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: GenerationHistoryEntity)

    @Query("DELETE FROM generation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM generation_history")
    suspend fun clearHistory()
}

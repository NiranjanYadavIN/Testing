package com.auravoice.tts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceModelDao {
    @Query("SELECT * FROM voice_models ORDER BY name ASC")
    fun getAllVoiceModels(): Flow<List<VoiceModelEntity>>

    @Query("SELECT * FROM voice_models ORDER BY name ASC")
    suspend fun getAllVoiceModelsList(): List<VoiceModelEntity>

    @Query("SELECT * FROM voice_models WHERE isDownloaded = 1 ORDER BY lastUsedTimestamp DESC")
    suspend fun getDownloadedVoiceModelsByLru(): List<VoiceModelEntity>

    @Query("SELECT * FROM voice_models WHERE id = :id")
    suspend fun getVoiceModelById(id: String): VoiceModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(voiceModel: VoiceModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(voiceModels: List<VoiceModelEntity>)

    @Query("UPDATE voice_models SET isDownloaded = 0, localFilePath = NULL WHERE id = :id")
    suspend fun markAsUnDownloaded(id: String)

    @Query("UPDATE voice_models SET lastUsedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastUsedTimestamp(id: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM voice_models WHERE isDownloaded = 1")
    suspend fun getDownloadedCount(): Int

    @Query("DELETE FROM voice_models WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM voice_models WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)

    @Query("DELETE FROM voice_models")
    suspend fun deleteAll()
}

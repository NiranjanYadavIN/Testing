package com.auravoice.tts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.auravoice.tts.data.model.GenerationHistory
import com.auravoice.tts.data.model.TtsEngineType

@Entity(tableName = "generation_history")
data class GenerationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val textPreview: String,
    val voiceName: String,
    val engine: TtsEngineType,
    val characterCount: Int,
    val timestamp: Long,
    val audioFilePath: String?,
    val isSaved: Boolean
) {
    fun toDomainModel(): GenerationHistory {
        return GenerationHistory(
            id = id,
            textPreview = textPreview,
            voiceName = voiceName,
            engine = engine,
            characterCount = characterCount,
            timestamp = timestamp,
            audioFilePath = audioFilePath,
            isSaved = isSaved
        )
    }

    companion object {
        fun fromDomainModel(domain: GenerationHistory): GenerationHistoryEntity {
            return GenerationHistoryEntity(
                id = domain.id,
                textPreview = domain.textPreview,
                voiceName = domain.voiceName,
                engine = domain.engine,
                characterCount = domain.characterCount,
                timestamp = domain.timestamp,
                audioFilePath = domain.audioFilePath,
                isSaved = domain.isSaved
            )
        }
    }
}

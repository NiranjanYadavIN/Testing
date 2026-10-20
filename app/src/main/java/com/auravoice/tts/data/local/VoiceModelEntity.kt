package com.auravoice.tts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.auravoice.tts.data.model.TtsEngineType
import com.auravoice.tts.data.model.VoiceModel

@Entity(tableName = "voice_models")
data class VoiceModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val engine: TtsEngineType,
    val language: String,
    val gender: String,
    val accent: String,
    val country: String = "",
    val modelDownloadUrl: String,
    val voiceSampleUrl: String,
    val sizeBytes: Long,
    val isPremium: Boolean,
    val isDownloaded: Boolean,
    val lastUsedTimestamp: Long,
    val localFilePath: String?
) {
    fun toDomainModel(downloadProgress: Float = 0f, isDownloading: Boolean = false): VoiceModel {
        return VoiceModel(
            id = id,
            name = name,
            engine = engine,
            language = language,
            gender = gender,
            accent = accent,
            country = country,
            modelDownloadUrl = modelDownloadUrl,
            voiceSampleUrl = voiceSampleUrl,
            sizeBytes = sizeBytes,
            isPremium = isPremium,
            isDownloaded = isDownloaded,
            downloadProgress = downloadProgress,
            isDownloading = isDownloading,
            lastUsedTimestamp = lastUsedTimestamp,
            localFilePath = localFilePath
        )
    }

    companion object {
        fun fromDomainModel(model: VoiceModel): VoiceModelEntity {
            return VoiceModelEntity(
                id = model.id,
                name = model.name,
                engine = model.engine,
                language = model.language,
                gender = model.gender,
                accent = model.accent,
                country = model.country,
                modelDownloadUrl = model.modelDownloadUrl,
                voiceSampleUrl = model.voiceSampleUrl,
                sizeBytes = model.sizeBytes,
                isPremium = model.isPremium,
                isDownloaded = model.isDownloaded,
                lastUsedTimestamp = model.lastUsedTimestamp,
                localFilePath = model.localFilePath
            )
        }
    }
}

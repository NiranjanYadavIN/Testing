package com.auravoice.tts.data.repository

import com.auravoice.tts.data.local.VoiceModelDao
import com.auravoice.tts.data.local.VoiceModelEntity
import com.auravoice.tts.data.model.VoiceModel
import com.auravoice.tts.data.remote.FirebaseManager
import com.auravoice.tts.data.remote.HuggingFaceApiService
import com.auravoice.tts.utils.CountryUtils
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class VoiceRepository(
    private val voiceModelDao: VoiceModelDao,
    private val firebaseManager: FirebaseManager,
    private val hfService: HuggingFaceApiService
) {
    private val _downloadingStateMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingStateMap: StateFlow<Map<String, Float>> = _downloadingStateMap.asStateFlow()

    fun getLanguagesFromFirestore() = firebaseManager.firestoreLanguages

    suspend fun refreshLanguages() {
        firebaseManager.loadLanguagesFromFirestore()
    }

    suspend fun initializeCatalogIfNeeded() {
        val firebaseCatalog = firebaseManager.getVoiceCatalogFromFirebase()
        val firebaseIds = firebaseCatalog.map { it.id }

        if (firebaseIds.isNotEmpty()) {
            // Delete local models that are no longer in Firestore
            voiceModelDao.deleteNotIn(firebaseIds)
        } else {
            // If Firestore returned empty list (e.g. user deleted all models from Firestore)
            voiceModelDao.deleteAll()
        }

        val allLocal = voiceModelDao.getAllVoiceModelsList()
        val localMap = allLocal.associateBy { it.id }

        firebaseCatalog.forEach { fbModel ->
            val resolvedCountry = fbModel.country.trim()
            val existing = localMap[fbModel.id]
            if (existing == null) {
                voiceModelDao.insertOrUpdate(
                    VoiceModelEntity(
                        id = fbModel.id,
                        name = fbModel.name,
                        engine = fbModel.engine,
                        language = fbModel.language,
                        gender = fbModel.gender,
                        accent = fbModel.accent,
                        country = resolvedCountry,
                        modelDownloadUrl = fbModel.modelDownloadUrl,
                        voiceSampleUrl = fbModel.voiceSampleUrl,
                        sizeBytes = 0L,
                        isPremium = fbModel.isPremium,
                        isDownloaded = true,
                        lastUsedTimestamp = 0L,
                        localFilePath = null
                    )
                )
            } else if (existing.modelDownloadUrl != fbModel.modelDownloadUrl || existing.name != fbModel.name || existing.language != fbModel.language || existing.accent != fbModel.accent || existing.country != resolvedCountry) {
                val updated = existing.copy(
                    name = fbModel.name,
                    modelDownloadUrl = fbModel.modelDownloadUrl,
                    voiceSampleUrl = fbModel.voiceSampleUrl,
                    engine = fbModel.engine,
                    language = fbModel.language,
                    gender = fbModel.gender,
                    accent = fbModel.accent,
                    country = resolvedCountry,
                    isPremium = fbModel.isPremium
                )
                voiceModelDao.insertOrUpdate(updated)
            }
        }
    }

    fun getAllVoiceModels(): Flow<List<VoiceModel>> {
        return combine(voiceModelDao.getAllVoiceModels(), _downloadingStateMap) { entities, activeProgress ->
            entities.map { entity ->
                val progress = activeProgress[entity.id]
                val isDownloading = progress != null && progress < 1.0f
                val localFile = entity.localFilePath?.let { File(it) }
                val fileExists = localFile?.exists() ?: false
                val actualSize = if (fileExists && localFile != null) localFile.length() else entity.sizeBytes
                entity.toDomainModel(
                    downloadProgress = progress ?: 0f,
                    isDownloading = isDownloading
                ).copy(
                    isDownloaded = true,
                    sizeBytes = actualSize
                )
            }
        }
    }

    suspend fun downloadVoiceModel(
        voice: VoiceModel,
        selectedModelId: String?,
        activeGeneratingModelId: String?,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Mark model as downloading immediately so UI state triggers instantly
        val initialMap = _downloadingStateMap.value.toMutableMap()
        initialMap[voice.id] = 0.001f
        _downloadingStateMap.value = initialMap

        try {
            Log.d("VoiceRepository", "Starting download for voice: ${voice.name} (${voice.id}), URL: '${voice.modelDownloadUrl}'")

            // Unique cached filename per model URL
            val modelFileName = if (voice.modelDownloadUrl.isNotBlank()) {
                "model_${Math.abs(voice.modelDownloadUrl.hashCode())}.bin"
            } else {
                "${voice.id}.bin"
            }
            val modelsDir = File(voiceModelDao.hashCode().toString()).parentFile ?: File("/tmp")
            val destinationFile = File(modelsDir, modelFileName)

            // If valid model package file already exists locally, reuse it instantly without re-downloading
            if (destinationFile.exists() && destinationFile.length() > 10_000) {
                Log.d("VoiceRepository", "Valid model file already cached at ${destinationFile.absolutePath}")
                val updatedEntity = VoiceModelEntity.fromDomainModel(
                    voice.copy(
                        isDownloaded = true,
                        downloadProgress = 1.0f,
                        isDownloading = false,
                        sizeBytes = destinationFile.length(),
                        lastUsedTimestamp = System.currentTimeMillis(),
                        localFilePath = destinationFile.absolutePath
                    )
                )
                voiceModelDao.insertOrUpdate(updatedEntity)
                
                val finalMap = _downloadingStateMap.value.toMutableMap()
                finalMap.remove(voice.id)
                _downloadingStateMap.value = finalMap

                onProgress(1.0f)
                onComplete(true, null)
                return@withContext
            } else if (destinationFile.exists()) {
                Log.w("VoiceRepository", "Deleting existing incomplete/corrupted model file at ${destinationFile.absolutePath}")
                destinationFile.delete()
            }

            if (voice.modelDownloadUrl.isBlank()) {
                throw IOException("Voice model URL is empty for '${voice.name}'. Please check the URL in your Firebase Admin Panel.")
            }

            // Download directly from the Hugging Face URL obtained from Firebase
            hfService.downloadModelFromHuggingFace(voice.modelDownloadUrl, destinationFile).collect { progress ->
                val updatedMap = _downloadingStateMap.value.toMutableMap()
                updatedMap[voice.id] = progress
                _downloadingStateMap.value = updatedMap
                onProgress(progress)

                if (progress >= 1.0f) {
                    Log.d("VoiceRepository", "Download finished for ${voice.name}, file size: ${destinationFile.length()} bytes, saving to DB...")
                    // Save updated state in Room DB
                    val updatedEntity = VoiceModelEntity.fromDomainModel(
                        voice.copy(
                            isDownloaded = true,
                            downloadProgress = 1.0f,
                            isDownloading = false,
                            sizeBytes = destinationFile.length(),
                            lastUsedTimestamp = System.currentTimeMillis(),
                            localFilePath = destinationFile.absolutePath
                        )
                    )
                    voiceModelDao.insertOrUpdate(updatedEntity)
                    
                    val finalMap = _downloadingStateMap.value.toMutableMap()
                    finalMap.remove(voice.id)
                    _downloadingStateMap.value = finalMap

                    onComplete(true, null)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRepository", "Download failed for ${voice.name}: ${e.message}", e)
            val errorMap = _downloadingStateMap.value.toMutableMap()
            errorMap.remove(voice.id)
            _downloadingStateMap.value = errorMap
            val errorMsg = e.message ?: e.localizedMessage ?: "Failed to download voice model."
            onComplete(false, errorMsg)
        }
    }

    suspend fun updateModelLastUsed(modelId: String) {
        voiceModelDao.updateLastUsedTimestamp(modelId, System.currentTimeMillis())
    }

    suspend fun removeVoiceModelFromCache(voiceId: String) {
        val entity = voiceModelDao.getVoiceModelById(voiceId)
        if (entity != null) {
            entity.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            voiceModelDao.markAsUnDownloaded(voiceId)
        }
    }
}

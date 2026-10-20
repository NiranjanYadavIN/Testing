package com.auravoice.tts.data.model

data class VoiceModel(
    val id: String,
    val name: String,
    val engine: TtsEngineType,
    val language: String,
    val gender: String,
    val accent: String,
    val country: String = "",
    val modelDownloadUrl: String, // Obtained from Firebase
    val voiceSampleUrl: String,
    val sizeBytes: Long,
    val isPremium: Boolean,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val lastUsedTimestamp: Long = 0L,
    val localFilePath: String? = null
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024L * 1024L)
            return if (mb > 0) {
                "$mb MB"
            } else if (sizeBytes > 0) {
                val kb = sizeBytes / 1024L
                "$kb KB"
            } else {
                "0 MB"
            }
        }
}

package com.auravoice.tts.data.model

data class GenerationHistory(
    val id: Long = 0L,
    val textPreview: String,
    val voiceName: String,
    val engine: TtsEngineType,
    val characterCount: Int,
    val timestamp: Long,
    val audioFilePath: String? = null,
    val isSaved: Boolean = false
)

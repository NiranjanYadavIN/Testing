package com.auravoice.tts.engine

import android.content.Context
import com.auravoice.tts.data.model.VoiceModel
import kotlinx.coroutines.flow.Flow

sealed class TtsGenerationState {
    data object Idle : TtsGenerationState()
    data class Processing(val progress: Float, val currentWord: String = "") : TtsGenerationState()
    data class Success(val audioFilePath: String, val generationTimeMs: Long, val sampleRate: Int) : TtsGenerationState()
    data class Error(val message: String) : TtsGenerationState()
}

interface TtsEngine {
    fun synthesizeSpeechOnDevice(
        context: Context,
        text: String,
        voice: VoiceModel,
        outputDir: String
    ): Flow<TtsGenerationState>
}

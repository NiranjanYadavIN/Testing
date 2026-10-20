package com.auravoice.tts.engine

import android.content.Context
import android.util.Log
import com.auravoice.tts.data.model.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class CloudTtsEngine : TtsEngine {

    companion object {
        private const val TAG = "CloudTtsEngine"
        private const val RENDER_BASE_URL = "https://av-backend-zevs.onrender.com/tts"
        const val SAMPLE_RATE = 24000
    }

    override fun synthesizeSpeechOnDevice(
        context: Context,
        text: String,
        voice: VoiceModel,
        outputDir: String
    ): Flow<TtsGenerationState> = flow {
        val startTime = System.currentTimeMillis()
        emit(TtsGenerationState.Processing(0.15f, "Connecting to AuraVoice Cloud Server..."))

        val auraDir = File(outputDir, "AuraVoice")
        if (!auraDir.exists()) auraDir.mkdirs()

        val sanitizedText = text.trim().take(18).replace("[^a-zA-Z0-9]".toRegex(), "_").replace("_+".toRegex(), "_").trim('_')
        val sanitizedVoice = voice.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "AuraVoice_${if (sanitizedText.isNotBlank()) "${sanitizedText}_" else ""}${sanitizedVoice}_${System.currentTimeMillis()}.mp3"
        val outputFile = File(auraDir, fileName)

        val voiceCode = getEdgeTtsVoiceCode(voice)
        Log.d(TAG, "Requesting Aura Cloud TTS for voice '${voice.name}' ($voiceCode), text length: ${text.length}")

        emit(TtsGenerationState.Processing(0.40f, "Generating Neural Speech (${voice.language})..."))

        var cloudSuccess = false
        try {
            cloudSuccess = withContext(Dispatchers.IO) {
                downloadCloudAudio(text, voiceCode, outputFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud TTS request failed: ${e.message}", e)
        }

        if (cloudSuccess && outputFile.exists() && outputFile.length() > 500) {
            val totalTime = System.currentTimeMillis() - startTime
            emit(TtsGenerationState.Processing(1.0f, "Speech Generation Complete"))
            emit(TtsGenerationState.Success(outputFile.absolutePath, totalTime, SAMPLE_RATE))
        } else {
            Log.e(TAG, "Cloud TTS synthesis failed")
            emit(TtsGenerationState.Error("Voice synthesis failed. Please check your internet connection and try again."))
        }
    }

    private fun getEdgeTtsVoiceCode(voice: VoiceModel): String {
        val id = voice.id
        if (id.contains("Neural") || id.contains("-")) {
            return id
        }

        val lang = voice.language.lowercase()
        val gender = voice.gender.lowercase()

        return when {
            lang.contains("hindi") || lang.contains("hi") || lang.contains("india") -> {
                if (gender == "male") "hi-IN-MadhurNeural" else "hi-IN-SwaraNeural"
            }
            lang.contains("uk") || lang.contains("gb") || lang.contains("british") -> {
                if (gender == "male") "en-GB-RyanNeural" else "en-GB-SoniaNeural"
            }
            lang.contains("french") || lang.contains("fr") -> {
                if (gender == "male") "fr-FR-HenriNeural" else "fr-FR-DeniseNeural"
            }
            lang.contains("spanish") || lang.contains("es") -> {
                if (gender == "male") "es-ES-AlvaroNeural" else "es-ES-ElviraNeural"
            }
            lang.contains("german") || lang.contains("de") -> {
                if (gender == "male") "de-DE-ConradNeural" else "de-DE-KatjaNeural"
            }
            else -> {
                if (gender == "male") "en-US-AndrewNeural" else "en-US-AvaNeural"
            }
        }
    }

    private fun downloadCloudAudio(text: String, voiceCode: String, outputFile: File): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val urlString = "$RENDER_BASE_URL?text=$encodedText&voice=$voiceCode"
            val url = URL(urlString)

            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 35000 // 35s timeout for Render cold start
            connection.readTimeout = 45000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "AuraVoiceAndroidApp/1.0")

            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream: InputStream = connection.inputStream
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                outputFile.exists() && outputFile.length() > 500
            } else {
                Log.e(TAG, "Render Cloud TTS returned response code: ${connection.responseCode}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading from Render Cloud: ${e.message}")
            false
        } finally {
            connection?.disconnect()
        }
    }
}

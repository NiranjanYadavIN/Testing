package com.auravoice.tts.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioExportUtils {

    private const val TAG = "AudioExportUtils"

    /**
     * Helper to generate a clean, recognizable filename for generated TTS audio files.
     * Example output: "AuraVoice_Hello_World_Female_20260803_153000.wav"
     */
    fun createCleanAudioFileName(text: String, voiceName: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timeStamp = dateFormat.format(Date())

        val sanitizedText = text.trim()
            .take(18)
            .replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')

        val sanitizedVoice = voiceName.trim()
            .replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')

        val textPart = if (sanitizedText.isNotBlank()) "${sanitizedText}_" else ""
        val voicePart = if (sanitizedVoice.isNotBlank()) "${sanitizedVoice}_" else ""

        return "AuraVoice_${textPart}${voicePart}${timeStamp}.wav"
    }

    /**
     * Gets or creates the dedicated 'AuraVoice' directory in external music or app storage.
     */
    fun getAuraVoiceDirectory(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir
        val auraVoiceDir = File(baseDir, "AuraVoice")
        if (!auraVoiceDir.exists()) {
            auraVoiceDir.mkdirs()
        }
        return auraVoiceDir
    }

    /**
     * Saves/Exports audio file into public Music/AuraVoice directory via MediaStore or direct file copy
     * so user can easily see and access it in File Manager & Music player apps.
     */
    fun saveAudioToPublicAuraVoiceFolder(context: Context, sourceFilePath: String, text: String, voiceName: String): Uri? {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
            return null
        }

        val fileName = createCleanAudioFileName(text, voiceName)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/AuraVoice")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    Toast.makeText(context, "Saved to Music/AuraVoice/$fileName", Toast.LENGTH_LONG).show()
                    return uri
                }
            } else {
                val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val auraVoiceFolder = File(publicMusicDir, "AuraVoice")
                if (!auraVoiceFolder.exists()) {
                    auraVoiceFolder.mkdirs()
                }

                val destFile = File(auraVoiceFolder, fileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Toast.makeText(context, "Saved to Music/AuraVoice/$fileName", Toast.LENGTH_LONG).show()
                return Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save audio to AuraVoice folder", e)
            Toast.makeText(context, "Failed to save audio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    /**
     * Shares audio file via Android Intent.
     */
    fun shareAudioFile(context: Context, filePath: String, text: String, voiceName: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AuraVoice Speech Clip")
                putExtra(Intent.EXTRA_TEXT, "Generated with AuraVoice: \"${text.take(50)}\" ($voiceName)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Speech via"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing audio file", e)
            Toast.makeText(context, "Could not share audio: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

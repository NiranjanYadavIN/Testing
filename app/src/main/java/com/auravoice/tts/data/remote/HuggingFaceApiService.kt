package com.auravoice.tts.data.remote

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HuggingFaceApiService {

    companion object {
        private const val TAG = "HuggingFaceApiService"
    }

    /**
     * Downloads model weights directly from the URL (e.g. HuggingFace) to local private storage.
     * Follows redirects properly and verifies downloaded file validity.
     */
    fun downloadModelFromHuggingFace(
        modelDownloadUrl: String,
        destinationFile: File
    ): Flow<Float> = flow {
        emit(0.05f)

        val rawUrl = modelDownloadUrl.trim()
        if (rawUrl.isBlank()) {
            throw IOException("Voice model download URL is empty. Please configure a valid model URL in the Admin Panel.")
        }

        val candidates = buildCandidateUrls(rawUrl)
        var lastException: Exception? = null
        var downloadSuccess = false

        for ((index, candidateUrl) in candidates.withIndex()) {
            Log.d(TAG, "Attempting model download candidate [${index + 1}/${candidates.size}]: $candidateUrl")
            try {
                if (attemptDownloadFromUrl(candidateUrl, destinationFile) { progress -> emit(progress) }) {
                    downloadSuccess = true
                    Log.d(TAG, "Successfully downloaded model from candidate: $candidateUrl")
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Download candidate failed ($candidateUrl): ${e.message}")
                lastException = e
                if (destinationFile.exists()) destinationFile.delete()
            }
        }

        if (!downloadSuccess) {
            val errorMsg = lastException?.message ?: "Failed to download model from all attempted URLs."
            Log.e(TAG, "All candidate download attempts failed for $modelDownloadUrl")
            throw IOException(errorMsg)
        }

        emit(1.0f)
    }

    private suspend fun attemptDownloadFromUrl(
        downloadUrl: String,
        destinationFile: File,
        onProgress: suspend (Float) -> Unit
    ): Boolean {
        var currentUrl = downloadUrl
        var connection: HttpURLConnection? = null
        var redirects = 0
        val maxRedirects = 10
        var cookies: String? = null

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 90000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/119.0")
            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("Accept-Encoding", "identity")
            if (!cookies.isNullOrBlank()) {
                conn.setRequestProperty("Cookie", cookies)
            }
            conn.connect()

            val status = conn.responseCode

            val setCookie = conn.getHeaderField("Set-Cookie")
            if (!setCookie.isNullOrBlank()) {
                cookies = setCookie.split(";")[0]
            }

            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308
            ) {
                var newUrl = conn.getHeaderField("Location")
                if (!newUrl.isNullOrBlank()) {
                    if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                        newUrl = URL(url, newUrl).toString()
                    }
                    currentUrl = newUrl
                    redirects++
                    conn.disconnect()
                    continue
                }
            }
            connection = conn
            break
        }

        val conn = connection ?: throw IOException("Failed to establish connection to: $downloadUrl")

        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val contentLength = conn.contentLengthLong
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(16384)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength).coerceIn(0.1f, 0.99f)
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            conn.disconnect()

            if (destinationFile.length() < 10_000) {
                val preview = try {
                    val headerBytes = ByteArray(minOf(256, destinationFile.length().toInt()))
                    destinationFile.inputStream().use { it.read(headerBytes) }
                    String(headerBytes, Charsets.UTF_8)
                } catch (e: Exception) { "" }

                if (destinationFile.exists()) destinationFile.delete()

                if (preview.contains("git-lfs")) {
                    throw IOException("URL points to Git LFS pointer text file instead of binary voice model.")
                } else if (preview.contains("<!DOCTYPE") || preview.contains("<html")) {
                    throw IOException("URL returned an HTML web page instead of binary model file.")
                } else {
                    throw IOException("Downloaded file is invalid or too small (${destinationFile.length()} bytes)")
                }
            }

            Log.d(TAG, "Downloaded valid binary file (${destinationFile.length()} bytes) from $downloadUrl")
            return true
        } else {
            val code = conn.responseCode
            val msg = conn.responseMessage
            conn.disconnect()
            if (destinationFile.exists()) destinationFile.delete()
            throw IOException("HTTP $code ($msg) from $downloadUrl")
        }
    }

    private fun buildCandidateUrls(inputUrl: String): List<String> {
        val list = mutableListOf<String>()
        val direct = transformToDirectDownloadUrl(inputUrl)
        list.add(direct)
        return list
    }

    /**
     * Converts common hosting URLs (HuggingFace, GitHub, Google Drive) into direct binary download links.
     */
    private fun transformToDirectDownloadUrl(url: String): String {
        var result = url

        // 1. HuggingFace transformation
        if (result.contains("huggingface.co")) {
            if (result.contains("/blob/")) {
                result = result.replace("/blob/", "/resolve/")
            } else if (result.contains("/raw/")) {
                result = result.replace("/raw/", "/resolve/")
            }

            if (!result.contains("download=true")) {
                result = if (result.contains("?")) "$result&download=true" else "$result?download=true"
            }
        }

        // 2. GitHub transformation (Handle normal GitHub & raw links -> LFS media links)
        if (result.contains("github.com") || result.contains("raw.githubusercontent.com")) {
            // E.g. https://github.com/user/repo/blob/main/path/file.bin
            // E.g. https://raw.githubusercontent.com/user/repo/main/path/file.bin
            val githubRegex = Regex("""https?://(?:raw\.githubusercontent\.com|github\.com)/([^/]+)/([^/]+)/(?:blob/|raw/)?(.+)""")
            val match = githubRegex.find(result)
            if (match != null) {
                val (owner, repo, path) = match.destructured
                result = "https://media.githubusercontent.com/media/$owner/$repo/$path"
            }
        }

        // 3. Google Drive transformation
        if (result.contains("drive.google.com")) {
            val driveRegex = Regex("""(?:file/d/|id=)([\w-]+)""")
            val match = driveRegex.find(result)
            if (match != null) {
                val fileId = match.groupValues[1]
                result = "https://drive.usercontent.google.com/download?id=$fileId&confirm=t"
            }
        }

        return result
    }
}


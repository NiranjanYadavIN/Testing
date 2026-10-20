package com.auravoice.tts.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentAudioPathOrUrl: String? = null,
    val error: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    companion object {
        private const val TAG = "AudioPlayerManager"
    }

    fun playAudio(pathOrUrl: String, isSamplePreview: Boolean = false) {
        try {
            stopAudio()

            // Set state to buffering immediately
            _playbackState.value = AudioPlaybackState(
                isPlaying = false,
                isBuffering = true,
                currentAudioPathOrUrl = pathOrUrl
            )

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                    setDataSource(pathOrUrl)
                } else {
                    val file = File(pathOrUrl)
                    if (file.exists()) {
                        setDataSource(context, Uri.fromFile(file))
                    } else {
                        // Fallback: setDataSource via string path
                        setDataSource(pathOrUrl)
                    }
                }

                setOnPreparedListener { mp ->
                    mp.start()
                    _playbackState.value = AudioPlaybackState(
                        isPlaying = true,
                        isBuffering = false,
                        currentPositionMs = 0,
                        durationMs = mp.duration,
                        currentAudioPathOrUrl = pathOrUrl
                    )
                }

                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = 0
                    )
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playbackState.value = AudioPlaybackState(
                        isPlaying = false,
                        isBuffering = false,
                        error = "Unable to play audio sample ($what)"
                    )
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating audio playback for $pathOrUrl", e)
            _playbackState.value = AudioPlaybackState(
                isPlaying = false,
                isBuffering = false,
                error = "Playback error: ${e.localizedMessage}"
            )
        }
    }

    fun pauseAudio() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = mp.currentPosition
                )
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media player", e)
            }
        }
        mediaPlayer = null
        _playbackState.value = AudioPlaybackState()
    }
}

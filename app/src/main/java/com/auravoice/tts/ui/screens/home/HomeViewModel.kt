package com.auravoice.tts.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auravoice.tts.data.model.GenerationHistory
import com.auravoice.tts.data.model.TtsEngineType
import com.auravoice.tts.data.model.UserProfile
import com.auravoice.tts.data.model.VoiceModel
import com.auravoice.tts.data.repository.UserRepository
import com.auravoice.tts.data.repository.VoiceRepository
import com.auravoice.tts.engine.AudioPlaybackState
import com.auravoice.tts.engine.AudioPlayerManager
import com.auravoice.tts.engine.CloudTtsEngine
import com.auravoice.tts.engine.TtsEngine
import com.auravoice.tts.engine.TtsGenerationState
import com.auravoice.tts.util.AudioExportUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine

data class HomeUiState(
    val inputText: String = "",
    val maxCharLimitPerRequest: Int = 5000,
    val selectedEngine: TtsEngineType = TtsEngineType.AURA_NEURAL,
    val selectedVoice: VoiceModel? = null,
    val selectedLanguage: String = "English",
    val generationState: TtsGenerationState = TtsGenerationState.Idle,
    val activeGeneratedAudioPath: String? = null,
    val premiumPromotionVoice: VoiceModel? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val context: Context,
    private val voiceRepository: VoiceRepository,
    private val userRepository: UserRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val availableVoices: StateFlow<List<VoiceModel>> = voiceRepository.getAllVoiceModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableLanguages: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(
        voiceRepository.getLanguagesFromFirestore(),
        availableVoices
    ) { firestoreLangs, voices ->
        val fromFirestore = firestoreLangs.map { it.name }
        val fromVoices = voices.map { it.language }
        (fromFirestore + fromVoices).distinct().filter { it.isNotBlank() }.sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val playbackState: StateFlow<AudioPlaybackState> = audioPlayerManager.playbackState

    private val cloudTtsEngine = CloudTtsEngine()

    init {
        viewModelScope.launch {
            voiceRepository.initializeCatalogIfNeeded()
            availableVoices.collect { voices ->
                val currentSelected = _uiState.value.selectedVoice
                if (currentSelected != null) {
                    val updated = voices.find { it.id == currentSelected.id }
                    if (updated != null) {
                        _uiState.value = _uiState.value.copy(
                            selectedVoice = updated,
                            selectedLanguage = updated.language
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(userRepository.appSettings, userProfile) { settings, profile ->
                if (profile.isPremium) {
                    settings.premiumUserCharLimitPerRequest
                } else {
                    settings.freeUserCharLimitPerRequest
                }
            }.collect { limit ->
                _uiState.value = _uiState.value.copy(maxCharLimitPerRequest = limit)
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            voiceRepository.initializeCatalogIfNeeded()
            userRepository.refreshUserData()
            kotlinx.coroutines.delay(600)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, errorMessage = null)
    }

    fun onSelectLanguage(language: String) {
        val currentVoice = _uiState.value.selectedVoice
        val matchesCurrent = currentVoice != null && (
            currentVoice.language.equals(language, ignoreCase = true) ||
            currentVoice.language.contains(language, ignoreCase = true) ||
            language.contains(currentVoice.language, ignoreCase = true)
        )

        _uiState.value = _uiState.value.copy(
            selectedLanguage = language,
            selectedVoice = if (matchesCurrent) currentVoice else null
        )
    }

    fun previewVoiceSample(voice: VoiceModel) {
        val currentAudio = playbackState.value.currentAudioPathOrUrl
        if (currentAudio == voice.voiceSampleUrl && playbackState.value.isPlaying) {
            audioPlayerManager.stopAudio()
        } else {
            audioPlayerManager.playAudio(voice.voiceSampleUrl, isSamplePreview = true)
        }
    }

    fun onSelectEngine(engine: TtsEngineType) {
        val currentVoice = _uiState.value.selectedVoice
        val suitableVoice = availableVoices.value.firstOrNull { it.engine == engine && it.isDownloaded }
            ?: availableVoices.value.firstOrNull { it.engine == engine }
            ?: currentVoice

        _uiState.value = _uiState.value.copy(
            selectedEngine = engine,
            selectedVoice = suitableVoice
        )
    }

    fun onSelectVoice(voice: VoiceModel) {
        val profile = userProfile.value
        if (voice.isPremium && !profile.isPremium) {
            _uiState.value = _uiState.value.copy(premiumPromotionVoice = voice)
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedVoice = voice,
            selectedEngine = voice.engine,
            selectedLanguage = voice.language
        )
        viewModelScope.launch {
            voiceRepository.updateModelLastUsed(voice.id)
        }
    }

    fun requestDownloadConfirmation(voice: VoiceModel) {
        downloadAndGenerate(voice)
    }

    fun dismissPremiumPromotion() {
        _uiState.value = _uiState.value.copy(premiumPromotionVoice = null)
    }

    private fun downloadAndGenerate(voice: VoiceModel) {
        val profile = userProfile.value
        if (voice.isPremium && !profile.isPremium) {
            _uiState.value = _uiState.value.copy(premiumPromotionVoice = voice)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                generationState = TtsGenerationState.Processing(0f, "Downloading voice package...")
            )
            voiceRepository.downloadVoiceModel(
                voice = voice,
                selectedModelId = voice.id,
                activeGeneratingModelId = null,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(
                        generationState = TtsGenerationState.Processing(progress, "Downloading voice package...")
                    )
                },
                onComplete = { success, errorMsg ->
                    if (success) {
                        generateSpeechOnDevice()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            generationState = TtsGenerationState.Idle,
                            errorMessage = errorMsg ?: "Failed to download voice package. Please check network and try again."
                        )
                    }
                }
            )
        }
    }

    fun generateSpeechOnDevice() {
        val currentState = _uiState.value
        val voice = currentState.selectedVoice
        if (voice == null) {
            _uiState.value = currentState.copy(errorMessage = "Please select a voice first.")
            return
        }

        val profile = userProfile.value

        if (voice.isPremium && !profile.isPremium) {
            _uiState.value = currentState.copy(premiumPromotionVoice = voice)
            return
        }

        val text = currentState.inputText.trim()

        if (text.isEmpty()) {
            _uiState.value = currentState.copy(errorMessage = "Please enter text to synthesize.")
            return
        }

        if (text.length > currentState.maxCharLimitPerRequest) {
            _uiState.value = currentState.copy(
                errorMessage = "Character limit per request exceeded (${text.length}/${currentState.maxCharLimitPerRequest}). Please reduce text length."
            )
            return
        }

        // Check daily limit for Free Users
        if (!profile.isPremium && (profile.dailyUsageChars + text.length > profile.dailyLimitChars || profile.creditsRemaining < text.length)) {
            _uiState.value = currentState.copy(
                errorMessage = "Daily character limit reached! Your credits will automatically reset tomorrow at 00:00 UTC."
            )
            return
        }

        viewModelScope.launch {
            voiceRepository.updateModelLastUsed(voice.id)

            val engine: TtsEngine = cloudTtsEngine
            val outputDir = AudioExportUtils.getAuraVoiceDirectory(context).absolutePath

            engine.synthesizeSpeechOnDevice(context, text, voice, outputDir).collect { genState ->
                _uiState.value = _uiState.value.copy(generationState = genState)

                if (genState is TtsGenerationState.Success) {
                    _uiState.value = _uiState.value.copy(
                        activeGeneratedAudioPath = genState.audioFilePath
                    )
                    // Record in Room DB & update Firebase character stats & Firestore history
                    userRepository.recordSpeechGeneration(
                        history = GenerationHistory(
                            textPreview = if (text.length > 60) text.take(60) + "..." else text,
                            voiceName = voice.name,
                            engine = voice.engine,
                            characterCount = text.length,
                            timestamp = System.currentTimeMillis(),
                            audioFilePath = genState.audioFilePath
                        ),
                        voiceId = voice.id,
                        language = voice.language,
                        rawText = text
                    )
                    // Auto Play generated speech
                    audioPlayerManager.playAudio(genState.audioFilePath)
                }
            }
        }
    }

    fun playPauseGeneratedAudio() {
        val currentAudio = _uiState.value.activeGeneratedAudioPath ?: return
        val state = playbackState.value
        if (state.isPlaying) {
            audioPlayerManager.pauseAudio()
        } else if (state.currentAudioPathOrUrl == currentAudio) {
            audioPlayerManager.resumeAudio()
        } else {
            audioPlayerManager.playAudio(currentAudio)
        }
    }

    class Factory(
        private val context: Context,
        private val voiceRepo: VoiceRepository,
        private val userRepo: UserRepository,
        private val audioPlayer: AudioPlayerManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(context, voiceRepo, userRepo, audioPlayer) as T
        }
    }
}

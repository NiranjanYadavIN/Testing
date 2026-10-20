package com.auravoice.tts.ui.screens.voices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auravoice.tts.data.model.UserProfile
import com.auravoice.tts.data.model.VoiceModel
import com.auravoice.tts.data.repository.UserRepository
import com.auravoice.tts.data.repository.VoiceRepository
import com.auravoice.tts.utils.CountryUtils
import com.auravoice.tts.engine.AudioPlaybackState
import com.auravoice.tts.engine.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.auravoice.tts.data.model.TtsEngineType

data class VoiceCatalogUiState(
    val selectedCountryFilter: String = "All",
    val selectedLanguageFilter: String = "All",
    val selectedGenderFilter: String = "ALL", // "ALL", "MALE", "FEMALE"
    val selectedTierFilter: String = "ALL", // "ALL", "FREE", "PREMIUM"
    val searchQuery: String = "",
    val activePreviewVoiceId: String? = null,
    val selectedVoiceId: String? = null,
    val isDownloadingAny: Boolean = false,
    val premiumPromotionVoice: VoiceModel? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class VoiceCatalogViewModel(
    private val voiceRepository: VoiceRepository,
    private val userRepository: UserRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceCatalogUiState())
    val uiState: StateFlow<VoiceCatalogUiState> = _uiState.asStateFlow()

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    private val _allVoices: StateFlow<List<VoiceModel>> = voiceRepository.getAllVoiceModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            voiceRepository.refreshLanguages()
            voiceRepository.initializeCatalogIfNeeded()
        }
    }

    companion object {
        fun getCountryForVoice(voice: VoiceModel): String {
            return voice.country.trim()
        }
    }

    val availableCountries: StateFlow<List<String>> = _allVoices.map { voices ->
        val countries = voices.map { it.country.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("All") + countries
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val availableLanguages: StateFlow<List<String>> = combine(
        _allVoices,
        _uiState
    ) { voices, state ->
        val countryFilter = state.selectedCountryFilter
        val voicesInCountry = if (countryFilter.equals("All", ignoreCase = true)) {
            voices
        } else {
            voices.filter { getCountryForVoice(it).equals(countryFilter, ignoreCase = true) }
        }
        val languages = voicesInCountry.map { it.language }.distinct().filter { it.isNotBlank() }.sorted()
        listOf("All") + languages
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val filteredVoices: StateFlow<List<VoiceModel>> = combine(_allVoices, _uiState) { voices, state ->
        voices.filter { voice ->
            val voiceCountry = getCountryForVoice(voice)
            val matchesCountry = state.selectedCountryFilter.equals("All", ignoreCase = true) ||
                    voiceCountry.equals(state.selectedCountryFilter, ignoreCase = true)

            val langFilter = state.selectedLanguageFilter.trim()
            val matchesLanguage = langFilter.isBlank() ||
                    langFilter.equals("All", ignoreCase = true) ||
                    voice.language.contains(langFilter, ignoreCase = true) ||
                    langFilter.contains(voice.language.substringBefore(" "), ignoreCase = true)

            val matchesGender = when (state.selectedGenderFilter.uppercase()) {
                "MALE" -> voice.gender.equals("male", ignoreCase = true)
                "FEMALE" -> voice.gender.equals("female", ignoreCase = true)
                else -> true
            }

            val matchesTier = when (state.selectedTierFilter.uppercase()) {
                "FREE" -> !voice.isPremium
                "PREMIUM" -> voice.isPremium
                else -> true
            }

            val matchesSearch = state.searchQuery.isEmpty() ||
                    voice.name.contains(state.searchQuery, ignoreCase = true) ||
                    voice.language.contains(state.searchQuery, ignoreCase = true) ||
                    voice.accent.contains(state.searchQuery, ignoreCase = true) ||
                    voice.gender.contains(state.searchQuery, ignoreCase = true) ||
                    voiceCountry.contains(state.searchQuery, ignoreCase = true)

            matchesCountry && matchesLanguage && matchesGender && matchesTier && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<AudioPlaybackState> = audioPlayerManager.playbackState

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            voiceRepository.refreshLanguages()
            voiceRepository.initializeCatalogIfNeeded()
            kotlinx.coroutines.delay(600)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterCountry(country: String) {
        val currentVoices = _allVoices.value
        val voicesInNewCountry = if (country.equals("All", ignoreCase = true)) {
            currentVoices
        } else {
            currentVoices.filter { getCountryForVoice(it).equals(country, ignoreCase = true) }
        }
        val availableLangsInCountry = voicesInNewCountry.map { it.language }.distinct()

        val currentLang = _uiState.value.selectedLanguageFilter
        val isLangStillValid = currentLang.equals("All", ignoreCase = true) ||
                availableLangsInCountry.any { it.equals(currentLang, ignoreCase = true) || it.contains(currentLang, ignoreCase = true) }

        _uiState.value = _uiState.value.copy(
            selectedCountryFilter = country,
            selectedLanguageFilter = if (isLangStillValid) currentLang else "All"
        )
    }

    fun onFilterLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguageFilter = language)
    }

    fun onFilterGender(gender: String) {
        _uiState.value = _uiState.value.copy(selectedGenderFilter = gender)
    }

    fun onFilterTier(tier: String) {
        _uiState.value = _uiState.value.copy(selectedTierFilter = tier)
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCountryFilter = "All",
            selectedLanguageFilter = "All",
            selectedGenderFilter = "ALL",
            selectedTierFilter = "ALL",
            searchQuery = ""
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissPremiumPromotion() {
        _uiState.value = _uiState.value.copy(premiumPromotionVoice = null)
    }

    fun selectVoice(voice: VoiceModel) {
        val isPremiumUser = userProfile.value.isPremium
        if (voice.isPremium && !isPremiumUser) {
            _uiState.value = _uiState.value.copy(premiumPromotionVoice = voice)
            return
        }
        _uiState.value = _uiState.value.copy(selectedVoiceId = voice.id)
        viewModelScope.launch {
            voiceRepository.updateModelLastUsed(voice.id)
        }
    }

    fun previewVoiceSample(voice: VoiceModel) {
        val currentPlaying = _uiState.value.activePreviewVoiceId
        if (currentPlaying == voice.id && playbackState.value.isPlaying) {
            audioPlayerManager.stopAudio()
            _uiState.value = _uiState.value.copy(activePreviewVoiceId = null)
        } else {
            _uiState.value = _uiState.value.copy(activePreviewVoiceId = voice.id)
            audioPlayerManager.playAudio(voice.voiceSampleUrl, isSamplePreview = true)
        }
    }

    fun startDownloadModelDirectly(voice: VoiceModel) {
        val isPremiumUser = userProfile.value.isPremium
        if (voice.isPremium && !isPremiumUser) {
            _uiState.value = _uiState.value.copy(premiumPromotionVoice = voice)
            return
        }
        if (voice.isDownloading || voice.isDownloaded) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloadingAny = true)
            voiceRepository.downloadVoiceModel(
                voice = voice,
                selectedModelId = _uiState.value.selectedVoiceId,
                activeGeneratingModelId = null,
                onProgress = {},
                onComplete = { success, errorMsg ->
                    _uiState.value = _uiState.value.copy(
                        isDownloadingAny = false,
                        selectedVoiceId = if (success) voice.id else _uiState.value.selectedVoiceId,
                        errorMessage = if (!success) (errorMsg ?: "Failed to download voice package.") else null
                    )
                }
            )
        }
    }

    fun requestDownloadConfirmation(voice: VoiceModel) {
        startDownloadModelDirectly(voice)
    }

    class Factory(
        private val voiceRepo: VoiceRepository,
        private val userRepo: UserRepository,
        private val audioPlayer: AudioPlayerManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VoiceCatalogViewModel(voiceRepo, userRepo, audioPlayer) as T
        }
    }
}

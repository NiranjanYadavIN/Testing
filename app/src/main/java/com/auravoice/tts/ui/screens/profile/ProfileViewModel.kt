package com.auravoice.tts.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auravoice.tts.data.model.GenerationHistory
import com.auravoice.tts.data.model.UserProfile
import com.auravoice.tts.data.repository.UserRepository
import com.auravoice.tts.engine.AudioPlayerManager
import com.auravoice.tts.data.remote.AppSettingsConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val appSettings: StateFlow<AppSettingsConfig> = userRepository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsConfig())

    val historyLogs: StateFlow<List<GenerationHistory>> = userRepository.generationHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshUserData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            userRepository.refreshUserData()
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
        }
    }

    fun upgradeToProPlan() {
        userRepository.upgradeSubscription()
    }

    fun signOut() {
        userRepository.signOut()
    }

    fun playHistoryAudio(path: String) {
        audioPlayerManager.playAudio(path)
    }

    class Factory(
        private val userRepo: UserRepository,
        private val audioPlayer: AudioPlayerManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(userRepo, audioPlayer) as T
        }
    }
}

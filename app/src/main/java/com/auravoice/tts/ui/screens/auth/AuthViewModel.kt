package com.auravoice.tts.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auravoice.tts.data.model.UserProfile
import com.auravoice.tts.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    private var attemptCount = 0

    private fun startCooldown(seconds: Int) {
        viewModelScope.launch {
            _cooldownSeconds.value = seconds
            while (_cooldownSeconds.value > 0) {
                delay(1000)
                _cooldownSeconds.value -= 1
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    fun login(
        email: String,
        pass: String,
        onClearPassword: () -> Unit = {},
        onSuccess: () -> Unit
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Please enter both email and password")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState(errorMessage = "Please enter a valid email address")
            return
        }
        if (_cooldownSeconds.value > 0) {
            _uiState.value = AuthUiState(errorMessage = "Rate limit active! Please wait ${_cooldownSeconds.value} seconds before trying again.")
            return
        }

        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val success = userRepository.signInWithEmail(trimmedEmail, pass)
            if (success) {
                if (userRepository.isUserAuthenticatedAndVerified()) {
                    attemptCount = 0
                    _uiState.value = AuthUiState(isSuccess = true, successMessage = "Logged in successfully")
                    onSuccess()
                } else {
                    onClearPassword()
                    attemptCount++
                    val cooldown = if (attemptCount >= 3) 45 else 15
                    startCooldown(cooldown)
                    _uiState.value = AuthUiState(
                        errorMessage = "Email verification required! Please check your inbox ($trimmedEmail) and click the verification link before logging in."
                    )
                }
            } else {
                onClearPassword()
                attemptCount++
                val cooldown = if (attemptCount >= 3) 30 else 10
                startCooldown(cooldown)
                _uiState.value = AuthUiState(errorMessage = "Invalid credentials. Please check your email and password.")
            }
        }
    }

    fun signUp(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Please fill in all required fields")
            return
        }
        if (trimmedName.length < 2) {
            _uiState.value = AuthUiState(errorMessage = "Name must be at least 2 characters long")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState(errorMessage = "Please enter a valid email address")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState(errorMessage = "Password must be at least 6 characters long")
            return
        }
        if (_cooldownSeconds.value > 0) {
            _uiState.value = AuthUiState(errorMessage = "Please wait ${_cooldownSeconds.value} seconds before submitting again.")
            return
        }

        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val success = userRepository.registerWithEmail(trimmedName, trimmedEmail, pass)
            if (success) {
                startCooldown(30)
                _uiState.value = AuthUiState(
                    isSuccess = true,
                    successMessage = "Account created! A verification email has been sent to $trimmedEmail. Please verify your email before signing in."
                )
            } else {
                startCooldown(15)
                _uiState.value = AuthUiState(errorMessage = "Failed to create account. Check email address or try a different one.")
            }
        }
    }

    fun resendVerificationEmail() {
        if (_cooldownSeconds.value > 0) {
            _uiState.value = AuthUiState(
                errorMessage = "Rate limit active! Please wait ${_cooldownSeconds.value} seconds before requesting another email."
            )
            return
        }
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val sent = userRepository.sendEmailVerification()
            startCooldown(30)
            if (sent) {
                _uiState.value = AuthUiState(
                    isSuccess = true,
                    successMessage = "Verification link resent successfully! Check your email inbox."
                )
            } else {
                _uiState.value = AuthUiState(
                    errorMessage = "Failed to resend verification email. Please make sure you signed up first."
                )
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = AuthUiState(errorMessage = "Please enter a valid email address")
            return
        }
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val success = userRepository.sendPasswordResetEmail(email)
            if (success) {
                _uiState.value = AuthUiState(
                    isSuccess = true,
                    successMessage = "Password reset instructions sent to $email. Check your inbox."
                )
                onSuccess()
            } else {
                _uiState.value = AuthUiState(errorMessage = "Failed to send reset email. Verify email address.")
            }
        }
    }

    fun signInWithGoogle(idToken: String, googleEmail: String, displayName: String?, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val success = userRepository.signInWithGoogle(idToken, googleEmail, displayName ?: "Aura User")
            if (success) {
                _uiState.value = AuthUiState(isSuccess = true, successMessage = "Signed in with Google successfully")
                onSuccess()
            } else {
                _uiState.value = AuthUiState(errorMessage = "Google sign-in failed. Please try again.")
            }
        }
    }

    fun setErrorMessage(message: String) {
        _uiState.value = AuthUiState(errorMessage = message)
    }

    fun isUserAuthenticatedAndVerified(): Boolean {
        return userRepository.isUserAuthenticatedAndVerified()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(userRepository) as T
        }
    }
}

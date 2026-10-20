package com.auravoice.tts.data.repository

import com.auravoice.tts.data.local.GenerationHistoryDao
import com.auravoice.tts.data.local.GenerationHistoryEntity
import com.auravoice.tts.data.model.GenerationHistory
import com.auravoice.tts.data.model.UserProfile
import com.auravoice.tts.data.remote.FirebaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    private val firebaseManager: FirebaseManager,
    private val historyDao: GenerationHistoryDao
) {
    val userProfile: Flow<UserProfile> = firebaseManager.userProfile
    val appSettings = firebaseManager.appSettings

    fun refreshUserData() {
        firebaseManager.refreshUserData()
    }

    val generationHistory: Flow<List<GenerationHistory>> = historyDao.getAllHistory().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun recordSpeechGeneration(
        history: GenerationHistory,
        voiceId: String = "",
        language: String = "",
        rawText: String = ""
    ) {
        historyDao.insertHistory(GenerationHistoryEntity.fromDomainModel(history))
        firebaseManager.updateUsageStats(history.characterCount)

        val preview = if (rawText.isNotBlank()) {
            val words = rawText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            val firstWords = words.take(10).joinToString(" ")
            if (words.size > 10) "$firstWords..." else firstWords
        } else {
            history.textPreview
        }

        firebaseManager.recordGenerationHistoryInFirestore(
            voiceName = history.voiceName,
            voiceId = voiceId,
            language = language,
            characterCount = history.characterCount,
            creditSpended = history.characterCount,
            textPreview = preview,
            timestamp = history.timestamp
        )
    }

    fun upgradeSubscription() {
        firebaseManager.upgradeToPremium()
    }

    fun isUserAuthenticatedAndVerified(): Boolean {
        return firebaseManager.isUserAuthenticatedAndVerified()
    }

    suspend fun sendEmailVerification(): Boolean {
        return firebaseManager.sendEmailVerification()
    }

    suspend fun signInWithEmail(email: String, pass: String): Boolean {
        return firebaseManager.signInWithEmail(email, pass)
    }

    suspend fun registerWithEmail(name: String, email: String, pass: String): Boolean {
        return firebaseManager.registerWithEmail(name, email, pass)
    }

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return firebaseManager.sendPasswordResetEmail(email)
    }

    suspend fun signInWithGoogle(idToken: String, googleEmail: String, displayName: String?): Boolean {
        return firebaseManager.signInWithGoogle(idToken, googleEmail, displayName)
    }

    fun signOut() {
        firebaseManager.signOut()
    }
}

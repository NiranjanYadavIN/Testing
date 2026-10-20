package com.auravoice.tts.data.remote

import android.util.Log
import com.auravoice.tts.data.model.TtsEngineType
import com.auravoice.tts.data.model.UserProfile
import com.auravoice.tts.utils.CountryUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class FirebaseVoiceConfig(
    val id: String,
    val name: String,
    val engine: TtsEngineType,
    val language: String,
    val gender: String,
    val accent: String,
    val modelDownloadUrl: String,
    val voiceSampleUrl: String,
    val isPremium: Boolean,
    val country: String = ""
)

data class FirebaseLanguageConfig(
    val id: String,
    val name: String,
    val code: String,
    val voiceCount: Int = 1
)

data class AppSettingsConfig(
    val id: String = "speech_generator_limits",
    val freeUserCharLimitPerRequest: Int = 5000,
    val premiumUserCharLimitPerRequest: Int = 10000,
    val freeDailyCharLimit: Int = 10000,
    val premiumDailyCharLimit: Int = 500000,
    val defaultEngine: String = "AURA_NEURAL",
    val minAppVersion: String = "1.0.0",
    val maintenanceMode: Boolean = false,
    val welcomeMessage: String = "Welcome to AuraVoice AI Speech Synthesis"
)

class FirebaseManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _firestoreLanguages = MutableStateFlow<List<FirebaseLanguageConfig>>(emptyList())
    val firestoreLanguages: StateFlow<List<FirebaseLanguageConfig>> = _firestoreLanguages.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettingsConfig())
    val appSettings: StateFlow<AppSettingsConfig> = _appSettings.asStateFlow()

    private var userSnapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Auth State Listener to sync real Firebase User with Firestore
        try {
            val initialUser = auth.currentUser
            if (initialUser != null) {
                _userProfile.value = UserProfile(
                    userId = initialUser.uid,
                    displayName = initialUser.displayName ?: initialUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User",
                    email = initialUser.email ?: "user@auravoice.ai",
                    photoUrl = initialUser.photoUrl?.toString()
                )
                attachUserProfileListener(initialUser.uid, initialUser.email, initialUser.displayName)
            }
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    attachUserProfileListener(user.uid, user.email, user.displayName)
                } else {
                    userSnapshotListener?.remove()
                    userSnapshotListener = null
                    _userProfile.value = UserProfile(
                        userId = "",
                        displayName = "",
                        email = "",
                        isPremium = false,
                        creditsRemaining = 0,
                        dailyUsageChars = 0,
                        dailyLimitChars = 5000,
                        monthlyUsageChars = 0,
                        monthlyLimitChars = 50000,
                        voicePermissions = listOf("aura_en_male_1", "aura_en_female_1", "aura_en_female_2", "aura_en_male_2")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Auth listener init error: ${e.localizedMessage}")
        }

        // Initialize Firestore languages, settings & catalog
        scope.launch {
            loadLanguagesFromFirestore()
            loadSettingsFromFirestore()
        }
    }

    fun refreshUserData() {
        val user = auth.currentUser
        scope.launch {
            loadSettingsFromFirestore()
            if (user != null) {
                syncUserProfileFromFirestore(user.uid, user.email, user.displayName)
            }
        }
    }

    private fun getCurrentUtcDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    private fun attachUserProfileListener(uid: String, email: String?, displayName: String?) {
        userSnapshotListener?.remove()
        if (_userProfile.value.userId.isEmpty()) {
            _userProfile.value = UserProfile(
                userId = uid,
                displayName = displayName ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User",
                email = email ?: "user@auravoice.ai",
                photoUrl = auth.currentUser?.photoUrl?.toString()
            )
        }
        val docRef = firestore.collection("users").document(uid)
        
        userSnapshotListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseManager", "User snapshot error: ${error.localizedMessage}")
                return@addSnapshotListener
            }

            val currentUtcDate = getCurrentUtcDate()

            if (snapshot != null && snapshot.exists()) {
                val isPremium = snapshot.getBoolean("isPremium") ?: false
                var credits = snapshot.getLong("creditsRemaining")?.toInt() ?: 10000
                var dailyUsage = snapshot.getLong("dailyUsageChars")?.toInt() ?: 0
                val dailyLimit = snapshot.getLong("dailyLimitChars")?.toInt() ?: (if (isPremium) 100000 else 10000)
                val monthlyUsage = snapshot.getLong("monthlyUsageChars")?.toInt() ?: 0
                val monthlyLimit = snapshot.getLong("monthlyLimitChars")?.toInt() ?: 500000
                val lastResetDate = snapshot.getString("lastResetDate") ?: ""
                val name = snapshot.getString("displayName") ?: displayName ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
                val photoUrl = snapshot.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()

                // Check for UTC Midnight Daily Credit Reset
                if (lastResetDate != currentUtcDate) {
                    credits = if (isPremium) 100000 else 10000
                    dailyUsage = 0
                    scope.launch {
                        try {
                            docRef.set(
                                mapOf(
                                    "creditsRemaining" to credits,
                                    "dailyUsageChars" to 0,
                                    "dailyLimitChars" to (if (isPremium) 100000 else 10000),
                                    "lastResetDate" to currentUtcDate
                                ),
                                SetOptions.merge()
                            ).await()
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error resetting daily credits: ${e.localizedMessage}")
                        }
                    }
                }

                _userProfile.value = UserProfile(
                    userId = uid,
                    displayName = name,
                    email = email ?: "user@auravoice.ai",
                    photoUrl = photoUrl,
                    isPremium = isPremium,
                    creditsRemaining = credits,
                    dailyUsageChars = dailyUsage,
                    dailyLimitChars = dailyLimit,
                    monthlyUsageChars = monthlyUsage,
                    monthlyLimitChars = monthlyLimit,
                    lastResetDate = currentUtcDate,
                    voicePermissions = if (isPremium) listOf("aura_en_male_1", "aura_en_female_1", "aura_en_female_2", "aura_en_male_2", "aura_en_female_3")
                    else listOf("aura_en_male_1", "aura_en_female_1", "aura_en_female_2")
                )
            } else {
                scope.launch {
                    val name = displayName ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
                    val initialData = hashMapOf(
                        "userId" to uid,
                        "displayName" to name,
                        "email" to (email ?: "user@auravoice.ai"),
                        "isPremium" to false,
                        "creditsRemaining" to 10000,
                        "dailyUsageChars" to 0,
                        "dailyLimitChars" to 10000,
                        "monthlyUsageChars" to 0,
                        "monthlyLimitChars" to 500000,
                        "lastResetDate" to currentUtcDate,
                        "createdAt" to System.currentTimeMillis()
                    )
                    docRef.set(initialData).await()
                }
            }
        }
    }

    private suspend fun syncUserProfileFromFirestore(uid: String, email: String?, displayName: String?) {
        attachUserProfileListener(uid, email, displayName)
    }

    /**
     * Firestore Languages Collection
     */
    suspend fun loadLanguagesFromFirestore(): List<FirebaseLanguageConfig> {
        return try {
            val snapshot = firestore.collection("languages").get().await()
            val languages = if (!snapshot.isEmpty) {
                snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val code = doc.getString("code") ?: "en-US"
                    val count = doc.getLong("voiceCount")?.toInt() ?: 1
                    FirebaseLanguageConfig(id = id, name = name, code = code, voiceCount = count)
                }
            } else {
                emptyList()
            }
            _firestoreLanguages.value = languages
            languages
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error loading languages: ${e.localizedMessage}")
            _firestoreLanguages.value
        }
    }

    /**
     * Firestore Settings Collection (Speech limits per request for free/premium users, app config)
     */
    suspend fun loadSettingsFromFirestore(): AppSettingsConfig {
        return try {
            val docRef = firestore.collection("settings").document("speech_generator_limits")
            val doc = docRef.get().await()
            val settings = if (doc.exists()) {
                AppSettingsConfig(
                    id = doc.id,
                    freeUserCharLimitPerRequest = doc.getLong("freeUserCharLimitPerRequest")?.toInt() 
                        ?: doc.getLong("maxCharLimitPerRequest")?.toInt() 
                        ?: doc.getLong("charLimitPerRequest")?.toInt() 
                        ?: 5000,
                    premiumUserCharLimitPerRequest = doc.getLong("premiumUserCharLimitPerRequest")?.toInt() ?: 10000,
                    freeDailyCharLimit = doc.getLong("freeDailyCharLimit")?.toInt() ?: 10000,
                    premiumDailyCharLimit = doc.getLong("premiumDailyCharLimit")?.toInt() ?: 500000,
                    defaultEngine = doc.getString("defaultEngine") ?: "AURA_NEURAL",
                    minAppVersion = doc.getString("minAppVersion") ?: "1.0.0",
                    maintenanceMode = doc.getBoolean("maintenanceMode") ?: false,
                    welcomeMessage = doc.getString("welcomeMessage") ?: "Welcome to AuraVoice AI Speech Synthesis"
                )
            } else {
                AppSettingsConfig()
            }
            _appSettings.value = settings
            settings
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error loading settings from Firestore: ${e.localizedMessage}")
            _appSettings.value
        }
    }

    /**
     * Firebase Firestore Single Source of Truth for Voice Models
     */
    suspend fun getVoiceCatalogFromFirebase(): List<FirebaseVoiceConfig> {
        return try {
            val snapshot = firestore.collection("voices").get().await()
            if (snapshot.isEmpty) {
                getDefaultCatalog()
            } else {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val engineStr = doc.getString("engine") ?: "AURA_NEURAL"
                    val engine = runCatching { TtsEngineType.valueOf(engineStr) }.getOrDefault(TtsEngineType.AURA_NEURAL)
                    val language = doc.getString("language") ?: "English (US)"
                    val gender = doc.getString("gender") ?: "Female"
                    val accent = doc.getString("accent") ?: "Neutral"
                    val rawModelUrl = doc.getString("modelDownloadUrl") ?: ""
                    val modelUrl = rawModelUrl

                    val sampleUrl = doc.getString("voiceSampleUrl") ?: ""
                    val isPremium = doc.getBoolean("isPremium") ?: false
                    val country = doc.getString("country")?.trim() ?: ""

                    FirebaseVoiceConfig(
                        id = id,
                        name = name,
                        engine = engine,
                        language = language,
                        gender = gender,
                        accent = accent,
                        modelDownloadUrl = modelUrl,
                        voiceSampleUrl = sampleUrl,
                        isPremium = isPremium,
                        country = country
                    )
                }
                if (list.isEmpty()) getDefaultCatalog() else list
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching voices from Firestore: ${e.localizedMessage}")
            getDefaultCatalog()
        }
    }

    private fun getDefaultCatalog(): List<FirebaseVoiceConfig> {
        return listOf(
            FirebaseVoiceConfig("aura_en_us_1", "Ava (Neural)", TtsEngineType.AURA_NEURAL, "English (US)", "Female", "American", "", "", false, "United States"),
            FirebaseVoiceConfig("aura_en_us_2", "Andrew (Neural)", TtsEngineType.AURA_NEURAL, "English (US)", "Male", "American", "", "", false, "United States"),
            FirebaseVoiceConfig("aura_en_us_3", "Emma (Neural)", TtsEngineType.AURA_NEURAL, "English (US)", "Female", "American", "", "", true, "United States"),
            FirebaseVoiceConfig("aura_en_gb_1", "Sonia (Neural)", TtsEngineType.AURA_NEURAL, "English (UK)", "Female", "British", "", "", false, "United Kingdom"),
            FirebaseVoiceConfig("aura_en_gb_2", "Ryan (Neural)", TtsEngineType.AURA_NEURAL, "English (UK)", "Male", "British", "", "", true, "United Kingdom"),
            FirebaseVoiceConfig("aura_hi_in_1", "Swara (Neural)", TtsEngineType.AURA_NEURAL, "Hindi (India)", "Female", "Indian", "", "", false, "India"),
            FirebaseVoiceConfig("aura_hi_in_2", "Madhur (Neural)", TtsEngineType.AURA_NEURAL, "Hindi (India)", "Male", "Indian", "", "", false, "India"),
            FirebaseVoiceConfig("aura_hi_in_3", "Ananya (Neural)", TtsEngineType.AURA_NEURAL, "Hindi (India)", "Female", "Indian", "", "", true, "India"),
            FirebaseVoiceConfig("aura_es_es_1", "Elvira (Neural)", TtsEngineType.AURA_NEURAL, "Spanish (Spain)", "Female", "Spanish", "", "", false, "Spain"),
            FirebaseVoiceConfig("aura_es_es_2", "Alvaro (Neural)", TtsEngineType.AURA_NEURAL, "Spanish (Spain)", "Male", "Spanish", "", "", true, "Spain"),
            FirebaseVoiceConfig("aura_fr_fr_1", "Denise (Neural)", TtsEngineType.AURA_NEURAL, "French (France)", "Female", "French", "", "", false, "France"),
            FirebaseVoiceConfig("aura_fr_fr_2", "Henri (Neural)", TtsEngineType.AURA_NEURAL, "French (France)", "Male", "French", "", "", true, "France"),
            FirebaseVoiceConfig("aura_de_de_1", "Katja (Neural)", TtsEngineType.AURA_NEURAL, "German (Germany)", "Female", "German", "", "", false, "Germany"),
            FirebaseVoiceConfig("aura_de_de_2", "Conrad (Neural)", TtsEngineType.AURA_NEURAL, "German (Germany)", "Male", "German", "", "", true, "Germany"),
            FirebaseVoiceConfig("aura_ja_jp_1", "Nanami (Neural)", TtsEngineType.AURA_NEURAL, "Japanese (Japan)", "Female", "Japanese", "", "", true, "Japan"),
            FirebaseVoiceConfig("aura_zh_cn_1", "Xiaoxiao (Neural)", TtsEngineType.AURA_NEURAL, "Chinese (China)", "Female", "Chinese", "", "", true, "China")
        )
    }

    fun updateUsageStats(charsGenerated: Int) {
        val current = _userProfile.value
        val newCredits = (current.creditsRemaining - charsGenerated).coerceAtLeast(0)
        val newDaily = current.dailyUsageChars + charsGenerated
        val newMonthly = current.monthlyUsageChars + charsGenerated

        _userProfile.value = current.copy(
            creditsRemaining = newCredits,
            dailyUsageChars = newDaily,
            monthlyUsageChars = newMonthly
        )

        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "creditsRemaining" to newCredits,
                        "dailyUsageChars" to newDaily,
                        "monthlyUsageChars" to newMonthly
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error updating usage in Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun recordGenerationHistoryInFirestore(
        voiceName: String,
        voiceId: String,
        language: String,
        characterCount: Int,
        creditSpended: Int,
        textPreview: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: _userProfile.value.userId.ifBlank { "anonymous" }
        val email = currentUser?.email ?: _userProfile.value.email
        val userName = currentUser?.displayName ?: _userProfile.value.displayName

        scope.launch {
            try {
                val usageData = mapOf(
                    "userId" to uid,
                    "userEmail" to email,
                    "userName" to userName,
                    "voiceName" to voiceName,
                    "voiceId" to voiceId,
                    "language" to language,
                    "characterCount" to characterCount,
                    "creditSpended" to creditSpended,
                    "textPreview" to textPreview,
                    "timestamp" to timestamp
                )
                firestore.collection("history")
                    .add(usageData)
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error saving usage history to Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun upgradeToPremium() {
        val current = _userProfile.value
        _userProfile.value = current.copy(
            isPremium = true,
            creditsRemaining = 100000,
            dailyLimitChars = 100000,
            monthlyLimitChars = 1000000,
            voicePermissions = listOf("aura_en_male_1", "aura_en_female_1", "aura_en_female_2", "aura_en_male_2", "aura_en_female_3")
        )

        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "isPremium" to true,
                        "creditsRemaining" to 100000,
                        "dailyLimitChars" to 100000,
                        "monthlyLimitChars" to 1000000
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error upgrading premium in Firestore: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Firebase Auth Methods
     */
    suspend fun signInWithEmail(email: String, pass: String): Boolean {
        return try {
            val trimmedEmail = email.trim()
            val result = auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
            val user = result.user
            if (user != null) {
                try {
                    user.reload().await()
                } catch (re: Exception) {
                    Log.w("FirebaseManager", "User reload error: ${re.localizedMessage}")
                }
                syncUserProfileFromFirestore(user.uid, user.email ?: trimmedEmail, user.displayName)
                true
            } else false
        } catch (e: Exception) {
            Log.e("FirebaseManager", "SignIn Error: ${e.localizedMessage}")
            false
        }
    }

    fun isUserAuthenticatedAndVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return true
    }

    suspend fun sendEmailVerification(): Boolean {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Send verification error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun registerWithEmail(name: String, email: String, pass: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
                try {
                    user.sendEmailVerification().await()
                } catch (ve: Exception) {
                    Log.w("FirebaseManager", "Email verification send issue: ${ve.localizedMessage}")
                }
                syncUserProfileFromFirestore(user.uid, email, name)
                true
            } else false
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Registration Error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Password Reset Error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String, googleEmail: String, displayName: String?): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return false
            val name = displayName ?: googleEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            _userProfile.value = _userProfile.value.copy(
                userId = uid,
                displayName = name,
                email = googleEmail
            )
            syncUserProfileFromFirestore(uid, googleEmail, name)
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Google Sign-In failed: ${e.localizedMessage}")
            false
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "SignOut error: ${e.localizedMessage}")
        }
        _userProfile.value = UserProfile(
            userId = "guest_${UUID.randomUUID().toString().take(8)}",
            displayName = "Guest User",
            email = "guest@auravoice.ai",
            isPremium = false,
            creditsRemaining = 2000,
            dailyUsageChars = 0,
            dailyLimitChars = 5000,
            monthlyUsageChars = 0,
            monthlyLimitChars = 50000,
            voicePermissions = listOf("aura_en_male_1", "aura_en_female_1", "aura_en_female_2", "aura_en_male_2")
        )
    }
}

package com.auravoice.tts.data.model

data class UserProfile(
    val userId: String = "",
    val displayName: String = "User",
    val email: String = "",
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val creditsRemaining: Int = 3800,
    val dailyUsageChars: Int = 1200,
    val dailyLimitChars: Int = 5000,
    val monthlyUsageChars: Int = 14200,
    val monthlyLimitChars: Int = 50000,
    val lastResetDate: String = "",
    val voicePermissions: List<String> = listOf("aura_en_male_1", "aura_en_female_1", "aura_en_female_2", "aura_en_male_2")
)

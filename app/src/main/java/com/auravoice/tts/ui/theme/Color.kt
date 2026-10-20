package com.auravoice.tts.ui.theme

import androidx.compose.ui.graphics.Color

// ChatGPT Dark Theme Colors (Refined Soft Charcoal Dark Mode)
val DeepBackgroundDark = Color(0xFF171717)             // ChatGPT soft charcoal canvas (#171717)
val SurfaceDark = Color(0xFF212121)                    // ChatGPT dark surface card container (#212121)
val SurfaceVariantDark = Color(0xFF2F2F2F)             // ChatGPT elevated dark chip surface (#2F2F2F)

val PrimaryViolet = Color(0xFF6366F1)                  // Vibrant Indigo primary accent (#6366F1)
val PrimaryVioletLight = Color(0xFF818CF8)             // Soft Indigo (#818CF8)
val PrimaryVioletContainerDark = Color(0xFF2E2F38)      // Dark Indigo container
val PrimaryVioletContainerLight = Color(0xFFEEF2FF)     // Light Indigo container

// Premium Blue Accents (Replacing green with electric premium blue)
val PremiumBlue = Color(0xFF3B82F6)                    // Electric Premium Blue (#3B82F6)
val PremiumBlueLight = Color(0xFF60A5FA)               // Soft Premium Blue (#60A5FA)
val PremiumBlueDark = Color(0xFF2563EB)                // Deep Premium Blue (#2563EB)

val TextPrimaryDark = Color(0xFFECECF1)                // ChatGPT soft white heading text (#ECECF1)
val TextSecondaryDark = Color(0xFFC5C5D2)              // ChatGPT secondary slate text (#C5C5D2)
val TextMutedDark = Color(0xFF8E8EA0)                  // ChatGPT muted caption text (#8E8EA0)

val CardBorderDark = Color(0xFF3E3F4B)                 // ChatGPT subtle dark border (#3E3F4B)

// ChatGPT Light Theme Colors (Clean Off-White Light Mode)
val DeepBackgroundLight = Color(0xFFFFFFFF)            // ChatGPT clean white canvas (#FFFFFF)
val SurfaceLight = Color(0xFFF9F9FB)                   // ChatGPT soft off-white card surface (#F9F9FB)
val SurfaceVariantLight = Color(0xFFECECF1)            // ChatGPT soft grey surface variant (#ECECF1)

val TextPrimaryLight = Color(0xFF0D0D0D)               // ChatGPT dark primary text (#0D0D0D)
val TextSecondaryLight = Color(0xFF5D5D6D)             // ChatGPT medium grey text (#5D5D6D)
val TextMutedLight = Color(0xFF8E8EA0)                 // ChatGPT muted caption text (#8E8EA0)

val CardBorderLight = Color(0xFFE5E5E5)                // ChatGPT subtle light border (#E5E5E5)

// Common branding colors
val SuccessGreen = Color(0xFF10B981)                   // Only used for explicit success status badges if needed
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Legacy aliases for fallback
val DeepBackground = DeepBackgroundDark
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark
val TextMuted = TextMutedDark
val CardBorder = CardBorderDark
val AccentCyanDark = PremiumBlueLight
val AccentCyanLight = PremiumBlue
val AccentCyan = PremiumBlue
val PrimaryVioletContainer = PrimaryVioletContainerDark

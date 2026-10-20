# AuraVoice R8 Optimization Rules

# Keep App Models & Entities
-keep class com.auravoice.tts.data.model.** { *; }
-keep class com.auravoice.tts.data.local.** { *; }

# Preserve Room generated implementation classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retain Coroutines internal dispatcher optimization
-keepclassmembers class kotlinx.coroutines.** { *; }

# ONNX Runtime SDK Protection
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }

# Retrofit, Moshi & OkHttp JSON Serialization
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Firebase & Google Play Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**


# 🎙️ AuraVoice - On-Device Neural TTS Studio

**AuraVoice** is a production-grade, privacy-first Android application for high-fidelity, real-time Text-to-Speech (TTS) synthesis. Designed with an **on-device neural architecture**, AuraVoice runs state-of-the-art TTS models directly on the user's device via ONNX Runtime — delivering near-instant speech generation with zero cloud latency and zero voice data logging.

---

## 🌟 Key Highlights

- **100% On-Device Neural Speech Generation**: Runs **Kokoro-82M ONNX** and **Piper ONNX** models directly on the Android NPU/CPU using ONNX Runtime.
- **Dual Neural Engines**:
  - **Kokoro 82M ONNX**: Ultra-realistic 82-million parameter neural TTS engine support with 50+ custom voice style embeddings (`.bin`).
  - **Piper ONNX**: Fast, lightweight neural voice engine supporting 170+ voice models across 30+ languages.
- **Public Audio Export & Sharing**: Automatically exports generated `.wav` speech clips to the public `Music/AuraVoice` folder via Android `MediaStore`, making files easily discoverable in standard File Managers and Music Player apps. Native sharing via Android `FileProvider`.
- **Firebase Dynamic Catalog**: Lightweight Cloud Firestore integration to sync voice model metadata, download mirrors, and usage statistics **without** saving large raw audio files to cloud storage.
- **Offline First**: All synthesized audio is generated locally. Cached voice models and speech history are persisted using Room Database.
- **Material 3 Design System**: Clean, modern dark/light themes with animated audio visualizers, dynamic pitch/rate controls, and intuitive model downloading screens.

---

## 🏗️ System Architecture

```
                                  +-----------------------+
                                  |   Firebase Firestore  |
                                  | (Voice Metadata Only) |
                                  +-----------+-----------+
                                              |
                                              v
+------------------------+        +-----------+-----------+        +-----------------------+
|  Hugging Face Mirrors  | ------ |   Voice Model Repository  | ------ |   Room Database       |
| (Kokoro ONNX & Piper)  |        |    (AuraVoice Engine)    |        | (Local Model Registry)|
+------------------------+        +-----------+-----------+        +-----------------------+
                                              |
                                              v
                                  +-----------+-----------+
                                  |    ONNX Runtime Engine    |
                                  |  (Kokoro 82M & Piper C++) |
                                  +-----------+-----------+
                                              |
                                              v
                                  +-----------+-----------+
                                  | Local Audio WAV Generator |
                                  +-----------+-----------+
                                              |
                                              v
                                  +-----------+-----------+
                                  |  Music/AuraVoice Folder   |
                                  |  & Android FileProvider   |
                                  +-----------------------+
```

---

## 📂 Hugging Face Repositories & Engine Specifications

AuraVoice dynamically fetches official ONNX model weights and voice configuration metadata from Hugging Face repositories:

### 1. Piper Voices Repository
- **URL**: [https://huggingface.co/NiranjanY/piper-voices](https://huggingface.co/NiranjanY/piper-voices)
- **Primary Index**: `resolve/main/voices.json`
- **Structure**:
  ```json
  {
    "ar_JO-kareem-medium": {
      "key": "ar_JO-kareem-medium",
      "name": "kareem",
      "language": {
        "code": "ar_JO",
        "family": "ar",
        "region": "JO",
        "name_native": "العربية",
        "name_english": "Arabic"
      },
      "quality": "medium",
      "files": {
        "ar/ar_JO/kareem/medium/ar_JO-kareem-medium.onnx": { "size_bytes": 63201294 },
        "ar/ar_JO/kareem/medium/ar_JO-kareem-medium.onnx.json": { "size_bytes": 5024 }
      }
    }
  }
  ```

### 2. Kokoro 82M ONNX Repository
- **URL**: [https://huggingface.co/NiranjanY/Kokoro-82M-v1.0-ONNX](https://huggingface.co/NiranjanY/Kokoro-82M-v1.0-ONNX)
- **Model Weights (`/onnx`)**: `model.onnx`, `model_quantized.onnx` (~92MB), `model_q8f16.onnx` (~86MB), `model_q4f16.onnx` (~154MB).
- **Voice Embeddings (`/voices`)**: Binary 256-dimensional float vector files (`af_bella.bin`, `am_adam.bin`, `bf_emma.bin`, `zf_xiaobei.bin`, etc.).

---

## 🌐 Language & Code Reference Guide

When adding or configuring new languages in AuraVoice or updating Firebase/HuggingFace indices, keep in mind the distinction between these standard identifier formats:

| Concept | Example | Explanation | Usage in AuraVoice |
|---|---|---|---|
| **BCP-47 Code** | `en-US`, `hi-IN`, `ar-JO` | IETF standard language tag separated by a hyphen `-`. | Used in Android Locale, Android TTS settings, and UI display. |
| **Language Slug ID** | `en_US`, `hi_IN`, `ar_JO` | File-system safe folder/identifier tag separated by an underscore `_`. | Used in Hugging Face repository folder paths, model key prefixes, and local file storage. |

---

## 🛡️ Firebase & Privacy Architecture

- **No Cloud Audio Storage**: To protect user privacy and avoid unnecessary server bandwidth costs, generated audio files (`.wav`) are **NEVER uploaded to Firebase Storage or external servers**.
- **Local Persistence Only**: All synthesized files remain strictly in the app's local storage and the user's public `Music/AuraVoice` folder.
- **Firestore Usage**: Used exclusively for:
  - User authentication / anonymous session records.
  - Remote configuration of featured voice models and Hugging Face mirror endpoints.
  - Minimal metadata history logs (text snippet preview, timestamp, engine type used).

---

## ⚙️ Configuration & Release Build Instructions

### Prerequisites
- **JDK**: Version 17 or higher
- **Android SDK**: API Level 34 (Target SDK), Minimum API Level 26
- **Gradle**: Kotlin DSL (`.gradle.kts`)

### Release Signing Setup
To sign release APKs / AABs, configure your keystore properties in `app/build.gradle.kts` or environment variables:

- **Key Store File**: `keystore.jks`
- **Key Alias**: `auravoice`
- **Store Password**: Configured via build secrets / local properties
- **Key Password**: Configured via build secrets / local properties

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "YOUR_STORE_PASSWORD"
            keyAlias = "auravoice"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Build Commands

1. **Compile & Lint Check**:
   ```bash
   gradle compileDebugKotlin
   ```

2. **Build Debug APK**:
   ```bash
   gradle :app:assembleDebug
   ```

3. **Build Release APK**:
   ```bash
   gradle :app:assembleRelease
   ```

4. **Run Unit Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 📁 Repository Structure

```
AuraVoice/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/java/com/auravoice/tts/
│       ├── data/
│       │   ├── local/            # Room Database entities, DAOs, & Migrations
│       │   ├── remote/           # FirebaseManager & HuggingFaceApiService
│       │   └── repository/       # VoiceRepository & UserRepository
│       ├── engine/               # KokoroEngine & PiperEngine (ONNX synthesis)
│       ├── ui/
│       │   ├── components/       # Waveforms, dialogs, sliders & cards
│       │   ├── screens/
│       │   │   ├── home/         # Primary TTS studio interface
│       │   │   ├── history/      # Speech history & audio player
│       │   │   ├── models/       # Voice model browser & downloader
│       │   │   ├── cache/        # Storage management & cache cleaner
│       │   │   ├── profile/      # User profile & stats
│       │   │   └── settings/     # App settings & keystore/storage info
│       │   └── theme/            # Material 3 Color Schemes & Typography
│       └── util/                 # AudioExportUtils, FileProvider, & Extensions
├── gradle/                       # Version catalogs (libs.versions.toml)
├── metadata.json                 # AI Studio Platform Metadata
└── README.md                     # Project documentation
```

---

## 👨‍💻 Maintainer & Contact

- **Lead Engineer**: Niranjan Y.
- **Hugging Face Hub**: [NiranjanY on HuggingFace](https://huggingface.co/NiranjanY)
- **App Theme**: Modern Neural Voice Studio
- **Package Name**: `com.auravoice.tts`

---

*AuraVoice — Engineered for Performance, Privacy, and Precision On-Device Speech.*

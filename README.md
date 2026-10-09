# ShareReady Android app

A Kotlin + Jetpack Compose offline-first text cleanup app.

## Included features
- Clean repeated spaces and excess blank lines
- Uppercase and lowercase conversion
- Remove empty lines and duplicate lines
- Trim each line
- Character and word counts
- Copy to clipboard and Android share sheet
- Undo last output transform
- Local recent-results history (stored in app preferences)
- No account, server, or internet permission required

## Build

Requirements: JDK 17, Android SDK Platform 35, Android Build Tools, and Gradle 8.11.1 or compatible.

From the project root, run:

```bash
gradle assembleDebug
```

The APK should be generated at `app/build/outputs/apk/debug/app-debug.apk`.

If using GitHub Codespaces and Gradle is already installed, open the terminal in this folder and run the command above. If your environment uses a Gradle wrapper, you can generate it with `gradle wrapper --gradle-version 8.11.1` and then run `./gradlew assembleDebug`.

## Package details
- Application ID: `com.revamine.shareready`
- Minimum Android: 7.0 (API 24)
- Target / compile SDK: 35
- Version: 1.0.0

This is a source project, not a prebuilt APK. It has not been built or tested in an Android SDK environment in this workspace. Build it once and fix any environment-specific Gradle/SDK errors if they appear.

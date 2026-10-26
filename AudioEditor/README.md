# AudioEditor

This folder contains a starter Kotlin audio editor module for the `com.muwanika.pro.audio` package.

Structure:
- `src/main/java/com/muwanika/pro/audio/AudioEditor.kt`

The editor currently includes:
- decode audio from a URI
- PCM conversion to float mono samples
- trim silence
- split and trim operations
- fade in/out
- gain and normalization
- energy curve calculation
- WAV export

This is a non-DJ, editor-focused implementation intended for audio editing workflows.

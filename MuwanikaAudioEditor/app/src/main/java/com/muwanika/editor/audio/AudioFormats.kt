package com.muwanika.editor.audio

enum class AudioFormatType(val extension: String, val label: String) {
    WAV("wav", "WAV"),
    FLAC("flac", "FLAC"),
    MP3("mp3", "MP3"),
    AAC("aac", "AAC"),
    M4A("m4a", "M4A"),
    OGG("ogg", "OGG"),
    AIFF("aiff", "AIFF");

    companion object {
        fun fromExtension(value: String): AudioFormatType? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.extension == normalized }
        }
    }
}

object SupportedAudioFormats {
    val all = listOf(
        AudioFormatType.WAV,
        AudioFormatType.FLAC,
        AudioFormatType.MP3,
        AudioFormatType.AAC,
        AudioFormatType.M4A,
        AudioFormatType.OGG,
        AudioFormatType.AIFF
    )

    fun filterMimeTypes(): List<String> = listOf(
        "audio/wav",
        "audio/x-wav",
        "audio/mpeg",
        "audio/mp4",
        "audio/aac",
        "audio/flac",
        "audio/ogg",
        "audio/x-aiff"
    )
}

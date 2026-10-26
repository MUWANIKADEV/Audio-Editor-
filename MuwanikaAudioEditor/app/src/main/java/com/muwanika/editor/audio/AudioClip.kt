package com.muwanika.editor.audio

import kotlin.math.max

data class AudioClip(
    var id: String = "",
    var name: String = "Untitled Clip",
    var sourcePath: String = "",
    var sampleRate: Int = 44100,
    var channelCount: Int = 2,
    var bpm: Float = 120f,
    var tempoMultiplier: Float = 1.0f,
    var gainDb: Float = 0f,
    var isMuted: Boolean = false,
    var startSample: Int = 0,
    var endSample: Int = 0,
    var cuePoints: MutableList<Int> = mutableListOf(),
    var markers: MutableList<Int> = mutableListOf()
) {
    fun durationSamples(): Int = max(0, endSample - startSample)

    fun durationSeconds(): Float = durationSamples() / sampleRate.toFloat()

    fun effectiveBpm(): Float = bpm * tempoMultiplier

    fun applyTempo(multiplier: Float) {
        tempoMultiplier = multiplier.coerceAtLeast(0.25f).coerceAtMost(4.0f)
    }

    fun addCuePoint(sample: Int) {
        cuePoints.add(sample.coerceAtLeast(0))
        cuePoints.sort()
    }

    fun addMarker(sample: Int) {
        markers.add(sample.coerceAtLeast(0))
        markers.sort()
    }
}

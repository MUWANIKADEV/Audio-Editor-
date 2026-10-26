package com.muwanika.editor.audio

import kotlin.math.abs
import kotlin.math.max

class BpmController {
    fun detectBpm(energyCurve: FloatArray, minBpm: Int = 60, maxBpm: Int = 200): Float {
        if (energyCurve.isEmpty()) return 120f

        val sampleCount = energyCurve.size
        var bestBpm = 120f
        var bestScore = Float.NEGATIVE_INFINITY

        val minFrames = max(1, ((60f / maxBpm.toFloat()) * sampleCount).toInt())
        val maxFrames = max(1, ((60f / minBpm.toFloat()) * sampleCount).toInt())

        for (frameDistance in minFrames..maxFrames) {
            if (frameDistance <= 0 || frameDistance >= sampleCount) continue

            var score = 0f
            for (i in 0 until sampleCount - frameDistance) {
                score += abs(energyCurve[i] - energyCurve[i + frameDistance])
            }

            if (score > bestScore) {
                bestScore = score
                bestBpm = 60f * sampleCount / frameDistance.toFloat()
            }
        }

        return bestBpm.coerceIn(minBpm.toFloat(), maxBpm.toFloat())
    }

    fun setBpm(clip: AudioClip, bpm: Float) {
        clip.bpm = bpm.coerceIn(40f, 220f)
    }

    fun syncProjectTempo(clip: AudioClip, projectBpm: Float) {
        clip.bpm = projectBpm.coerceIn(40f, 220f)
    }
}

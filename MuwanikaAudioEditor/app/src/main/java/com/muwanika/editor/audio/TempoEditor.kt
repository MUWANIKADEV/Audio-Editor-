package com.muwanika.editor.audio

import kotlin.math.pow

class TempoEditor {
    fun applyTempoChange(samples: FloatArray, multiplier: Float): FloatArray {
        val safeMultiplier = multiplier.coerceIn(0.5f, 2.0f)
        if (safeMultiplier == 1f) return samples.copyOf()

        val out = FloatArray(samples.size)
        val step = 1f / safeMultiplier
        var index = 0

        for (i in samples.indices) {
            val srcIndex = (i * step).toInt()
            if (srcIndex < samples.size) {
                out[index] = samples[srcIndex]
                index++
            }
        }

        return out.copyOfRange(0, index)
    }

    fun pitchShift(samples: FloatArray, semitones: Float): FloatArray {
        val ratio = 2.0.pow((semitones / 12.0)).toFloat()
        return applyTempoChange(samples, ratio)
    }

    fun tempoPercent(multiplier: Float): Float = (multiplier - 1f) * 100f
}

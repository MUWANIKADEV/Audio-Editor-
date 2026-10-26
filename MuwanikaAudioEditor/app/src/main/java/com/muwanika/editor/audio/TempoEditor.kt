package com.muwanika.editor.audio

import kotlin.math.abs

class TempoEditor {
    fun setTempoPercent(clip: AudioClip, percent: Float) {
        val normalized = percent.coerceIn(-75f, 200f)
        clip.tempoMultiplier = 1f + (normalized / 100f)
    }

    fun tempoPercentFromMultiplier(multiplier: Float): Float = (multiplier - 1f) * 100f

    fun applyTempoChange(samples: FloatArray, multiplier: Float): FloatArray {
        val safeMultiplier = multiplier.coerceAtLeast(0.25f).coerceAtMost(4.0f)
        val out = FloatArray(samples.size)

        if (safeMultiplier == 1f) return samples.copyOf()

        val step = 1f / safeMultiplier
        var targetIndex = 0

        for (i in samples.indices) {
            val sourceIndex = (i * step).toInt()
            if (sourceIndex < samples.size) {
                out[targetIndex] = samples[sourceIndex]
                targetIndex++
            }
        }

        return out.copyOfRange(0, targetIndex)
    }

    fun timeStretch(samples: FloatArray, amount: Float): FloatArray {
        val ratio = amount.coerceAtLeast(0.5f).coerceAtMost(2.0f)
        val output = FloatArray((samples.size / ratio).toInt().coerceAtLeast(1))

        for (i in output.indices) {
            val srcIndex = (i * ratio).toInt()
            output[i] = if (srcIndex < samples.size) samples[srcIndex] else 0f
        }

        return output
    }

    fun pitchShift(samples: FloatArray, semitones: Float): FloatArray {
        val shift = semitones.coerceIn(-12f, 12f)
        val ratio = Math.pow(2.0, (shift / 12.0)).toFloat()
        return applyTempoChange(samples, ratio)
    }

    fun beatSnap(sample: Int, gridSize: Int): Int {
        if (gridSize <= 0) return sample
        return ((sample + gridSize / 2) / gridSize) * gridSize
    }
}

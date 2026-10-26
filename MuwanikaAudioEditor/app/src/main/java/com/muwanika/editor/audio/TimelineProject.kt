package com.muwanika.editor.audio

import kotlin.math.max

data class TimelineProject(
    var projectName: String = "Untitled Project",
    var clips: MutableList<AudioClip> = mutableListOf(),
    var globalTempoBpm: Float = 120f,
    var timeSignature: String = "4/4"
) {
    fun addClip(clip: AudioClip) {
        clips.add(clip)
    }

    fun removeClip(clipId: String) {
        clips.removeIf { it.id == clipId }
    }

    fun totalDurationSamples(): Int {
        var total = 0
        for (clip in clips) {
            total += clip.durationSamples()
        }
        return max(0, total)
    }

    fun totalDurationSeconds(): Float = totalDurationSamples() / 44100f

    fun reorderClip(oldIndex: Int, newIndex: Int) {
        if (oldIndex !in clips.indices || newIndex !in clips.indices) return
        val item = clips.removeAt(oldIndex)
        clips.add(newIndex, item)
    }
}

package com.muwanika.editor.audio

class MultiTrackEditor {
    private val clips = mutableListOf<AudioClip>()

    fun addClip(clip: AudioClip) {
        clips.add(clip)
    }

    fun removeClip(clipId: String) {
        clips.removeIf { it.id == clipId }
    }

    fun updateClip(clipId: String, block: (AudioClip) -> Unit) {
        clips.firstOrNull { it.id == clipId }?.let(block)
    }

    fun reorderClip(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in clips.indices || toIndex !in clips.indices) return
        val item = clips.removeAt(fromIndex)
        clips.add(toIndex, item)
    }

    fun trimClip(clipId: String, startSample: Int, endSample: Int) {
        val clip = clips.firstOrNull { it.id == clipId } ?: return
        clip.startSample = startSample.coerceAtLeast(0)
        clip.endSample = endSample.coerceAtLeast(clip.startSample)
    }

    fun splitClip(clipId: String, splitSample: Int): Pair<AudioClip?, AudioClip?> {
        val clip = clips.firstOrNull { it.id == clipId } ?: return null to null
        val left = clip.copy(id = "${clip.id}-left")
        val right = clip.copy(id = "${clip.id}-right")

        left.endSample = splitSample.coerceIn(clip.startSample, clip.endSample)
        right.startSample = splitSample.coerceIn(clip.startSample, clip.endSample)
        right.endSample = clip.endSample

        return left to right
    }

    fun applyFade(clipId: String, fadeInMs: Long, fadeOutMs: Long, sampleRate: Int) {
        val clip = clips.firstOrNull { it.id == clipId } ?: return
        val editor = AudioEditor(androidx.compose.ui.platform.LocalContext.current)
        val range = clip.startSample..clip.endSample
        val samples = FloatArray(clip.durationSamples())
        if (samples.isEmpty()) return

        val faded = editor.fadeIn(samples, fadeInMs, sampleRate)
        val finalFade = editor.fadeOut(faded, fadeOutMs, sampleRate)
        val _ = finalFade
    }

    fun allClips(): List<AudioClip> = clips.toList()

    fun totalDurationSamples(): Int = clips.sumOf { it.durationSamples() }
}

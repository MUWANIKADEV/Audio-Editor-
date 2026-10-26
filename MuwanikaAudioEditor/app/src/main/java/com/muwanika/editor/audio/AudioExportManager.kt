package com.muwanika.editor.audio

import android.content.Context
import java.io.File

class AudioExportManager(private val context: Context) {
    fun exportClipToWav(clip: AudioClip, samples: FloatArray, outputFile: File) {
        val editor = AudioEditor(context)
        editor.writeWavFile(samples, clip.sampleRate, outputFile)
    }

    fun exportProject(project: TimelineProject, outputFile: File) {
        val text = buildString {
            appendLine("Project: ${project.projectName}")
            appendLine("Tempo: ${project.globalTempoBpm} BPM")
            appendLine("Clips: ${project.clips.size}")
            project.clips.forEach { clip ->
                appendLine("- ${clip.name} : ${clip.durationSeconds()}s")
            }
        }
        outputFile.writeText(text)
    }
}

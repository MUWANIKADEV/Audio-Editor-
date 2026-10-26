package com.muwanika.editor.audio

import android.content.Context
import java.io.File

class AudioExportManager(private val context: Context) {
    fun exportWav(samples: FloatArray, sampleRate: Int, outputFile: File) {
        val editor = AudioEditor(context)
        editor.writeWavFile(samples, sampleRate, outputFile)
    }

    fun exportMp3(samples: FloatArray, sampleRate: Int, outputFile: File) {
        // Placeholder for real MP3 export.
        // In production, use LAME or MediaMuxer-based encoder approach.
        val editor = AudioEditor(context)
        editor.writeWavFile(samples, sampleRate, outputFile)
    }

    fun exportAac(samples: FloatArray, sampleRate: Int, outputFile: File) {
        // Placeholder for AAC export.
        val editor = AudioEditor(context)
        editor.writeWavFile(samples, sampleRate, outputFile)
    }
}

package com.muwanika.editor.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.muwanika.editor.audio.AudioEditor
import com.muwanika.editor.audio.AudioPCM
import java.io.File

@Composable
fun AudioEditorScreen() {
    val context = LocalContext.current
    val editor = remember { AudioEditor(context) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var audioPcm by remember { mutableStateOf<AudioPCM?>(null) }
    var energyCurve by remember { mutableStateOf<FloatArray?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            isLoading = true

            try {
                val pcm = editor.readPcmFromUri(uri)
                audioPcm = pcm
                energyCurve = editor.computeEnergyCurve(pcm.samples, frameSize = 2048)
                Log.d("AudioEditor", "Loaded: ${pcm.samples.size} samples")
            } catch (e: Exception) {
                Log.e("AudioEditor", "Load failed", e)
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { launcher.launch("audio/*") },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Open audio file")
        }

        if (audioPcm != null) {
            Text("Sample rate: ${audioPcm!!.sampleRate}")
            Text("Channels: ${audioPcm!!.channelCount}")
            Text("Samples: ${audioPcm!!.samples.size}")
        }

        if (energyCurve != null) {
            Text("Energy points: ${energyCurve!!.size}")
        }

        Button(
            onClick = {
                val pcm = audioPcm ?: return@Button
                val trimRange = editor.trimSilence(pcm.samples, threshold = 0.01f, paddingSamples = 1000)
                val trimmed = editor.trim(pcm.samples, trimRange.startSample, trimRange.endSample)
                val faded = editor.fadeIn(trimmed, 200, pcm.sampleRate)
                val normalized = editor.normalize(faded)

                val outputFile = File(context.getExternalFilesDir(null), "muwanika_audio_editor.wav")
                editor.writeWavFile(normalized, pcm.sampleRate, outputFile)
            }
        ) {
            Text("Trim + Fade + Export")
        }
    }
}

package com.muwanika.editor.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.muwanika.editor.audio.AudioEditor
import com.muwanika.editor.audio.AudioPCM
import java.io.File

@Composable
fun ResponsiveEditorScreen() {
    val context = LocalContext.current
    val editor = remember { AudioEditor(context) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var audioPcm by remember { mutableStateOf<AudioPCM?>(null) }
    var energyCurve by remember { mutableStateOf<FloatArray?>(null) }
    var bpm by remember { mutableFloatStateOf(120f) }
    var gainDb by remember { mutableFloatStateOf(0f) }
    var fadeInMs by remember { mutableFloatStateOf(150f) }
    var fadeOutMs by remember { mutableFloatStateOf(150f) }
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
                val trimmedRange = editor.trimSilence(pcm.samples, threshold = 0.01f, paddingSamples = 2000)
                Log.d("ResponsiveEditor", "Loaded pcm: ${pcm.samples.size}, trimmed=(${trimmedRange.startSample}, ${trimmedRange.endSample})")
            } catch (e: Exception) {
                Log.e("ResponsiveEditor", "Failed to load audio", e)
            } finally {
                isLoading = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101720))
            .padding(16.dp)
    ) {
        if (maxWidth < 600.dp) {
            PortraitEditorLayout(
                audioPcm = audioPcm,
                bpm = bpm,
                gainDb = gainDb,
                fadeInMs = fadeInMs,
                fadeOutMs = fadeOutMs,
                energyCurve = energyCurve,
                isLoading = isLoading,
                onOpenAudio = { launcher.launch("audio/*") },
                onBpmChange = { bpm = it },
                onGainChange = { gainDb = it },
                onFadeInChange = { fadeInMs = it },
                onFadeOutChange = { fadeOutMs = it },
                onExport = {
                    val pcm = audioPcm ?: return@PortraitEditorLayout
                    val trimmedRange = editor.trimSilence(pcm.samples, threshold = 0.01f, paddingSamples = 1000)
                    val trimmed = editor.trim(pcm.samples, trimmedRange.startSample, trimmedRange.endSample)
                    val faded = editor.fadeIn(trimmed, fadeInMs.toLong(), pcm.sampleRate)
                    val fadeOut = editor.fadeOut(faded, fadeOutMs.toLong(), pcm.sampleRate)
                    val outputFile = File(context.getExternalFilesDir(null), "muwanika_audio_editor.wav")
                    editor.writeWavFile(editor.normalize(fadeOut), pcm.sampleRate, outputFile)
                }
            )
        } else {
            LandscapeEditorLayout(
                audioPcm = audioPcm,
                bpm = bpm,
                gainDb = gainDb,
                fadeInMs = fadeInMs,
                fadeOutMs = fadeOutMs,
                energyCurve = energyCurve,
                isLoading = isLoading,
                onOpenAudio = { launcher.launch("audio/*") },
                onBpmChange = { bpm = it },
                onGainChange = { gainDb = it },
                onFadeInChange = { fadeInMs = it },
                onFadeOutChange = { fadeOutMs = it },
                onExport = {
                    val pcm = audioPcm ?: return@LandscapeEditorLayout
                    val trimmedRange = editor.trimSilence(pcm.samples, threshold = 0.01f, paddingSamples = 1000)
                    val trimmed = editor.trim(pcm.samples, trimmedRange.startSample, trimmedRange.endSample)
                    val faded = editor.fadeIn(trimmed, fadeInMs.toLong(), pcm.sampleRate)
                    val fadeOut = editor.fadeOut(faded, fadeOutMs.toLong(), pcm.sampleRate)
                    val outputFile = File(context.getExternalFilesDir(null), "muwanika_audio_editor.wav")
                    editor.writeWavFile(editor.normalize(fadeOut), pcm.sampleRate, outputFile)
                }
            )
        }
    }
}

@Composable
private fun PortraitEditorLayout(
    audioPcm: AudioPCM?,
    bpm: Float,
    gainDb: Float,
    fadeInMs: Float,
    fadeOutMs: Float,
    energyCurve: FloatArray?,
    isLoading: Boolean,
    onOpenAudio: () -> Unit,
    onBpmChange: (Float) -> Unit,
    onGainChange: (Float) -> Unit,
    onFadeInChange: (Float) -> Unit,
    onFadeOutChange: (Float) -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopHeader(title = "MUWANIKA AUDIO EDITOR")

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF182533)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onOpenAudio, enabled = !isLoading) {
                    Text(if (isLoading) "Loading..." else "Open audio")
                }

                if (audioPcm != null) {
                    Text("Sample rate: ${audioPcm.sampleRate} Hz")
                    Text("Channels: ${audioPcm.channelCount}")
                    Text("Samples: ${audioPcm.samples.size}")
                } else {
                    Text("No file loaded")
                }
            }
        }

        WaveformPreview(energyCurve = energyCurve)

        EditorControlSlider(
            label = "BPM",
            value = bpm,
            range = 60f..200f,
            onValueChange = onBpmChange
        )

        EditorControlSlider(
            label = "Gain",
            value = gainDb,
            range = -24f..24f,
            onValueChange = onGainChange
        )

        EditorControlSlider(
            label = "Fade In",
            value = fadeInMs,
            range = 0f..2000f,
            onValueChange = onFadeInChange
        )

        EditorControlSlider(
            label = "Fade Out",
            value = fadeOutMs,
            range = 0f..2000f,
            onValueChange = onFadeOutChange
        )

        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Export WAV")
        }
    }
}

@Composable
private fun LandscapeEditorLayout(
    audioPcm: AudioPCM?,
    bpm: Float,
    gainDb: Float,
    fadeInMs: Float,
    fadeOutMs: Float,
    energyCurve: FloatArray?,
    isLoading: Boolean,
    onOpenAudio: () -> Unit,
    onBpmChange: (Float) -> Unit,
    onGainChange: (Float) -> Unit,
    onFadeInChange: (Float) -> Unit,
    onFadeOutChange: (Float) -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopHeader(title = "MUWANIKA AUDIO EDITOR")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF182533)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onOpenAudio, enabled = !isLoading) {
                        Text(if (isLoading) "Loading..." else "Open audio")
                    }

                    if (audioPcm != null) {
                        Text("Sample rate: ${audioPcm.sampleRate} Hz")
                        Text("Channels: ${audioPcm.channelCount}")
                        Text("Samples: ${audioPcm.samples.size}")
                    } else {
                        Text("No file loaded")
                    }
                }
            }

            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export WAV")
            }
        }

        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WaveformPreview(energyCurve = energyCurve)

            EditorControlSlider(
                label = "BPM",
                value = bpm,
                range = 60f..200f,
                onValueChange = onBpmChange
            )

            EditorControlSlider(
                label = "Gain",
                value = gainDb,
                range = -24f..24f,
                onValueChange = onGainChange
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorControlSlider(
                    label = "Fade In",
                    value = fadeInMs,
                    range = 0f..2000f,
                    onValueChange = onFadeInChange,
                    modifier = Modifier.weight(1f)
                )

                EditorControlSlider(
                    label = "Fade Out",
                    value = fadeOutMs,
                    range = 0f..2000f,
                    onValueChange = onFadeOutChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TopHeader(title: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F3F)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
            color = Color.White
        )
    }
}

@Composable
private fun WaveformPreview(energyCurve: FloatArray?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101720)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (energyCurve != null) "Waveform / Energy preview" else "Load an audio file to preview waveform",
                color = Color(0xFFEAF3FF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (energyCurve != null) {
                val points = energyCurve
                val step = (points.size / 240f).coerceAtLeast(1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (i in 0 until 240) {
                        val index = (i * step).toInt().coerceAtMost(points.size - 1)
                        val value = points[index]
                        val height = (value * 100).toInt().coerceAtLeast(4)
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(height.dp)
                                .background(Color(0xFF4FC3F7))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorControlSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF182533)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$label: ${value.toInt()}",
                color = Color.White
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range
            )
        }
    }
}

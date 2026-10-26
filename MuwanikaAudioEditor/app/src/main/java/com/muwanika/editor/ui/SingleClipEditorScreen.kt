package com.muwanika.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muwanika.editor.audio.AudioClip
import com.muwanika.editor.audio.BpmController
import com.muwanika.editor.audio.TempoEditor

@Composable
fun SingleClipEditorScreen() {
    val bpmController = remember { BpmController() }
    val tempoEditor = remember { TempoEditor() }
    val clipState = remember { mutableStateOf(AudioClip(id = "clip-1", name = "Single clip")) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Single Clip Editor")
        Text("Name: ${clipState.value.name}")
        Text("BPM: ${clipState.value.bpm}")
        Text("Tempo multiplier: ${clipState.value.tempoMultiplier}")

        Button(onClick = { clipState.value = clipState.value.copy(gainDb = clipState.value.gainDb + 3f) }) {
            Text("Increase gain +3dB")
        }

        Button(onClick = {
            clipState.value = clipState.value.copy(bpm = 128f)
            bpmController.setBpm(clipState.value, 128f)
        }) {
            Text("Set BPM to 128")
        }

        Button(onClick = {
            tempoEditor.setTempoPercent(clipState.value, 10f)
            clipState.value = clipState.value.copy(tempoMultiplier = clipState.value.tempoMultiplier)
        }) {
            Text("Apply +10% tempo")
        }
    }
}

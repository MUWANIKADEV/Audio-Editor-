package com.muwanika.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muwanika.editor.audio.BpmController

@Composable
fun BpmEditorScreen() {
    val controller = remember { BpmController() }
    var bpm by remember { mutableFloatStateOf(120f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BPM and Tempo Editor")
        Text("Current BPM: ${bpm}")

        Slider(
            value = bpm,
            onValueChange = { bpm = it },
            valueRange = 60f..200f
        )

        Button(onClick = {
            val estimate = controller.detectBpm(floatArrayOf(0.2f, 0.6f, 0.8f, 0.5f, 0.7f, 0.9f, 0.4f), 60, 200)
            bpm = estimate
        }) {
            Text("Auto-detect BPM")
        }
    }
}

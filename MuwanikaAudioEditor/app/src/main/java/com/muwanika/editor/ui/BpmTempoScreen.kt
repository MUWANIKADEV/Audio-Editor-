package com.muwanika.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muwanika.editor.audio.BpmController
import com.muwanika.editor.audio.TempoEditor

@Composable
fun BpmTempoScreen() {
    val bpmController = remember { BpmController() }
    val tempoEditor = remember { TempoEditor() }

    var bpm by remember { mutableFloatStateOf(120f) }
    var tempoPercent by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("BPM / Tempo Editor", color = Color.White)

                Text("Current BPM: ${bpm.toInt()}", color = Color.White)
                Slider(
                    value = bpm,
                    onValueChange = { bpm = it },
                    valueRange = 60f..200f
                )

                Text("Tempo shift: ${tempoPercent.toInt()}%", color = Color.White)
                Slider(
                    value = tempoPercent,
                    onValueChange = { tempoPercent = it },
                    valueRange = -50f..100f
                )
            }
        }

        Button(onClick = {
            val energy = floatArrayOf(0.2f, 0.6f, 0.8f, 0.4f, 0.7f, 0.9f, 0.5f)
            bpm = bpmController.detectBpm(energy, 60, 200)
        }) {
            Text("Auto-detect BPM")
        }

        Button(onClick = {
            val multiplier = 1f + (tempoPercent / 100f)
            val adjusted = tempoEditor.applyTempoChange(
                floatArrayOf(0.1f, 0.2f, 0.7f, 0.8f, 0.9f, 0.6f, 0.4f),
                multiplier
            )
            Text("Adjusted samples: ${adjusted.size}")
        }) {
            Text("Apply tempo change")
        }
    }
}

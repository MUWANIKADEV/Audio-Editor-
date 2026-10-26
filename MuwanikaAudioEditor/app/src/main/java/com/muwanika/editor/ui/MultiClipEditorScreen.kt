package com.muwanika.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muwanika.editor.audio.AudioClip
import com.muwanika.editor.audio.MultiTrackEditor

@Composable
fun MultiClipEditorScreen() {
    val multiTrackEditor = remember { MultiTrackEditor() }
    val clips = remember { mutableStateListOf<AudioClip>() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Multi-Clip Timeline")

        Button(onClick = {
            val newClip = AudioClip(
                id = "clip-${clips.size + 1}",
                name = "Track ${clips.size + 1}",
                bpm = 120f,
                tempoMultiplier = 1f
            )
            clips.add(newClip)
            multiTrackEditor.addClip(newClip)
        }) {
            Text("Add clip")
        }

        clips.forEach { clip ->
            Text("- ${clip.name} | BPM ${clip.bpm} | Gain ${clip.gainDb}dB")
        }
    }
}

package com.example.soundscapejourney.screens.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.soundscapejourney.R
import com.example.soundscapejourney.data.models.RemixTrack
import com.example.soundscapejourney.viewmodels.RemixViewModel

@Composable
fun SaveRemixDialog (
    remixViewModel: RemixViewModel,
    remixTracks: List<RemixTrack>,
    onShowSaveDialog: (Boolean) -> Unit
) {
    var remixName by remember { mutableStateOf("") }

    AlertDialog(
    onDismissRequest = { onShowSaveDialog(false) },
    title = { Text(stringResource(R.string.save_mix), color = MaterialTheme.colorScheme.onSurface) },
    text = {
        OutlinedTextField(
            value = remixName,
            onValueChange = { remixName = it },
            placeholder = { Text(stringResource(R.string.enter_mix_name_form)) },
            singleLine = true
        )
    },
    confirmButton = {
        Button (
            onClick = {
                if (remixName.isNotBlank()) {
                    remixViewModel.saveRemix(
                        remixName = remixName,
                        currentTracks = remixTracks
                    )
                    onShowSaveDialog(false)
                }
            }
        ) {
            Text(stringResource(R.string.ok_form))
        }
    },
    dismissButton = {
        TextButton (onClick = { onShowSaveDialog(false) }) {
            Text(stringResource(R.string.cancel_form))
        }
    },
    containerColor = MaterialTheme.colorScheme.surface
    )
}
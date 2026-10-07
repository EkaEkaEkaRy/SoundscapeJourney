package com.example.soundscapejourney.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundscapejourney.data.models.RemixTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.soundscapejourney.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemixScreen(
    tracks: List<RemixTrack>,
    isRemixPlaying: Boolean,
    isSaving: Boolean,
    isRemixSaved: Boolean,
    onVolumeChange: (String, Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSaveRemix: () -> Unit,
    onRemoveClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.my_remix_header),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Список треков с ползунками громкости
            if (isSaving)
                Box(Modifier.fillMaxSize()) {
                    Column (Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Сохранение микса",
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            else if (tracks.isEmpty())
                Box(Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.no_sounds_added),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            else
                LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tracks) { track ->
                    // Карточка одного звука в ремиксе
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row (modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically) {
                            Column(Modifier
                                .weight(1f)
                                .padding(end = 8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween,
                                    Alignment.Bottom
                                ) {
                                    Text(
                                        text = track.sound.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(Modifier.width(30.dp))

                                    Text(
                                        text = "${(track.volume * 100).toInt()} %",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                // Слайдер громкости
                                Slider(
                                    value = track.volume,
                                    onValueChange = { newVolume ->
                                        onVolumeChange(
                                            track.sound.id,
                                            newVolume
                                        )
                                    },
                                    valueRange = 0f..1f,
                                    track = { sliderState ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondary.copy(
                                                        alpha = 0.24f),
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = sliderState.value)
                                                    .fillMaxHeight()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    },
                                    thumb = {
                                        SliderDefaults.Thumb(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                            thumbSize = DpSize(5.dp, 15.dp),
                                            colors = SliderColors(
                                                thumbColor = MaterialTheme.colorScheme.secondary,
                                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                                activeTickColor = MaterialTheme.colorScheme.secondary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.secondary,
                                                inactiveTickColor = MaterialTheme.colorScheme.secondary,
                                                disabledThumbColor = MaterialTheme.colorScheme.secondary,
                                                disabledActiveTrackColor = MaterialTheme.colorScheme.secondary,
                                                disabledActiveTickColor = MaterialTheme.colorScheme.secondary,
                                                disabledInactiveTrackColor = MaterialTheme.colorScheme.secondary,
                                                disabledInactiveTickColor = MaterialTheme.colorScheme.secondary
                                            ),
                                        )
                                    }
                                )


                            }

                            var isMenuExpanded by remember { mutableStateOf(false) }

                            IconButton(onClick = { isMenuExpanded = true },
                                modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Опции",
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.remove_from_mix), color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        isMenuExpanded = false
                                        onRemoveClick(track.sound.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.padding(bottom = 8.dp))

            if (!tracks.isEmpty() || isSaving)
                Row (Modifier.fillMaxWidth(),
                Arrangement.Center,
                Alignment.Bottom) {
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isRemixPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Воспроизвести",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.width(30.dp))

                Button(
                    onClick = onSaveRemix,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isRemixPlaying) 1f else 0.75f)), // Фиолетовая кнопка
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (isRemixSaved) stringResource(R.string.save_mix_button)
                        else stringResource(R.string.delete_mix_button),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }


        }
    }
}

package com.example.soundscapejourney.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.soundscapejourney.data.database.entities.RemixWithSounds
import com.example.soundscapejourney.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    remixWithSounds: RemixWithSounds,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onVolumeChange: (String, Float) -> Unit,
    onDeleteMixClick: (RemixWithSounds) -> Unit
) {
    var showVolumeList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Верхняя панель: Стрелочка вниз для закрытия
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Row (Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Свернуть",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                var isMenuExpanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.align(Alignment.Top).padding(4.dp)) {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Опции микса",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.remove_mix),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteMixClick(remixWithSounds)
                            }
                        )
                    }
                }
            }
        }

        // Обложка или Список ползунков
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = showVolumeList,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "PlayerContentAnimation"
            ) { isListVisible ->
                if (!isListVisible) {
                    Card(
                        modifier = Modifier
                            .size(280.dp)
                            .clickable { showVolumeList = true },
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/${remixWithSounds.remix.imagePath}",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // кликабельный фон для возврата назад к обложке
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showVolumeList = false }
                    ) {
                        items(remixWithSounds.sounds) { sound ->
                            // var currentVolume by remember(sound.volume) { mutableFloatStateOf(sound.volume) }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier
                                    .padding(16.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        Arrangement.SpaceBetween,
                                        Alignment.Bottom
                                    ) {
                                        Text(
                                            text = sound.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(Modifier.width(30.dp))

                                        Text(
                                            text = "${(sound.volume * 100).toInt()} %",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Слайдер громкости
                                    Slider(
                                        value = sound.volume,
                                        onValueChange = {
                                            // currentVolume = it
                                            onVolumeChange(sound.filePath, it)
                                        },
                                        valueRange = 0f..1f,
                                        track = { sliderState ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(7.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondary.copy(
                                                            alpha = 0.24f
                                                        ),
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
                            }
                        }
                    }
                }
            }
        }

        // Названия ремикса
        Text(
            text = remixWithSounds.remix.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

//        Text(
//            text = "Локальный ремикс • Офлайн",
//            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
//            fontSize = 14.sp,
//            modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
//        )

        // 3 кнопки внизу
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка: Предыдущий трек
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_previous),
                    contentDescription = "Предыдущий",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Кнопка: Плей / Пауза
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(36.dp)
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Плей Пауза",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Кнопка: Следующий трек
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_next),
                    contentDescription = "Следующий",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

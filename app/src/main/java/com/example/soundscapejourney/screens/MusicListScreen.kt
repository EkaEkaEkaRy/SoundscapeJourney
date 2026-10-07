package com.example.soundscapejourney.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.soundscapejourney.R
import com.example.soundscapejourney.data.models.Sound


@Composable
fun HomeHeader(
    selectedCategory: String,
    searchQuery: String,
    isSearchVisible: Boolean,
    onCategoryClick: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearchClick: () -> Unit
) {
    val categories = listOf("Rain", "Forest", "Ocean", "Space", "Cafe", "White noise")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        // Верхний ряд: Текст + Поиск
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.main_greetings),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(10.dp))

            IconButton(
                onClick = onToggleSearchClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Поиск",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Выезжающая поисковая строка с анимацией
        AnimatedVisibility (visible = isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.searching_sound_hint), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(50.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Нижний ряд: Горизонтальный скролл категорий
        LazyRow (
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.size) { i ->
                val isSelected = categories[i] == selectedCategory

                // Кастомный чипс
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        .clickable { onCategoryClick(categories[i]) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categories[i],
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

    }
}

@Composable
fun MusicList(sounds: List<Sound>, playingSoundId: String?, bufferingSoundId: String?,
              onSoundPlayClick: (Sound) -> Unit, onAddToRemixClick: (Sound) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(sounds) { sound ->
            MusicCard(sound = sound, isPlaying = sound.id == playingSoundId,
                isBuffering = sound.id == bufferingSoundId,
                onPlayClick = { onSoundPlayClick(sound) },
                onAddToRemixClick = { onAddToRemixClick(sound) })
        }
    }
}

@Composable
fun MusicCard(sound: Sound,
              isPlaying: Boolean,
              isBuffering: Boolean,
              onPlayClick: () -> Unit,
              onAddToRemixClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Квадратный блок для обложки и элементов управления поверх неё
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = sound.imageUrl,
                    contentDescription = sound.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Кнопка Play строго по центру
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), shape = CircleShape)
                        .align(Alignment.Center)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Воспроизвести",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                }
            }

            Text(
                text = sound.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )

            // Блок с текстом и сердечком под картинкой
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val tags = if (sound.tags.size > 5) sound.tags.subList(0, 5) else sound.tags

                    // теги с решеткой
                    val tagsString = tags.joinToString(" ") { "#$it" }
                    Text(
                        text = tagsString,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 12.sp,
                        lineHeight = 13.sp
                    )
                }

                Spacer(Modifier.width(5.dp))

                // Кнопка-сердечко
                IconButton(
                    onClick = onAddToRemixClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = MaterialTheme.colorScheme.onSecondary)
                }

            }

            Text(text = sound.duration,
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth().align(Alignment.End)
                    .padding(top = 1.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
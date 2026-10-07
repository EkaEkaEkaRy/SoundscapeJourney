package com.example.soundscapejourney

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.soundscapejourney.ui.theme.SoundscapeJourneyTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundscapejourney.screens.components.SoundBottomSheetContent
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.soundscapejourney.components.FileDownloader
import com.example.soundscapejourney.data.database.AppDatabase
import com.example.soundscapejourney.screens.HomeHeader
import com.example.soundscapejourney.screens.MusicList
import com.example.soundscapejourney.screens.PlayerScreen
import com.example.soundscapejourney.screens.ProfileScreen
import com.example.soundscapejourney.screens.RemixScreen
import com.example.soundscapejourney.screens.components.MiniPlayer
import com.example.soundscapejourney.screens.components.SaveRemixDialog
import com.example.soundscapejourney.viewmodels.MainViewModel
import com.example.soundscapejourney.viewmodels.RemixViewModel
import com.example.soundscapejourney.viewmodels.SettingsViewModel
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
            val prefsIsDark = prefs.getBoolean("is_dark_theme", true)
            val prefsLanguage = prefs.getString("language", "ru") ?: "ru"

            val settingsViewModel: SettingsViewModel = viewModel()
            settingsViewModel.toggleTheme(prefsIsDark)
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsStateWithLifecycle()

            settingsViewModel.setLanguage(prefsLanguage)
            val language by settingsViewModel.language.collectAsStateWithLifecycle()

            SoundscapeJourneyTheme(darkTheme = isDarkTheme) {
                SoundscapeJourneyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun SoundscapeJourneyApp() {
    val context = LocalContext.current

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // Инициализируем ViewModel
    val mainViewModel: MainViewModel = viewModel()
    // Подписываемся на состояние сети из ViewModel
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()

    val selectedSoundForSheet by mainViewModel.selectedSoundForSheet.collectAsStateWithLifecycle()

    val textPrimaryColor = MaterialTheme.colorScheme.secondary
    val textSecondaryColor = MaterialTheme.colorScheme.onSurface

    val myNavigationSuiteItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = textPrimaryColor,
            selectedTextColor = textPrimaryColor,
            unselectedIconColor = textSecondaryColor,
            unselectedTextColor = textSecondaryColor,
            disabledIconColor = textSecondaryColor,
            disabledTextColor = textSecondaryColor,
        ),
    )

    // получаем доступ к самой базе данных и загрузчику файлов
    val appContext = context.applicationContext
    val database = AppDatabase.getDatabase(context)
    val fileDownloader = FileDownloader(context)

    val remixViewModel: RemixViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Передаем в конструктор все три обязательных параметра
                return RemixViewModel(
                    remixDao = database.remixDao(),
                    fileDownloader = fileDownloader,
                    context = appContext
                ) as T
            }
        }
    )

    // Подписываемся на глобальные стейты плеера из RemixViewModel
    val currentPlayingRemix by remixViewModel.currentPlayingRemix.collectAsStateWithLifecycle()
    val isMusicPlaying by remixViewModel.isMusicPlaying.collectAsStateWithLifecycle()

    // Стейт контроля: развернут ли сейчас плеер на весь экран (true) или свернут (false)
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }


    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = stringResource(it.label)
                        )
                    },
                    label = { Text(stringResource(it.label)) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it
                                isPlayerExpanded = false },
                    colors = myNavigationSuiteItemColors
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface)
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(color = MaterialTheme.colorScheme.background)
            ) {

                Column (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (!isPlayerExpanded && currentPlayingRemix != null) 64.dp else 0.dp)
                ) {
                    when (currentDestination) {
                        AppDestinations.HOME -> {
                            val selectedCategory by mainViewModel.selectedCategory.collectAsStateWithLifecycle()
                            val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
                            val isSearchVisible by mainViewModel.isSearchVisible.collectAsStateWithLifecycle()

                            Column {
                                HomeHeader(
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    isSearchVisible = isSearchVisible,
                                    onCategoryClick = { mainViewModel.onCategorySelect(it) },
                                    onSearchQueryChange = { mainViewModel.onSearchQueryChanged(it) },
                                    onToggleSearchClick = { mainViewModel.toggleSearchVisibility() }
                                )


                                // обрабатываем состояния MVI для главного экрана
                                when (val currentState = state) {
                                    is MainUiState.Loading -> {
                                        Box(Modifier.fillMaxSize()) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.Center
                                                )
                                            )
                                        }
                                    }

                                    is MainUiState.Success -> {
                                        val playingSoundId by mainViewModel.playingSoundId.collectAsStateWithLifecycle()
                                        val bufferingSoundId by mainViewModel.bufferingSoundId.collectAsStateWithLifecycle()


                                        if (currentState.sounds.isEmpty()) {
                                            Box(Modifier.fillMaxSize()) {
                                                Text(
                                                    text = stringResource(R.string.no_sounds_data),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                        } else {
                                            // Передаем список звуков
                                            MusicList(
                                                sounds = currentState.sounds,
                                                playingSoundId = playingSoundId,
                                                bufferingSoundId = bufferingSoundId,
                                                onSoundPlayClick = { sound ->
                                                    mainViewModel.togglePlayPause(
                                                        sound
                                                    )
                                                },
                                                onAddToRemixClick = { sound ->
                                                    mainViewModel.showBottomSheet(
                                                        sound
                                                    )
                                                })
                                        }
                                    }

                                    is MainUiState.Error -> {
                                        Box(Modifier.fillMaxSize()) {
                                            Text(
                                                text = stringResource(
                                                    id = R.string.error_type,
                                                    currentState.message
                                                ),
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AppDestinations.FAVORITES -> {
                            val remixTracks by mainViewModel.remixTracks.collectAsStateWithLifecycle()
                            val isRemixPlaying by mainViewModel.isRemixPlaying.collectAsStateWithLifecycle()
                            val isSaving by remixViewModel.isSaving.collectAsStateWithLifecycle()

                            var showSaveDialog by remember { mutableStateOf(false) }

                            RemixScreen(
                                tracks = remixTracks,
                                isRemixPlaying = isRemixPlaying,
                                isSaving = isSaving,
                                isRemixSaved = true,
                                onVolumeChange = { id, vol ->
                                    mainViewModel.changeTrackVolume(
                                        id,
                                        vol
                                    )
                                },
                                onPlayPauseClick = { mainViewModel.toggleRemixPlayPause() },
                                onSaveRemix = { showSaveDialog = true },
                                onRemoveClick = { id -> mainViewModel.removeSoundFromRemix(id) }
                            )

                            // Всплывающее диалоговое окно для ввода имени ремикса
                            if (showSaveDialog) {

                                SaveRemixDialog(
                                    remixViewModel = remixViewModel,
                                    remixTracks = remixTracks,
                                    onShowSaveDialog = { showSaveDialog = it }
                                )

                            }


                        }

                        AppDestinations.PROFILE -> {
                            val prefs =
                                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

                            val settingsViewModel: SettingsViewModel = viewModel()

                            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsStateWithLifecycle()
                            val currentLanguage by settingsViewModel.language.collectAsStateWithLifecycle()

                            // Собираем список всех сохраненных миксов
                            val savedMixes by remixViewModel.allRemixes.collectAsStateWithLifecycle(
                                initialValue = emptyList()
                            )

                            ProfileScreen(
                                    savedMixes = savedMixes,
                                    isDarkTheme = isDarkTheme,
                                    currentLanguage = currentLanguage,
                                    onThemeToggle = {
                                        settingsViewModel.toggleTheme(it)
                                        prefs.edit { putBoolean("is_dark_theme", it) }
                                    },
                                    onLanguageChange = {
                                        settingsViewModel.setLanguage(it)
                                        prefs.edit { putString("language", it) }
                                    },
                                    onFavoritesClick = { /* Переход в избранное */ },
                                    onMixClick = { chosenMix ->
                                        remixViewModel.setCurrentPlayingRemix(chosenMix)
                                        remixViewModel.playOfflineRemix(chosenMix)
                                        isPlayerExpanded = true
                                    }
                            )

                        }


                    }


                }

                if (!isPlayerExpanded && currentPlayingRemix != null)
                    Box (
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        currentPlayingRemix?.let { remix ->
                            MiniPlayer(
                                remixWithSounds = remix,
                                isPlaying = isMusicPlaying,
                                onPlayPauseClick = { remixViewModel.togglePlayPause() },
                                onPlayerExpand = { isPlayerExpanded = true }
                            )
                        }
                    }


                if (isPlayerExpanded && currentPlayingRemix != null) {
                    PlayerScreen(
                        remixWithSounds = currentPlayingRemix!!,
                        isPlaying = isMusicPlaying,
                        onBackClick = {
                            isPlayerExpanded = false
                        },
                        onPreviousClick = { remixViewModel.playPreviousRemix() },
                        onPlayPauseClick = { remixViewModel.togglePlayPause() },
                        onNextClick = { remixViewModel.playNextRemix() },
                        onVolumeChange = { path, vol -> /* изменение громкости */ },
                        onDeleteMixClick = { remixViewModel.deleteFullRemix(currentPlayingRemix!!)
                            remixViewModel.setCurrentPlayingRemix(null)
                            isPlayerExpanded = false
                        }
                    )
                }
            }




            selectedSoundForSheet?.let { sound ->
                ModalBottomSheet (
                    onDismissRequest = { mainViewModel.hideBottomSheet() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.surfaceVariant) }
                ) {
                    SoundBottomSheetContent (
                        sound = sound,
                        onCloseClick = { mainViewModel.hideBottomSheet() },
                        onAddToMixClick = { mainViewModel.addSoundToRemix(sound) },
                        onFavoriteClick = {
                            mainViewModel.hideBottomSheet()
                        }
                    )
                }




            }

        }
    }
}

enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.botton_bar_home_icon, Icons.Default.Home),
    FAVORITES(R.string.botton_bar_mixer_icon, Icons.Default.Tune),
    PROFILE(R.string.botton_bar_profile_icon, Icons.Default.AccountBox),
}
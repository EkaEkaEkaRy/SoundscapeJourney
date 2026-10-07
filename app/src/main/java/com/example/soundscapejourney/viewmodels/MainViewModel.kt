package com.example.soundscapejourney.viewmodels

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soundscapejourney.BuildConfig
import com.example.soundscapejourney.MainUiState
import com.example.soundscapejourney.data.models.RemixTrack
import com.example.soundscapejourney.data.models.Sound
import com.example.soundscapejourney.data.models.toDomainSound
import com.example.soundscapejourney.network.NetworkClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel : ViewModel() {
    // По умолчанию экран находится в состоянии загрузки
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Храним текущую выбранную категорию.
    private val _selectedCategory = MutableStateFlow("Rain")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Создаем экземпляр медиаплеера
    private var mediaPlayer: MediaPlayer? = null

    // Храним ID звука, который играет прямо сейчас (null — если ничего не играет)
    private val _playingSoundId = MutableStateFlow<String?>(null)
    val playingSoundId: StateFlow<String?> = _playingSoundId.asStateFlow()

    // Храним ID звука в буфере
    private val _bufferingSoundId = MutableStateFlow<String?>(null)
    val bufferingSoundId: StateFlow<String?> = _bufferingSoundId.asStateFlow()

    // Поток для хранения текста, который вводит пользователь прямо сейчас
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Флаг: видима ли строка поиска на экране
    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    init {
        observeSearchQuery()
        loadSounds(_selectedCategory.value)
    }

    private fun loadSounds(query: String = "meditation") {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val response = NetworkClient.freesoundApi.searchSounds(
                    query = query,
                    token = BuildConfig.FREESOUND_KEY
                )

                // Берем список из сети, превращаем каждую карточку
                // в наш чистый Sound с помощью маппера
                val domainSounds = response.results.map { it.toDomainSound() }

                // Переключаем экран в состояние Успеха и отдаем ему список
                _uiState.value = MainUiState.Success(sounds = domainSounds)

            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(message = e.localizedMessage ?: "Неизвестная ошибка")
            }
        }
    }



    // Функция, которую будет вызывать интерфейс при клике на чипс
    fun onCategorySelect(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category

        val apiQuery = category.lowercase()

        // Перезапускаем загрузку звуков с новым запросом
        loadSounds(query = apiQuery)
    }

    fun togglePlayPause(sound: Sound) {
        // Если этот же звук уже играет, ставим на паузу/выключаем
        if (_playingSoundId.value == sound.id) {
            stopAudio()
            return
        }

        // Если играл другой звук, сначала выключаем его
        stopAudio()

        _bufferingSoundId.value = sound.id

        // Запускаем новый звук
        mediaPlayer = MediaPlayer().apply {
            setDataSource(sound.audioUrl)
            prepareAsync() // Плеер подготавливается в фоновом потоке
            isLooping = true // зацикливание
            setOnPreparedListener {
                _bufferingSoundId.value = null
                start() // Как только песня готова, включаем
                _playingSoundId.value = sound.id
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _playingSoundId.value = null
    }

    // Переключатель видимости строки поиска
    fun toggleSearchVisibility() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) {
            // Если закрыли поиск, сбрасываем текст и возвращаем дефолтную категорию
            if (_searchQuery.value != "") {
                _searchQuery.value = ""
                loadSounds(query = _selectedCategory.value)
            }
        }
    }

    // Функция, которую UI будет вызывать при изменении текста в поле ввода
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // Ждем после последней вбитой буквы
                .distinctUntilChanged()
                .collect { text ->
                    if (text.isNotBlank()) {
                        loadSounds(query = text)
                    } else if (_isSearchVisible.value) {
                        loadSounds(query = _selectedCategory.value)
                    }
                }
        }
    }


    // Поток данных для экрана Ремикса
    private val _remixTracks = MutableStateFlow<List<RemixTrack>>(emptyList())
    val remixTracks: StateFlow<List<RemixTrack>> = _remixTracks.asStateFlow()

    // Хранилище запущенных плееров для ремикса
    private val remixPlayers = HashMap<String, MediaPlayer>()

    // Глобальный статус: играет ли весь ремикс прямо сейчас
    private val _isRemixPlaying = MutableStateFlow(false)
    val isRemixPlaying: StateFlow<Boolean> = _isRemixPlaying.asStateFlow()


    // функция для добавления в микс
    fun addSoundToRemix(sound: Sound) {
        val alreadyExists = _remixTracks.value.any { it.sound.id == sound.id }
        if (alreadyExists) return

        // Создаем новый трек для ремикса со стандартной громкостью 50%
        val newRemixTrack = RemixTrack(sound = sound, volume = 0.5f)

        // Плюсуем новый трек к текущему списку в потоке данных
        _remixTracks.value = _remixTracks.value + newRemixTrack

        // Если ремикс прямо сейчас играет, то новый звук должен сразу начать петь
        if (_isRemixPlaying.value) {
            val player = MediaPlayer().apply {
                setDataSource(sound.audioUrl)
                isLooping = true
                setVolume(0.5f, 0.5f)
                prepareAsync()
                setOnPreparedListener { start() }
            }
            remixPlayers[sound.id] = player
        }
    }

    // функция удаления звука из микса
    fun removeSoundFromRemix(soundId: String) {
        remixPlayers[soundId]?.apply {
            stop()
            release()
        }
        remixPlayers.remove(soundId)

        _remixTracks.value = _remixTracks.value.filter { it.sound.id != soundId }

        // Если из микса удалили все звуки, то останавливаем статус проигрывания ремикса
        if (_remixTracks.value.isEmpty()) {
            _isRemixPlaying.value = false
        }
    }


    // Изменение громкости конкретного звука в ремиксе
    fun changeTrackVolume(soundId: String, newVolume: Float) {
        // Обновляем список треков, чтобы Compose перерисовал ползунок слайдера
        _remixTracks.value = _remixTracks.value.map { track ->
            if (track.sound.id == soundId) track.copy(volume = newVolume) else track
        }

        // Если этот звук сейчас играет, мгновенно меняем ему громкость в динамике
        remixPlayers[soundId]?.setVolume(newVolume, newVolume)
    }

    // Главная кнопка Включения / Выключения ремикса
    fun toggleRemixPlayPause() {
        stopAudio()
        if (_isRemixPlaying.value) {
            remixPlayers.values.forEach { it.stop(); it.release() }
            remixPlayers.clear()
            _isRemixPlaying.value = false
        } else {
            _isRemixPlaying.value = true

            _remixTracks.value.forEach { track ->
                val player = MediaPlayer().apply {
                    setDataSource(track.sound.audioUrl)
                    isLooping = true
                    setVolume(track.volume, track.volume)
                    prepareAsync()
                    setOnPreparedListener { start() }
                }
                // Сохраняем плеер в нашу карту, чтобы потом им управлять
                remixPlayers[track.sound.id] = player
            }
        }
    }

    // Храним звук, который сейчас открыт в шторке
    private val _selectedSoundForSheet = MutableStateFlow<Sound?>(null)
    val selectedSoundForSheet: StateFlow<Sound?> = _selectedSoundForSheet.asStateFlow()

    // Функция для открытия шторки
    fun showBottomSheet(sound: Sound) {
        _selectedSoundForSheet.value = sound
    }

    // Функция для закрытия шторки
    fun hideBottomSheet() {
        _selectedSoundForSheet.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        remixPlayers.values.forEach { it.release() }
    }
}
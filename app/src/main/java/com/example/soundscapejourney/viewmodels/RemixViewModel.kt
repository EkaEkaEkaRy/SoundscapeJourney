package com.example.soundscapejourney.viewmodels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soundscapejourney.components.FileDownloader
import com.example.soundscapejourney.data.RemixPlaybackService
import com.example.soundscapejourney.data.database.dao.RemixDao
import com.example.soundscapejourney.data.database.entities.RemixEntity
import com.example.soundscapejourney.data.database.entities.RemixWithSounds
import com.example.soundscapejourney.data.database.entities.SoundEntity
import com.example.soundscapejourney.data.models.RemixTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RemixViewModel(
    private val remixDao: RemixDao,
    private val fileDownloader: FileDownloader,
    private val context: Context
) : ViewModel() {

    // Чтение данных для UI
    val allRemixes = remixDao.getAllRemixesWithSounds()

    // Ссылки на фоновый сервис
    private var playbackService: RemixPlaybackService? = null
    private var isServiceBound = false

    // Стейт загрузки
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Храним текущий запущенный ремикс
    private val _currentPlayingRemix = MutableStateFlow<RemixWithSounds?>(null)
    val currentPlayingRemix = _currentPlayingRemix.asStateFlow()

    // Статус: играет ли микс или стоит на паузе
    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying = _isMusicPlaying.asStateFlow()

    // Карта для активных офлайн-плееров
    private val offlinePlayers = HashMap<String, MediaPlayer>()

    // Список всех сохраненных миксов
    private var cachedRemixesList: List<RemixWithSounds> = emptyList()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RemixPlaybackService.PlaybackBinder
            playbackService = binder.getService()
            isServiceBound = true
            android.util.Log.d("RemixViewModel", "Сервис уведомлений успешно подключен!")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    // private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    init {

        // Автоматически кэшируем список миксов при изменениях в БД
        viewModelScope.launch {
            allRemixes.collect { cachedRemixesList = it }
        }

//        viewModelScope.launch {
//            // Читаем сохраненный ID из SharedPreferences
//            val savedRemixId = prefs.getLong("last_playing_remix_id", -1L)
//
//            if (savedRemixId != -1L) {
//                allRemixes.collect { list ->
//                    if (list.isNotEmpty()) {
//                        // Ищем наш ремикс в списке по ID
//                        val lastRemix = list.find { it.remix.id == savedRemixId }
//                        if (lastRemix != null) {
//                            // Просто восстанавливаем стейт плеера БЕЗ автозапуска звука,
//                            _currentPlayingRemix.value = lastRemix
//                            _isMusicPlaying.value = false // Плеер на паузе, но плашка на месте!
//                        }
//                    }
//                }
//            }
//        }

        // Запускаем и привязываем фоновый сервис при создании ViewModel
        val intent = Intent(context, RemixPlaybackService::class.java)
        context.startService(intent) // Запуск, чтобы сервис жил независимо
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) // Привязка для команд
    }

    fun setCurrentPlayingRemix(remix: RemixWithSounds?) {
        _currentPlayingRemix.value = remix
    }

    fun saveRemix(remixName: String, currentTracks: List<RemixTrack>) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val imageFiles: Array<String>? = context.assets.list("images/mixes_images")

                val randomImageName = if (!imageFiles.isNullOrEmpty()) {
                    imageFiles.random()
                } else {
                    "5182990109_af549947c5_m.jpg"
                }

                // Сохраняем путь к картинке относительно папки assets
                val finalImagePath = "images/mixes_images/$randomImageName"

                // Сначала скачиваем каждый аудиофайл на устройство
                val soundsToSave = currentTracks.map { track ->
                    val localPath = fileDownloader.downloadAudioFile(
                        soundIdApi = track.sound.id,
                        audioUrl = track.sound.audioUrl
                    )

                    SoundEntity(
                        soundIdApi = track.sound.id,
                        remixId = 0,
                        title = track.sound.title,
                        filePath = localPath,
                        volume = track.volume
                    )
                }

                // Сохраняем карточку ремикса в Room и получаем сгенерированный ID
                val newRemixId = remixDao.insertRemix(
                    RemixEntity(name = remixName, imagePath = finalImagePath)
                )

                // Привязываем наши скачанные звуки к полученному ID реmiкса
                val finalSounds = soundsToSave.map { sound ->
                    sound.copy(remixId = newRemixId)
                }

                // Записываем в базу данных
                remixDao.insertSounds(finalSounds)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false // Выключаем лоадер
            }
        }
    }

    fun deleteFullRemix(remixWithSounds: RemixWithSounds) {
        // Сначала останавливаем всё, что играло до этого
        stopAllOfflinePlayers()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    remixWithSounds.sounds.forEach { soundEntity ->

                        val usageCount = remixDao.getSoundUsageCount(soundEntity.soundIdApi)

                        if (usageCount <= 1) {
                            val file = File(soundEntity.filePath)
                            if (file.exists()) {
                                val isDeleted = file.delete()
                                android.util.Log.d("RemixViewModel", "Файл ${soundEntity.title} удален: $isDeleted")
                            }
                        } else {
                            android.util.Log.d("RemixViewModel", "Файл ${soundEntity.title} оставлен, так как нужен другим миксам")
                        }
                    }

                    remixDao.deleteRemix(remixWithSounds.remix)

                } catch (e: Exception) {
                    android.util.Log.e("RemixViewModel", "Ошибка при удалении ремикса", e)
                }
            }
        }
    }

    // Вспомогательный метод очистки плееров
    private fun stopAllOfflinePlayers() {
        offlinePlayers.values.forEach {
            try { it.stop(); it.release() } catch (e: Exception) { e.printStackTrace() }
        }
        offlinePlayers.clear()
    }


    // Запуск микса через шторку
    fun playOfflineRemix(remixWithSounds: RemixWithSounds) {
        _currentPlayingRemix.value = remixWithSounds
        _isMusicPlaying.value = true

        // prefs.edit().putLong("last_playing_remix_id", remixWithSounds.remix.id).apply()

        // Делегируем проигрывание и создание уведомления фоновому сервису
        playbackService?.playRemix(
            mixName = remixWithSounds.remix.name,
            sounds = remixWithSounds.sounds,
            assetImagePath = remixWithSounds.remix.imagePath,
        )
    }

    // метод плей/пауза через сервис
    fun togglePlayPause() {
        if (_currentPlayingRemix.value == null) return

        if (_isMusicPlaying.value) {
            playbackService?.pauseAll()
            _isMusicPlaying.value = false
        } else {
            playbackService?.resumeAll()
            _isMusicPlaying.value = true
        }
    }

    // метод следующего микса
    fun playNextRemix() {
        val current = _currentPlayingRemix.value ?: return
        val currentIndex = cachedRemixesList.indexOfFirst { it.remix.id == current.remix.id }
        if (currentIndex == -1) return

        val nextIndex = if (currentIndex == cachedRemixesList.lastIndex) 0 else currentIndex + 1
        playOfflineRemix(cachedRemixesList[nextIndex])
    }

    // метод предыдущего микса
    fun playPreviousRemix() {
        val current = _currentPlayingRemix.value ?: return
        val currentIndex = cachedRemixesList.indexOfFirst { it.remix.id == current.remix.id }
        if (currentIndex == -1) return

        val prevIndex = if (currentIndex == 0) cachedRemixesList.lastIndex else currentIndex - 1
        playOfflineRemix(cachedRemixesList[prevIndex])
    }

    fun resetCurrentSession() {
        _currentPlayingRemix.value = null
        _isMusicPlaying.value = false
        // prefs.edit().remove("last_playing_remix_id").apply()
    }

    // Когда ViewModel уничтожается, корректно отвязываем сервис, чтобы не было утечек памяти
    override fun onCleared() {
        super.onCleared()
        // stopAllOfflinePlayers()
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    /*
       // Запуск сохраненного ремикса
    fun playOfflineRemix(remixWithSounds: RemixWithSounds) {
        // Сначала останавливаем всё, что играло до этого
        stopAllOfflinePlayers()

        _currentPlayingRemix.value = remixWithSounds
        _isMusicPlaying.value = true

        // Запускаем все звуки этого ремикса
        remixWithSounds.sounds.forEach { soundEntity ->
            try {
                val player = MediaPlayer().apply {
                    // Передаем локальный filePath из базы данных
                    setDataSource(soundEntity.filePath)
                    isLooping = true
                    setVolume(soundEntity.volume, soundEntity.volume)
                    prepareAsync()
                    setOnPreparedListener { start() }
                }
                offlinePlayers[soundEntity.filePath] = player
            } catch (e: Exception) {
                android.util.Log.e("RemixViewModel", "Ошибка запуска файла: ${soundEntity.filePath}", e)
            }
        }
    }

    // Функция play/pause
    fun togglePlayPause() {
        if (_currentPlayingRemix.value == null) return

        if (_isMusicPlaying.value) {
            // Ставим на паузу все плееры
            offlinePlayers.values.forEach { if (it.isPlaying) it.pause() }
            _isMusicPlaying.value = false
        } else {
            // Запускаем заново все плееры
            offlinePlayers.values.forEach { it.start() }
            _isMusicPlaying.value = true
        }
    }

    // Функция следующего ремикса
    fun playNextRemix() {
        val current = _currentPlayingRemix.value ?: return
        val currentIndex = cachedRemixesList.indexOfFirst { it.remix.id == current.remix.id }
        if (currentIndex == -1) return

        // Если это последний микс — переключаемся на самый первый
        val nextIndex = if (currentIndex == cachedRemixesList.lastIndex) 0 else currentIndex + 1
        playOfflineRemix(cachedRemixesList[nextIndex])
    }

    // Функция предыдущего ремикс
    fun playPreviousRemix() {
        val current = _currentPlayingRemix.value ?: return
        val currentIndex = cachedRemixesList.indexOfFirst { it.remix.id == current.remix.id }
        if (currentIndex == -1) return

        val prevIndex = if (currentIndex == 0) cachedRemixesList.lastIndex else currentIndex - 1
        playOfflineRemix(cachedRemixesList[prevIndex])
    }
    */
}

package com.example.soundscapejourney.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.soundscapejourney.MainActivity
import com.example.soundscapejourney.data.database.entities.SoundEntity
import java.io.InputStream

class RemixPlaybackService : Service() {

    private val binder = PlaybackBinder()

    // Системный пульт управления для шторки
    private var mediaSession: MediaSessionCompat? = null

    // Карта активных локальных плееров
    private val offlinePlayers = HashMap<String, MediaPlayer>()

    // Данные о текущем играющем миксе
    private var currentMixName = "Ремикс"
    private var isPlaying = false

    private val channelID = "soundscape_playback_channel"
    private val notificationID = 101

    inner class PlaybackBinder : Binder() {
        fun getService(): RemixPlaybackService = this@RemixPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Инициализируем системную медиа-сессию
        mediaSession = MediaSessionCompat(this, "SoundscapeMediaSession").apply {
            isActive = true
            // Настраиваем колбэки: что делать, когда пользователь нажимает кнопки в шторке
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumeAll()
                }

                override fun onPause() {
                    pauseAll()
                }
            })
        }
    }

    // Перевод картинки из ассетов в формате Bitmap
    private fun loadBitmapFromAssets(assetPath: String): Bitmap? {
        return try {
            // Открываем поток чтения файла из папки assets
            val inputStream: InputStream = assets.open(assetPath)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Не удалось загрузить обложку из ассетов: $assetPath", e)
            null
        }
    }

    private fun updateMediaMetadata(mixName: String, assetImagePath: String) {
        val albumArtBitmap = loadBitmapFromAssets(assetImagePath)

        // Упаковываем данные в системный контейнер метаданных
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mixName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Soundscape Journey")

        // Если картинка успешно считалась, отдаем её операционной системе Android
        if (albumArtBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, albumArtBitmap)
        }

        // Передаем метаданные в активную сессию
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    // Запуск офлайн звуков
    fun playRemix(mixName: String, assetImagePath: String, sounds: List<SoundEntity>) {
        stopAll()
        currentMixName = mixName
        isPlaying = true

        updateMediaMetadata(mixName, assetImagePath)

        sounds.forEach { sound ->
            try {
                val player = MediaPlayer().apply {
                    setDataSource(sound.filePath)
                    isLooping = true
                    setVolume(sound.volume, sound.volume)
                    prepareAsync()
                    setOnPreparedListener { start() }
                }
                offlinePlayers[sound.filePath] = player
            } catch (e: Exception) {
                android.util.Log.e("PlaybackService", "Ошибка запуска: ${sound.filePath}", e)
            }
        }

        // Переводим сервис в статус Foreground
        startForeground(notificationID, buildNotification())
        updatePlaybackState()
    }

    fun pauseAll() {
        offlinePlayers.values.forEach { if (it.isPlaying) it.pause() }
        isPlaying = false
        // Обновляем уведомление, меняя кнопку на "Play"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationID, buildNotification())
        updatePlaybackState()
    }

    fun resumeAll() {
        offlinePlayers.values.forEach { it.start() }
        isPlaying = true
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationID, buildNotification())
        updatePlaybackState()
    }

    fun stopAll() {
        offlinePlayers.values.forEach {
            try { it.stop(); it.release() } catch (e: Exception) { e.printStackTrace() }
        }
        offlinePlayers.clear()
        isPlaying = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Синхронизируем статус плеера с операционной системой Android
    private fun updatePlaybackState() {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE

        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .setActions(actions)
                .build()
        )
    }

    // Собираем красивый медиа-виджет для шторки уведомлений
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Создаем кнопки управления для шторки
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Пауза",
                PendingIntent.getService(this, 1, Intent(this, RemixPlaybackService::class.java).setAction("PAUSE"), PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Старт",
                PendingIntent.getService(this, 1, Intent(this, RemixPlaybackService::class.java).setAction("PLAY"), PendingIntent.FLAG_IMMUTABLE)
            )
        }

        return NotificationCompat.Builder(this, channelID)
            .setContentTitle(currentMixName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0)
            )
            .addAction(playPauseAction)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Перехватываем клики по кнопкам уведомления
        when (intent?.action) {
            "PLAY" -> resumeAll()
            "PAUSE" -> pauseAll()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID, "Медиаплеер Soundscape",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Управление фоновым воспроизведением миксов" }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        stopAll()
        mediaSession?.release()
    }
}

package com.example.soundscapejourney.components

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class FileDownloader(private val context: Context) {

    // Скачивает файл по URL-ссылке и сохраняет его во внутреннюю память приложения
    // Возвращает абсолютный локальный путь к файлу (filePath)
    suspend fun downloadAudioFile(soundIdApi: String, audioUrl: String): String =
        withContext(Dispatchers.IO) {
            // Создаем локальный файл в скрытой папке приложения на телефоне (files/offline_sounds/)
            val folder = File(context.filesDir, "offline_sounds")
            if (!folder.exists()) {
                folder.mkdirs() // Если папки нет — создаем её
            }

            // Имя файла делаем уникальным на основе ID из Freesound API
            val localFile = File(folder, "sound_$soundIdApi.mp3")

            // Если такой файл уже скачивался ранее,
            // мы не качаем его заново, а просто возвращаем готовый путь
            if (localFile.exists()) {
                return@withContext localFile.absolutePath
            }

            // Скачиваем поток байт из интернета и записываем на диск телефона
            URL(audioUrl).openStream().use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Возвращаем итоговый путь
            return@withContext localFile.absolutePath
        }
}
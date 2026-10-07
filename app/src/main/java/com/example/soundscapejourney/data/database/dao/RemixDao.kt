package com.example.soundscapejourney.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.soundscapejourney.data.database.entities.RemixEntity
import com.example.soundscapejourney.data.database.entities.RemixWithSounds
import com.example.soundscapejourney.data.database.entities.SoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemixDao {
    // Вставка ремикса
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertRemix(remix: RemixEntity): Long

    // Вставка списка звуков
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertSounds(sounds: List<SoundEntity>)

    // Получить все ремиксы со звуками
    @Transaction
    @Query("SELECT * FROM remixes ORDER BY createdAt DESC")
    fun getAllRemixesWithSounds(): Flow<List<RemixWithSounds>>

    // Считаем, сколько раз данный звук используется в сохраненных ремиксах
    @Query("SELECT COUNT(*) FROM sounds WHERE soundIdApi = :apiId")
    suspend fun getSoundUsageCount(apiId: String): Int

    // Удалить конкретный ремикс
    @Delete
    suspend fun deleteRemix(remix: RemixEntity)
}
package com.example.soundscapejourney.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// Таблица Звуков
@Entity(
    tableName = "sounds",
    foreignKeys = [
        ForeignKey(
            entity = RemixEntity::class,
            parentColumns = ["id"],
            childColumns = ["remixId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val soundIdApi: String,
    val remixId: Long, // Ссылка на ID ремикса
    val title: String,
    val filePath: String,
    val volume: Float
)
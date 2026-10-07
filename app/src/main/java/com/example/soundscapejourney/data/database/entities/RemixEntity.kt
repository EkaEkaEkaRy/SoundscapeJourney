package com.example.soundscapejourney.data.database.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

// Таблица Ремиксов
@Entity(tableName = "remixes")
data class RemixEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imagePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class RemixWithSounds(
    @Embedded val remix: RemixEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "remixId"
    )
    val sounds: List<SoundEntity>
)

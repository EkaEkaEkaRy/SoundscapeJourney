package com.example.soundscapejourney.data.models

import com.google.gson.annotations.SerializedName

// Класс, описывающий весь ответ от сервера
data class FreesoundResponse(
    @SerializedName("results") val results: List<FreesoundSoundDto>
)

data class FreesoundSoundDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("tags") val tags: List<String>,
    // Картинки и превью-аудио Freesound прячет внутри объекта "previews" и "images"
    @SerializedName("images") val images: FreesoundImagesDto?,
    @SerializedName("previews") val previews: FreesoundPreviewsDto?
)

data class FreesoundImagesDto(
    @SerializedName("waveform_m") val waveformUrl: String
)

data class FreesoundPreviewsDto(
    @SerializedName("preview-hq-mp3") val audioUrl: String
)


fun FreesoundSoundDto.toDomainSound(): Sound {
    // Переводим секунды в красивый формат
    val minutes = (this.duration / 60).toInt()
    val seconds = (this.duration % 60).toInt()
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    return Sound(
        id = this.id.toString(),
        title = this.name,
        audioUrl = this.previews?.audioUrl ?: "",
        imageUrl = this.images?.waveformUrl ?: "",
        duration = formattedDuration,
        tags = this.tags
    )
}

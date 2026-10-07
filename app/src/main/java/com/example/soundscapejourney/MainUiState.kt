package com.example.soundscapejourney

import com.example.soundscapejourney.data.models.Sound

sealed interface MainUiState {
    object Loading : MainUiState // Состояние загрузки
    data class Success(val sounds: List<Sound>) : MainUiState // Успех
    data class Error(val message: String) : MainUiState // Ошибка
}

package com.example.soundscapejourney.viewmodels

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    // Поток для темы: true = Тёмная, false = Светлая
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Поток для языка: "ru" = Русский, "en" = Английский
    private val _language = MutableStateFlow("ru")
    val language: StateFlow<String> = _language.asStateFlow()

    // Функция переключения темы
    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    // Функция переключения языка
    fun setLanguage(langCode: String) {
        _language.value = langCode

        viewModelScope.launch(Dispatchers.Main) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)

            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

}
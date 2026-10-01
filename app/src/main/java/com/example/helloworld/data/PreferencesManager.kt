package com.example.helloworld.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("kfcc_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Kept for compatibility with existing screens.
    val isDarkMode: StateFlow<Boolean> = MutableStateFlow(_themeMode.value == "dark").asStateFlow()

    private val _isAudioAutoplay = MutableStateFlow(prefs.getBoolean("audio_autoplay", true))
    val isAudioAutoplay: StateFlow<Boolean> = _isAudioAutoplay.asStateFlow()

    fun setThemeMode(mode: String) {
        val normalized = mode.lowercase().let {
            if (it == "light" || it == "dark") it else "system"
        }
        prefs.edit().putString("theme_mode", normalized).apply()
        _themeMode.value = normalized
    }

    fun toggleDarkMode() {
        setThemeMode(if (_themeMode.value == "dark") "light" else "dark")
    }

    fun toggleAudioAutoplay() {
        val newValue = !_isAudioAutoplay.value
        prefs.edit().putBoolean("audio_autoplay", newValue).apply()
        _isAudioAutoplay.value = newValue
    }
}

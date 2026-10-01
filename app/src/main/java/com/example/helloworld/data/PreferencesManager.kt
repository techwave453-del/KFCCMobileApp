package com.example.helloworld.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("kfcc_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", null)
            ?: if (prefs.getBoolean("dark_mode", false)) "dark" else "system"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isAudioAutoplay = MutableStateFlow(prefs.getBoolean("audio_autoplay", true))
    val isAudioAutoplay: StateFlow<Boolean> = _isAudioAutoplay.asStateFlow()

    fun setThemeMode(mode: String) {
        val normalized = mode.takeIf { it in setOf("system", "light", "dark") } ?: "system"
        prefs.edit()
            .putString("theme_mode", normalized)
            .putBoolean("dark_mode", normalized == "dark")
            .apply()
        _themeMode.value = normalized
    }

    fun toggleAudioAutoplay() {
        val newValue = !_isAudioAutoplay.value
        prefs.edit().putBoolean("audio_autoplay", newValue).apply()
        _isAudioAutoplay.value = newValue
    }
}

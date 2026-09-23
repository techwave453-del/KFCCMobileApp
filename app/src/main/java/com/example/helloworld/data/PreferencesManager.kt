package com.example.helloworld.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("kfcc_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAudioAutoplay = MutableStateFlow(prefs.getBoolean("audio_autoplay", true))
    val isAudioAutoplay: StateFlow<Boolean> = _isAudioAutoplay.asStateFlow()

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        prefs.edit().putBoolean("dark_mode", newValue).apply()
        _isDarkMode.value = newValue
    }

    fun toggleAudioAutoplay() {
        val newValue = !_isAudioAutoplay.value
        prefs.edit().putBoolean("audio_autoplay", newValue).apply()
        _isAudioAutoplay.value = newValue
    }
}

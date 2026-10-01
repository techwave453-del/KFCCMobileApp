package com.example.helloworld.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.helloworld.data.PreferencesManager
import kotlinx.coroutines.flow.StateFlow

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    val themeMode: StateFlow<String> = preferencesManager.themeMode
    val isAudioAutoplay: StateFlow<Boolean> = preferencesManager.isAudioAutoplay

    fun setThemeMode(mode: String) = preferencesManager.setThemeMode(mode)
    fun toggleAudioAutoplay() = preferencesManager.toggleAudioAutoplay()
}

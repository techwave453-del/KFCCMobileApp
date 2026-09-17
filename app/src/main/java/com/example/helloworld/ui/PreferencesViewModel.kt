package com.example.helloworld.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.helloworld.data.PreferencesManager
import kotlinx.coroutines.flow.StateFlow

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    val isDarkMode: StateFlow<Boolean> = preferencesManager.isDarkMode
    val isAudioAutoplay: StateFlow<Boolean> = preferencesManager.isAudioAutoplay

    fun toggleDarkMode() = preferencesManager.toggleDarkMode()
    fun toggleAudioAutoplay() = preferencesManager.toggleAudioAutoplay()
}

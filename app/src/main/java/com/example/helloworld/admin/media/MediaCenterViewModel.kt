package com.example.helloworld.admin.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application.applicationContext)

    private val _items = MutableStateFlow<List<AdminMediaItem>>(emptyList())
    val items: StateFlow<List<AdminMediaItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.load()
                .onSuccess { _items.value = it }
                .onFailure { _error.value = it.message ?: "Unable to load the media library." }
            _loading.value = false
        }
    }

    fun refresh() = load()
}

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
    private val _savingId = MutableStateFlow<Long?>(null)
    val savingId: StateFlow<Long?> = _savingId.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.load().onSuccess { _items.value = it }.onFailure { _error.value = it.message ?: "Unable to load the media library." }
            _loading.value = false
        }
    }

    fun update(item: AdminMediaItem, title: String, description: String, category: String, featured: Boolean) {
        viewModelScope.launch {
            _savingId.value = item.id
            _error.value = null
            _saved.value = false
            repository.update(item.id, MediaUpdateRequest(title.trim(), description.trim(), category.trim().ifBlank { "general" }, featured && item.isVideo))
                .onSuccess { updated -> _items.value = _items.value.map { if (it.id == updated.id) updated else it }; _saved.value = true }
                .onFailure { _error.value = it.message ?: "Unable to save media." }
            _savingId.value = null
        }
    }

    fun delete(item: AdminMediaItem) {
        viewModelScope.launch {
            _savingId.value = item.id
            _error.value = null
            repository.delete(item.id)
                .onSuccess { _items.value = _items.value.filterNot { it.id == item.id } }
                .onFailure { _error.value = it.message ?: "Unable to delete media." }
            _savingId.value = null
        }
    }

    fun clearStatus() { _error.value = null; _saved.value = false }
}

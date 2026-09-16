package com.example.helloworld.admin.media

import android.app.Application
import android.net.Uri
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

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    private val context = application.applicationContext

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

    fun upload(
        uri: Uri,
        title: String,
        description: String,
        category: String,
        type: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _uploading.value = true
            _error.value = null
            _uploadMessage.value = null
            try {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to read the selected file.")
                val fileName = resolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: (uri.lastPathSegment ?: "media-file")
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"

                repository.upload(bytes, fileName, mimeType, title, description, category, type)
                    .onSuccess {
                        _uploadMessage.value = "Media uploaded successfully."
                        onComplete()
                        load()
                    }
                    .onFailure { _error.value = it.message ?: "Unable to upload media." }
            } catch (error: Exception) {
                _error.value = error.message ?: "Unable to upload media."
            } finally {
                _uploading.value = false
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _uploadMessage.value = null
    }

    fun refresh() = load()
}

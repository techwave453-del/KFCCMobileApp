package com.example.helloworld.admin.media

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application.applicationContext)
    private val context = application.applicationContext

    private val _items = MutableStateFlow<List<AdminMediaItem>>(emptyList())
    val items: StateFlow<List<AdminMediaItem>> = _items.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()
    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init { load() }

    fun contentResolver(): ContentResolver = context.contentResolver

    fun displayName(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: (uri.lastPathSegment ?: "media-file")

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.load().onSuccess { _items.value = it }.onFailure { _error.value = it.message ?: "Unable to load the media library." }
            _loading.value = false
        }
    }

    fun upload(uri: Uri, title: String, description: String, category: String, type: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uploading.value = true
            _error.value = null
            _uploadMessage.value = null
            try {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("The selected file can no longer be accessed. Please select it again.")
                if (bytes.isEmpty()) error("The selected file is empty. Please select another file.")
                val fileName = displayName(uri)
                val detectedMime = resolver.getType(uri)
                val mimeType = detectedMime?.takeIf { it != "application/octet-stream" && it != "binary/octet-stream" }
                    ?: mimeTypeFromFileName(fileName)
                    ?: error("The selected file type could not be determined. Please select an image, video, audio file, or PDF.")
                repository.upload(bytes, fileName, mimeType, title, description, category, type)
                    .onSuccess { _uploadMessage.value = "Media uploaded successfully."; onComplete(); load() }
                    .onFailure { _error.value = it.message ?: "Unable to upload media." }
            } catch (error: Exception) { _error.value = error.message ?: "Unable to upload media." }
            finally { _uploading.value = false }
        }
    }

    private fun mimeTypeFromFileName(fileName: String): String? = when (fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "pdf" -> "application/pdf"
        else -> null
    }

    fun save(item: AdminMediaItem, title: String, description: String, category: String, published: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true; _error.value = null; _actionMessage.value = null
            repository.update(item.id, title, description, category, published = published)
                .onSuccess { _actionMessage.value = "Media details saved."; onComplete(); load() }
                .onFailure { _error.value = it.message ?: "Unable to save media." }
            _saving.value = false
        }
    }

    fun setFeatured(item: AdminMediaItem, featured: Boolean) {
        viewModelScope.launch {
            _saving.value = true; _error.value = null; _actionMessage.value = null
            repository.setFeatured(item.id, featured)
                .onSuccess { _actionMessage.value = if (featured) "Featured video updated." else "Video removed from Featured."; load() }
                .onFailure { _error.value = it.message ?: "Unable to update the Featured Video." }
            _saving.value = false
        }
    }

    fun delete(item: AdminMediaItem, onComplete: () -> Unit) {
        viewModelScope.launch {
            _deleting.value = true; _error.value = null; _actionMessage.value = null
            repository.delete(item.id)
                .onSuccess { _actionMessage.value = "Media deleted."; onComplete(); load() }
                .onFailure { _error.value = it.message ?: "Unable to delete media." }
            _deleting.value = false
        }
    }

    fun clearMessages() { _error.value = null; _uploadMessage.value = null; _actionMessage.value = null }
    fun refresh() = load()
}

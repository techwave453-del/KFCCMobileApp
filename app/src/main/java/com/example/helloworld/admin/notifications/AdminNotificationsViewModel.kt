package com.example.helloworld.admin.notifications

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.data.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminNotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminRepositoryProvider.get(application)

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            repository.getNotifications()
                .onSuccess { _notifications.value = it }
                .onFailure { _error.value = it.message ?: "Unable to load notifications." }
            _loading.value = false
        }
    }

    fun uploadImage(uri: Uri, contentType: String, onComplete: (String?, String?) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val bytes = compressNotificationImage(uri)
                repository.uploadNotificationImage(bytes, "image/jpeg").getOrThrow()
            }
            result.onSuccess { url ->
                _message.value = "Picture uploaded."
                onComplete(url, uri.lastPathSegment ?: "Picture selected")
            }.onFailure {
                _error.value = it.message ?: "Unable to upload the notification picture."
                onComplete(null, null)
            }
        }
    }

    private fun compressNotificationImage(uri: Uri): ByteArray {
        val resolver = getApplication<Application>().contentResolver
        val source = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Unable to read the selected picture.")

        source.use {
            val maxDimension = 1280
            val scale = minOf(
                1f,
                maxDimension.toFloat() / maxOf(source.width, source.height).toFloat()
            )
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                source
            }

            try {
                var quality = 88
                var output: ByteArray
                do {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    output = stream.toByteArray()
                    stream.close()
                    quality -= 8
                } while (output.size > 900 * 1024 && quality >= 40)

                require(output.size <= 900 * 1024) {
                    "The selected picture could not be compressed enough for notification delivery."
                }
                return output
            } finally {
                if (bitmap !== source) bitmap.recycle()
            }
        }
    }

    fun send(title: String, body: String, type: String, imageUrl: String? = null) {
        viewModelScope.launch {
            _sending.value = true
            _message.value = null
            _error.value = null
            repository.postAnnouncement(
                title.trim(),
                body.trim(),
                type.trim().ifBlank { "general" },
                imageUrl = imageUrl?.trim()?.ifBlank { null }
            ).onSuccess { sentDirectly ->
                _message.value = if (sentDirectly) {
                    "Notification sent successfully. It is now in notification history."
                } else {
                    "Notification saved offline. It will be sent automatically when the connection is restored."
                }
                refresh()
            }.onFailure {
                _error.value = it.message ?: "Unable to send notification."
            }
            _sending.value = false
        }
    }

    fun update(notification: AppNotification) {
        viewModelScope.launch {
            repository.updateNotification(
                notification.id,
                notification.title,
                notification.message,
                notification.type,
                notification.isEnabled,
                notification.showOnInstall,
                notification.showOnSignIn
            ).onSuccess {
                _message.value = "Notification updated."
                refresh()
            }.onFailure {
                _error.value = it.message ?: "Unable to update notification."
            }
        }
    }

    fun setInstallDefault(notification: AppNotification, enabled: Boolean) {
        viewModelScope.launch {
            repository.setInstallDefault(notification.id, enabled)
                .onSuccess { _message.value = if (enabled) "Selected as install notification." else "Install notification cleared."; refresh() }
                .onFailure { _error.value = it.message ?: "Unable to change install notification." }
        }
    }

    fun setSignInDefault(notification: AppNotification, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSignInDefault(notification.id, enabled)
                .onSuccess { _message.value = if (enabled) "Selected as sign-in notification." else "Sign-in notification cleared."; refresh() }
                .onFailure { _error.value = it.message ?: "Unable to change sign-in notification." }
        }
    }

    fun delete(notification: AppNotification) {
        viewModelScope.launch {
            repository.deleteNotification(notification.id)
                .onSuccess { _message.value = "Notification deleted."; refresh() }
                .onFailure { _error.value = it.message ?: "Unable to delete notification." }
        }
    }

    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AdminNotificationsViewModel(application) as T
    }
}

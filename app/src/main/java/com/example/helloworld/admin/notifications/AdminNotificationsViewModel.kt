package com.example.helloworld.admin.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepositoryProvider
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

    fun send(title: String, body: String, type: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _sending.value = true
            _message.value = null
            _error.value = null
            repository.postAnnouncement(title.trim(), body.trim(), type.trim().ifBlank { "general" })
                .onSuccess {
                    _message.value = "Notification queued. It will sync to Supabase when connectivity is available."
                    onComplete()
                }
                .onFailure { _error.value = it.message ?: "Unable to send notification." }
            _sending.value = false
        }
    }

    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AdminNotificationsViewModel(application) as T
    }
}

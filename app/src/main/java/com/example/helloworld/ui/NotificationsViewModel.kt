package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.getNotifications()
                .onSuccess { _notifications.value = it }
                .onFailure { _error.value = it.message ?: "Unable to load notifications." }
            _loading.value = false
        }
    }

    fun markRead(notification: AppNotification) {
        if (notification.readAt != null) return
        viewModelScope.launch {
            repository.markRead(notification.id).onSuccess {
                _notifications.value = _notifications.value.map {
                    if (it.id == notification.id) it.copy(readAt = "read") else it
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead().onSuccess {
                _notifications.value = _notifications.value.map { it.copy(readAt = "read") }
            }
        }
    }

    fun clearError() { _error.value = null }
}

package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.NotificationRepository
import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.notifications.KfccNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeNotifications().collectLatest {
                _notifications.value = it.sortedByDescending(AppNotification::createdAt)
            }
        }
        KfccNotificationScheduler.syncNow(KfccDataContext.appContext)
        refresh()
    }

    fun refresh() {
        KfccNotificationScheduler.syncNow(KfccDataContext.appContext)
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getNotifications()
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}

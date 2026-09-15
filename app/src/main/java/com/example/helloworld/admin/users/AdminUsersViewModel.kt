package com.example.helloworld.admin.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminUsersRepository(application.applicationContext)
    private val _users = MutableStateFlow<List<AdminManagedUser>>(emptyList())
    val users: StateFlow<List<AdminManagedUser>> = _users.asStateFlow()
    private val _requests = MutableStateFlow<List<AdminAccessRequest>>(emptyList())
    val requests: StateFlow<List<AdminAccessRequest>> = _requests.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        val usersResult = repository.users()
        usersResult.onSuccess { _users.value = it }.onFailure { _error.value = it.message }
        repository.accessRequests().onSuccess { _requests.value = it }
        _loading.value = false
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminUsersViewModel::class.java)) return AdminUsersViewModel(application) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

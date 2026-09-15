package com.example.helloworld.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminRepository(application.applicationContext)

    private val _user = MutableStateFlow<AdminUser?>(null)
    val user: StateFlow<AdminUser?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { restoreSession() }

    fun restoreSession() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _user.value = repository.restoreSession()
            _isLoading.value = false
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.login(username, password)
            if (result.ok && result.user != null) {
                _user.value = result.user
            } else {
                _error.value = result.error ?: "Invalid username or password."
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.logout()
            _user.value = null
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}

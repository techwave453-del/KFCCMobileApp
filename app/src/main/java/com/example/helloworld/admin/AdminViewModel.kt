package com.example.helloworld.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    // One application-scoped repository is shared with every admin module.
    private val repository = AdminRepositoryProvider.get(application)

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
            val restored = repository.restoreSession()
            _user.value = restored?.takeIf { it.is_active }
            _isLoading.value = false
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.login(username, password)
            if (result.ok) {
                // Never trust role/permissions from the login response alone.
                // /api/admin/me is the authoritative post-login authorization state.
                val authoritativeUser = repository.restoreSession()
                if (authoritativeUser != null && authoritativeUser.is_active) {
                    _user.value = authoritativeUser
                } else {
                    _user.value = null
                    _error.value = "Your administrator session could not be verified. Please sign in again."
                }
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

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                return AdminViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

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
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        repository.users().onSuccess { _users.value = it }.onFailure { _error.value = it.message }
        repository.accessRequests().onSuccess { _requests.value = it }
            .onFailure { if (_error.value == null) _error.value = it.message }
        _loading.value = false
    }

    fun approve(request: AdminAccessRequest, role: String, permissions: List<String>) = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        _message.value = null
        repository.approveRequest(request.id, role, permissions)
            .onSuccess { code ->
                _message.value = if (!code.isNullOrBlank()) {
                    "Administrator approved. One-time activation password: $code"
                } else "Administrator approved."
                refresh()
            }
            .onFailure { _error.value = it.message ?: "Unable to approve administrator." }
        _loading.value = false
    }

    fun reject(request: AdminAccessRequest) = action("reject") { repository.rejectRequest(request.id) }
    fun setRole(user: AdminManagedUser, role: String) = action("role") { repository.setRole(user.id, role) }
    fun setStatus(user: AdminManagedUser, active: Boolean) = action("status") { repository.setStatus(user.id, active) }
    fun delete(user: AdminManagedUser) = action("delete") { repository.deleteUser(user.id) }
    fun setPermissions(user: AdminManagedUser, permissions: List<String>) =
        action("permissions") { repository.setPermissions(user.id, permissions) }

    private fun action(label: String, operation: suspend () -> Result<Unit>) = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        _message.value = null
        operation().onSuccess {
            _message.value = "Administrator $label updated."
            refresh()
        }.onFailure {
            _error.value = it.message ?: "Unable to update administrator."
        }
        _loading.value = false
    }

    fun clearMessage() { _message.value = null }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AdminUsersViewModel(application) as T
    }
}

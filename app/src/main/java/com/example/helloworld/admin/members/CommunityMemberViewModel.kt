package com.example.helloworld.admin.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityMemberViewModel : ViewModel() {
    private val repository = CommunityMemberRepository()

    private val _members = MutableStateFlow<List<CommunityMember>>(emptyList())
    val members: StateFlow<List<CommunityMember>> = _members.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.getMembers()
                .onSuccess { _members.value = it }
                .onFailure { _error.value = it.message ?: "Unable to load community members." }
            _loading.value = false
        }
    }

    fun updateMember(userId: String, username: String, displayName: String, bio: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            repository.updateMember(userId, username, displayName, bio)
                .onSuccess {
                    _message.value = "Member updated successfully."
                    refresh()
                    onComplete(true)
                }
                .onFailure {
                    _error.value = it.message ?: "Unable to update member."
                    onComplete(false)
                }
            _loading.value = false
        }
    }

    fun updateRole(userId: String, username: String, role: String, permissions: List<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            repository.updateMemberRole(userId, username, role, permissions)
                .onSuccess {
                    _message.value = "Member role & admin access updated successfully."
                    refresh()
                    onComplete(true)
                }
                .onFailure {
                    _error.value = it.message ?: "Unable to update member role."
                    onComplete(false)
                }
            _loading.value = false
        }
    }

    fun deleteMember(userId: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.deleteMember(userId)
                .onSuccess {
                    _message.value = "Member deleted successfully."
                    refresh()
                }
                .onFailure {
                    _error.value = it.message ?: "Unable to delete member."
                }
            _loading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
        _error.value = null
    }
}

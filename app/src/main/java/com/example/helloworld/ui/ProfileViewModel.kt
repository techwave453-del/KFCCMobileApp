package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.ChatProfile
import com.example.helloworld.data.ProfileRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _profile = MutableStateFlow<ChatProfile?>(null)
    val profile: StateFlow<ChatProfile?> = _profile.asStateFlow()

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _email.value = com.example.helloworld.data.SupabaseProvider.client.auth.currentUserOrNull()?.email
            repository.getProfile()
                .onSuccess { _profile.value = it }
                .onFailure { _error.value = it.message ?: "Unable to load your profile." }
            _loading.value = false
        }
    }

    fun save(username: String, displayName: String, avatarUrl: String) {
        viewModelScope.launch {
            _saving.value = true
            _saved.value = false
            _error.value = null
            repository.updateProfile(
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl,
            )
                .onSuccess {
                    _profile.value = it
                    _saved.value = true
                }
                .onFailure { _error.value = it.message ?: "Unable to save your profile." }
            _saving.value = false
        }
    }

    fun clearMessage() {
        _error.value = null
        _saved.value = false
    }
}

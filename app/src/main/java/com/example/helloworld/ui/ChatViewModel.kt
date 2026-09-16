package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.ChatMessage
import com.example.helloworld.data.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the complete community-chat lifecycle.
 *
 * The app talks directly to Supabase. The ViewModel loads the community room,
 * joins the authenticated member to it, loads recent messages and keeps the
 * conversation fresh while the screen is open.
 */
class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val authRepository = ChatAuthRepository()

    private val _signedIn = MutableStateFlow(authRepository.isSignedIn())
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    private var refreshJob: Job? = null
    private var initialized = false

    fun currentUserId(): String? = authRepository.currentUserId()

    fun onSignedIn() {
        _signedIn.value = authRepository.isSignedIn()
        if (_signedIn.value) initChat(force = true)
    }

    fun initChat(force: Boolean = false) {
        if (!_signedIn.value && authRepository.isSignedIn()) {
            _signedIn.value = true
        }
        if (!_signedIn.value) return
        if (initialized && !force) return

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            initialized = false
            _loading.value = true
            _error.value = null

            try {
                // Keep membership current before resolving the room.
                repository.joinCommunity().getOrThrow()
                val room = repository.getCommunityRoom().getOrThrow()
                _roomId.value = room.id
                loadMessages(showLoading = false)
                initialized = true

                // Supabase Realtime is backed by the same direct database path.
                // Polling is intentionally retained as a reliable fallback so
                // chat remains functional when Realtime replication is disabled.
                while (true) {
                    delay(5000)
                    loadMessages(showLoading = false, reportError = false)
                }
            } catch (e: Exception) {
                _error.value = readableError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadMessages(
        showLoading: Boolean,
        reportError: Boolean = true,
    ) {
        if (showLoading) _loading.value = true
        val id = _roomId.value ?: return

        repository.getMessages(id)
            .onSuccess { loaded ->
                _messages.value = loaded
                if (reportError) _error.value = null
            }
            .onFailure { error ->
                if (reportError) _error.value = readableError(error)
            }

        if (showLoading) _loading.value = false
    }

    fun sendMessage(text: String) {
        val id = _roomId.value
        val message = text.trim()
        if (id == null || message.isEmpty() || _sending.value) return

        viewModelScope.launch {
            _sending.value = true
            _error.value = null
            repository.sendMessage(id, message)
                .onSuccess {
                    loadMessages(showLoading = false)
                }
                .onFailure { error ->
                    _error.value = readableError(error)
                }
            _sending.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun readableError(error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        return when {
            raw.isBlank() -> "Unable to load the community chat. Please try again."
            raw.contains("JWT", ignoreCase = true) || raw.contains("token", ignoreCase = true) ->
                "Your chat session has expired. Please sign in again."
            raw.contains("permission", ignoreCase = true) || raw.contains("row-level", ignoreCase = true) ->
                "You do not currently have permission to use community chat."
            else -> raw
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}

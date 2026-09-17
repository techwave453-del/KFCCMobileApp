package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.*
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val authRepository = ChatAuthRepository()
    private val chatRepository = ChatRepository()

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

    init {
        if (signedIn.value) {
            initChat()
        }
    }

    fun initChat() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            chatRepository.joinCommunity()
                .onSuccess { id ->
                    _roomId.value = id
                    loadMessages(id)
                    observeMessages(id)
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    private fun loadMessages(roomId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(roomId)
                .onSuccess { _messages.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    private fun observeMessages(roomId: String) {
        viewModelScope.launch {
            chatRepository.observeMessages(roomId).collect { action ->
                when (action) {
                    is PostgresAction.Insert,
                    is PostgresAction.Update,
                    is PostgresAction.Delete -> {
                        loadMessages(roomId)
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val id = roomId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _sending.value = true
            chatRepository.sendMessage(id, text)
                .onFailure { _error.value = it.message }
            _sending.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _signedIn.value = false
            _messages.value = emptyList()
            _roomId.value = null
        }
    }

    fun onSignedIn() {
        _signedIn.value = true
        initChat()
    }
    
    fun currentUserId(): String? = authRepository.currentUserId()

    fun clearError() { _error.value = null }
}

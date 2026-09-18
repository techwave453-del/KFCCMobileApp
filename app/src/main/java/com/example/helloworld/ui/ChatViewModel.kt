package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _rooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val rooms: StateFlow<List<ChatRoom>> = _rooms.asStateFlow()

    private val _discoverableGroups = MutableStateFlow<List<ChatRoom>>(emptyList())
    val discoverableGroups: StateFlow<List<ChatRoom>> = _discoverableGroups.asStateFlow()

    private val _joinRequests = MutableStateFlow<Map<String, ChatGroupJoinRequest>>(emptyMap())
    val joinRequests: StateFlow<Map<String, ChatGroupJoinRequest>> = _joinRequests.asStateFlow()

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo.asStateFlow()

    private var observeJob: Job? = null

    init {
        // Automatically sign in if the Supabase client already has a session
        if (authRepository.isSignedIn()) {
            initChat()
        }
    }

    fun initChat() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            // If signed in but user info is missing (common for new admin sessions), 
            // proactively fetch user details from Supabase Auth.
            if (authRepository.isSignedIn()) {
                try {
                    SupabaseProvider.client.auth.retrieveUserForCurrentSession()
                } catch (_: Exception) {}
            }

            chatRepository.joinCommunity()
                .onSuccess { id ->
                    _roomId.value = id
                    loadMessages(id)
                    observeMessages(id)
                }
                .onFailure { _error.value = it.message }
            loadRooms()
            loadDiscoverableGroups()
            _loading.value = false
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            chatRepository.getRooms()
                .onSuccess { _rooms.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun loadDiscoverableGroups() {
        viewModelScope.launch {
            chatRepository.getDiscoverableGroups()
                .onSuccess { groups ->
                    _discoverableGroups.value = groups
                    groups.filter { group -> _rooms.value.none { it.id == group.id } }
                        .forEach { loadJoinRequest(it.id) }
                }
                .onFailure { _error.value = it.message }
        }
    }

    private fun loadJoinRequest(roomId: String) {
        viewModelScope.launch {
            chatRepository.getMyJoinRequest(roomId)
                .onSuccess { request ->
                    if (request != null) _joinRequests.value = _joinRequests.value + (roomId to request)
                }
        }
    }

    fun joinGroup(roomId: String) {
        viewModelScope.launch {
            _loading.value = true
            chatRepository.joinGroup(roomId)
                .onSuccess { loadRooms(); selectRoom(roomId); loadDiscoverableGroups() }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun requestGroupJoin(roomId: String) {
        viewModelScope.launch {
            _loading.value = true
            chatRepository.requestGroupJoin(roomId)
                .onSuccess { loadJoinRequest(roomId) }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun selectRoom(id: String) {
        if (_roomId.value == id) return
        _roomId.value = id
        _messages.value = emptyList()
        loadMessages(id)
        observeMessages(id)
    }

    private fun loadMessages(roomId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(roomId)
                .onSuccess { _messages.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    private fun observeMessages(roomId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
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
            val replyId = _replyingTo.value?.id
            chatRepository.sendMessage(id, text, replyId)
                .onSuccess { _replyingTo.value = null }
                .onFailure { _error.value = it.message }
            _sending.value = false
        }
    }

    fun setReplyingTo(message: ChatMessage?) {
        _replyingTo.value = message
    }

    fun editMessage(messageId: String, text: String) {
        viewModelScope.launch {
            chatRepository.editMessage(messageId, text)
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
                .onFailure { _error.value = it.message }
        }
    }

    fun createGroup(title: String) {
        viewModelScope.launch {
            _loading.value = true
            chatRepository.createGroup(title)
                .onSuccess { newRoom ->
                    loadRooms()
                    selectRoom(newRoom.id)
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
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

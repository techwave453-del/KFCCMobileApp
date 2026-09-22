package com.example.helloworld.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = ChatAuthRepository()
    private val chatRepository = ChatRepository()
    private val context = application.applicationContext

    private val _signedIn = MutableStateFlow(authRepository.isSignedIn())
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = roomId
        .flatMapLatest { id ->
            if (id != null) chatRepository.getLocalMessages(id, context)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val rooms: StateFlow<List<ChatRoom>> = chatRepository.getLocalRooms(context)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo.asStateFlow()

    private var observeJob: Job? = null

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
    }

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
            _loading.value = false
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            chatRepository.getRooms()
                .onSuccess { chatRepository.syncRoomsToLocal(it, context) }
                .onFailure { _error.value = it.message }
        }
    }

    fun selectRoom(id: String) {
        if (_roomId.value == id) return
        _roomId.value = id
        loadMessages(id)
        observeMessages(id)
    }

    private fun loadMessages(roomId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(roomId)
                .onSuccess { chatRepository.syncMessagesToLocal(roomId, it, context) }
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
            val myId = currentUserId() ?: ""
            chatRepository.saveMessageOffline(id, text, myId, context)
            _replyingTo.value = null
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

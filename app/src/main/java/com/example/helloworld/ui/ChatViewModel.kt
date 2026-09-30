package com.example.helloworld.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
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
    private val kanisaAssistantRepository = KanisaAssistantRepository()
    private val context = application.applicationContext

    private val _assistantMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val assistantMessages: StateFlow<Map<String, List<ChatMessage>>> = _assistantMessages.asStateFlow()

    private val _signedIn = MutableStateFlow(authRepository.isSignedIn())
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = combine(
        roomId.flatMapLatest { id -> if (id != null) chatRepository.getLocalMessages(id, context) else flowOf(emptyList()) },
        combine(roomId, assistantMessages) { id, all -> if (id != null) all[id].orEmpty() else emptyList() }
    ) { local, assistant ->
        (local + assistant)
            .distinctBy { it.id }
            .sortedWith(
            compareBy<ChatMessage> { message ->
                try {
                    java.time.Instant.parse(message.createdAt).toEpochMilli()
                } catch (_: Exception) {
                    Long.MAX_VALUE
                }
            }.thenBy { it.id }
        )
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
    private var kanisaObserveJob: Job? = null
    private var sessionInitialized = false

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
    }

    init {
        // Supabase restores the persisted session asynchronously from Android storage.
        // Listen to sessionStatus so a cold app start does not appear signed out
        // while Auth is still loading the saved session.
        viewModelScope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _signedIn.value = true
                        if (!sessionInitialized) {
                            sessionInitialized = true
                            initChat()
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        sessionInitialized = false
                        _signedIn.value = false
                        _roomId.value = null
                        observeJob?.cancel()
                        kanisaObserveJob?.cancel()
                    }
                    is SessionStatus.Initializing,
                    is SessionStatus.RefreshFailure -> {
                        // Do not clear the current session during storage loading
                        // or a temporary token-refresh failure.
                    }
                }
            }
        }

        // Covers a session that was already restored before the collector started.
        if (authRepository.isSignedIn()) {
            _signedIn.value = true
            sessionInitialized = true
            initChat()
        }
    }

    fun initChat() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            if (!authRepository.isSignedIn()) {
                _signedIn.value = false
                _loading.value = false
                return@launch
            }

            // Do not retrieve the Auth user here. Imported administrator sessions
            // already have a valid JWT, and ChatAuthRepository can resolve the user
            // ID from the session/JWT. Keeping chat startup free of user/session
            // refresh operations prevents concurrent refresh-token rotation with
            // AdminViewModel during administrator sign-in.

            chatRepository.joinCommunity()
                .onSuccess { id ->
                    _roomId.value = id
                    loadMessages(id)
                    observeMessages(id)
                    observeKanisaMessages(id)
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
        observeKanisaMessages(id)
    }

    private fun loadMessages(roomId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(roomId)
                .onSuccess { chatRepository.syncMessagesToLocal(roomId, it, context) }
                .onFailure { _error.value = it.message }
            chatRepository.getKanisaRoomMessages(roomId)
                .onSuccess { stored ->
                    _assistantMessages.update { current ->
                        current + (roomId to stored.map { row ->
                            ChatMessage(
                                id = row.id,
                                roomId = row.roomId,
                                senderId = KANISA_ASSISTANT_ID,
                                message = row.message,
                                createdAt = row.createdAt,
                                replyToId = row.replyToMessageId,
                                bibleReferences = row.bibleReferences.distinct().take(6),
                                bibleQuotes = row.bibleQuotes.take(6),
                                senderProfile = ChatProfile(
                                    user_id = KANISA_ASSISTANT_ID,
                                    username = "Kanisa Assistant",
                                    display_name = "AI Bible & Church Assistant",
                                    is_admin_visible = false
                                )
                            )
                        }.takeLast(50))
                    }
                }
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
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        viewModelScope.launch {
            val myId = currentUserId() ?: ""
            val localMessageId = chatRepository.saveMessageOffline(id, cleanText, myId, context)
            _replyingTo.value = null
            if (isDirectedToKanisa(cleanText)) {
                askKanisaInRoom(id, cleanText, localMessageId)
            }
        }
    }

    private fun isDirectedToKanisa(text: String): Boolean {
        val normalized = text.lowercase().trim()
        return normalized.contains("@kanisa") ||
            Regex("\\bkanisa(?: assistant)?\\b").containsMatchIn(normalized)
    }

    private fun askKanisaInRoom(
        roomId: String,
        originalText: String,
        replyToMessageId: String
    ) {
        viewModelScope.launch {
            val prompt = originalText
                .replace(Regex("@kanisa(?:\\s+assistant)?", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\bkanisa(?:\\s+assistant)?\\b", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { "Please respond to my message in this chat." }

            kanisaAssistantRepository.ask(
                message = prompt,
                roomId = roomId,
                replyToMessageId = replyToMessageId
            ).onSuccess { response ->
                addKanisaMessage(
                    roomId = roomId,
                    messageId = response.room_message_id ?: ("kanisa-" + System.currentTimeMillis()),
                    text = response.answer.normalizeKanisaLineBreaks(),
                    username = response.assistant_name.ifBlank { "Kanisa Assistant" },
                    bibleReferences = response.bible_references,
                    bibleQuotes = response.bible_quotes.map { quote -> KanisaBibleQuote(quote.reference, quote.text, quote.translation) },
                    replyToMessageId = replyToMessageId
                )
            }.onFailure { failure ->
                addKanisaMessage(
                    roomId = roomId,
                    messageId = "kanisa-" + System.currentTimeMillis(),
                    text = "I’m here, but I couldn't answer that right now. " + failure.message.orEmpty().trim(),
                    username = "Kanisa Assistant",
                    replyToMessageId = replyToMessageId
                )
            }
        }
    }

    private fun addKanisaMessage(
        roomId: String,
        messageId: String,
        text: String,
        username: String,
        bibleReferences: List<String> = emptyList(),
        bibleQuotes: List<KanisaBibleQuote> = emptyList(),
        replyToMessageId: String? = null
    ) {
        val assistantMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = KANISA_ASSISTANT_ID,
            message = text,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.getDefault()).format(java.util.Date()),
            replyToId = replyToMessageId,
            bibleReferences = bibleReferences.distinct().take(6),
            bibleQuotes = bibleQuotes.take(6),
            senderProfile = ChatProfile(
                user_id = KANISA_ASSISTANT_ID,
                username = username,
                display_name = "AI Bible & Church Assistant",
                is_admin_visible = false
            )
        )
        _assistantMessages.update { current ->
            current + (roomId to (current[roomId].orEmpty() + assistantMessage).takeLast(50))
        }
    }

    private fun String.normalizeKanisaLineBreaks(): String =
        replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")

    private fun observeKanisaMessages(roomId: String) {
        kanisaObserveJob?.cancel()
        kanisaObserveJob = viewModelScope.launch {
            chatRepository.observeKanisaMessages(roomId).collect {
                chatRepository.getKanisaRoomMessages(roomId)
                    .onSuccess { stored ->
                        _assistantMessages.update { current ->
                            current + (roomId to stored.map { row ->
                                ChatMessage(
                                    id = row.id,
                                    roomId = row.roomId,
                                    senderId = KANISA_ASSISTANT_ID,
                                    message = row.message,
                                    createdAt = row.createdAt,
                                    replyToId = row.replyToMessageId,
                                    bibleReferences = row.bibleReferences.distinct().take(6),
                                    bibleQuotes = row.bibleQuotes.take(6),
                                    senderProfile = ChatProfile(
                                        user_id = KANISA_ASSISTANT_ID,
                                        username = "Kanisa Assistant",
                                        display_name = "AI Bible & Church Assistant",
                                        is_admin_visible = false
                                    )
                                )
                            }.takeLast(50))
                        }
                    }
            }
        }
    }

    companion object {
        const val KANISA_ASSISTANT_ID = "kanisa-assistant"
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
            chatRepository.deleteMessage(messageId, context)
                .onSuccess {
                    // The Room cache is updated by the repository so the message
                    // disappears immediately instead of waiting for a realtime event.
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteAssistantMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteKanisaRoomMessage(messageId)
                .onSuccess {
                    _assistantMessages.update { current ->
                        current.mapValues { (_, list) -> list.filterNot { it.id == messageId } }
                    }
                }
                .onFailure { _error.value = it.message ?: "Unable to delete the assistant message." }
        }
    }

    fun clearChatMessages() {
        val id = roomId.value ?: return
        val myId = currentUserId() ?: return
        viewModelScope.launch {
            val normalResult = chatRepository.clearMyMessages(id, myId, context)
            val kanisaResult = chatRepository.clearMyKanisaRoomMessages(id, myId)

            if (normalResult.isSuccess && kanisaResult.isSuccess) {
                _assistantMessages.update { current -> current - id }
                loadMessages(id)
            } else {
                _error.value = normalResult.exceptionOrNull()?.message
                    ?: kanisaResult.exceptionOrNull()?.message
                    ?: "Unable to clear your messages."
            }
        }
    }

    fun clearMyMessages() {
        val id = roomId.value ?: return
        val myId = currentUserId() ?: return
        viewModelScope.launch {
            val normalResult = chatRepository.clearMyMessages(id, myId, context)
            val kanisaResult = chatRepository.clearMyKanisaRoomMessages(id, myId)

            if (normalResult.isSuccess && kanisaResult.isSuccess) {
                _assistantMessages.update { current -> current - id }
                loadMessages(id)
            } else {
                _error.value = normalResult.exceptionOrNull()?.message
                    ?: kanisaResult.exceptionOrNull()?.message
                    ?: "Unable to clear your messages."
            }
        }
    }

    fun createGroup(title: String, isAdmin: Boolean) {
        if (!isAdmin) {
            _error.value = "Only administrators can create community groups."
            return
        }
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
            sessionInitialized = false
            observeJob?.cancel()
            kanisaObserveJob?.cancel()
        }
    }

    fun onSignedIn() {
        _signedIn.value = true
        initChat()
    }
    
    fun currentUserId(): String? = authRepository.currentUserId()

    fun clearError() { _error.value = null }
}

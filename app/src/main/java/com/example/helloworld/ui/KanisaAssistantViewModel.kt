package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.KanisaAssistantBibleQuote
import com.example.helloworld.data.KanisaAssistantMessage
import com.example.helloworld.data.KanisaAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KanisaAssistantUiMessage(
    val id: Long,
    val role: String,
    val content: String,
    val bibleReferences: List<String> = emptyList(),
    val bibleQuotes: List<KanisaAssistantBibleQuote> = emptyList()
)

class KanisaAssistantViewModel : ViewModel() {
    private val repository = KanisaAssistantRepository()

    private val _messages = MutableStateFlow(
        listOf(
            KanisaAssistantUiMessage(
                0L,
                "assistant",
                "Hello! 👋 I’m Kanisa Assistant. I can help with church information, services, events and Bible questions."
            )
        )
    )
    val messages: StateFlow<List<KanisaAssistantUiMessage>> = _messages.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var nextId = 1L

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.loadHistory()
                .onSuccess { stored ->
                    val restored = stored.map { row ->
                        KanisaAssistantUiMessage(
                            id = row.id,
                            role = row.role,
                            content = row.content,
                            bibleReferences = row.bible_references,
                            bibleQuotes = row.bible_quotes
                        )
                    }
                    _messages.value = listOf(
                        KanisaAssistantUiMessage(
                            0L,
                            "assistant",
                            "Hello! 👋 I’m Kanisa Assistant. I can help with church information, services, events and Bible questions."
                        )
                    ) + restored
                    nextId = (restored.maxOfOrNull { it.id } ?: 0L) + 1L
                }
        }
    }

    fun ask(text: String, onComplete: (Boolean) -> Unit = {}) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _sending.value) return

        val userMessageId = nextId++
        _messages.value = _messages.value + KanisaAssistantUiMessage(userMessageId, "user", clean)
        _sending.value = true
        _error.value = null

        viewModelScope.launch {
            repository.saveMessage("user", clean)
                .onFailure {
                    _messages.value = _messages.value.filterNot { it.id == userMessageId }
                    _error.value = it.message ?: "Unable to save your message."
                    _sending.value = false
                    onComplete(false)
                    return@launch
                }

            val history = _messages.value
                .dropLast(1)
                .takeLast(8)
                .map { KanisaAssistantMessage(it.role, it.content) }

            repository.ask(clean, history)
                .onSuccess { response ->
                    val answer = response.answer.normalizeAssistantText()
                    val references = response.bible_references.distinct().take(6)
                    val quotes = response.bible_quotes.distinctBy { it.reference }.take(6)
                    val assistantId = nextId++
                    _messages.value = _messages.value + KanisaAssistantUiMessage(
                        assistantId,
                        "assistant",
                        answer,
                        references,
                        quotes
                    )
                    repository.saveMessage("assistant", answer, references, quotes)
                        .onFailure { _error.value = "The reply was received, but could not be saved." }
                    onComplete(true)
                }
                .onFailure {
                    _error.value = it.message ?: "Unable to reach Kanisa Assistant."
                    onComplete(false)
                }

            _sending.value = false
        }
    }

    fun deleteMessage(id: Long) {
        if (id == 0L) return
        viewModelScope.launch {
            repository.deleteMessage(id)
                .onSuccess {
                    _messages.value = _messages.value.filterNot { it.id == id }
                }
                .onFailure { _error.value = it.message ?: "Unable to delete the message." }
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            repository.clearHistory()
                .onSuccess {
                    _messages.value = listOf(
                        KanisaAssistantUiMessage(
                            0L,
                            "assistant",
                            "Hello! 👋 I’m Kanisa Assistant. I can help with church information, services, events and Bible questions."
                        )
                    )
                    nextId = 1L
                    _error.value = null
                }
                .onFailure { _error.value = it.message ?: "Unable to clear assistant history." }
        }
    }

    private fun String.normalizeAssistantText(): String =
        replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    fun clearError() {
        _error.value = null
    }
}

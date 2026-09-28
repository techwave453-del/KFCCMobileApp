package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val bibleReferences: List<String> = emptyList()
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

    fun ask(text: String, onComplete: (Boolean) -> Unit = {}) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _sending.value) return

        _messages.value = _messages.value + KanisaAssistantUiMessage(nextId++, "user", clean)
        _sending.value = true
        _error.value = null

        viewModelScope.launch {
            val history = _messages.value
                .dropLast(1)
                .takeLast(8)
                .map { KanisaAssistantMessage(it.role, it.content) }

            repository.ask(clean, history)
                .onSuccess { response ->
                    _messages.value = _messages.value + KanisaAssistantUiMessage(
                        nextId++,
                        "assistant",
                        response.answer.normalizeAssistantText(),
                        response.bible_references.distinct().take(6)
                    )
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
        _messages.value = _messages.value.filterNot { it.id == id }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _error.value = null
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

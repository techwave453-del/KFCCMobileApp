package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.BibleBookRecord
import com.example.helloworld.data.BibleRepository
import com.example.helloworld.data.BibleTranslation
import com.example.helloworld.data.BibleVerseRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BibleUiState(
    val translations: List<BibleTranslation> = emptyList(),
    val books: List<BibleBookRecord> = emptyList(),
    val selectedTranslationId: String = "kjv",
    val chapter: List<BibleVerseRecord> = emptyList(),
    val searchResults: List<BibleVerseRecord> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class BibleViewModel(
    private val repository: BibleRepository = BibleRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(BibleUiState())
    val state: StateFlow<BibleUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.loading || _state.value.books.isNotEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val translations = repository.getTranslations()
                val books = repository.getBooks()
                val selected = translations.firstOrNull { it.id == _state.value.selectedTranslationId }
                    ?: translations.firstOrNull()
                _state.value = _state.value.copy(
                    translations = translations,
                    books = books,
                    selectedTranslationId = selected?.id ?: _state.value.selectedTranslationId,
                    loading = false
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = error.message ?: "Unable to load the Bible."
                )
            }
        }
    }

    fun selectTranslation(id: String) {
        _state.value = _state.value.copy(
            selectedTranslationId = id,
            chapter = emptyList(),
            searchResults = emptyList()
        )
    }

    fun loadChapter(bookId: String, chapter: Int) {
        val translationId = _state.value.selectedTranslationId
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                repository.getChapter(translationId, bookId, chapter)
            }.onSuccess { verses ->
                _state.value = _state.value.copy(
                    chapter = verses,
                    loading = false
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = error.message ?: "Unable to load this chapter."
                )
            }
        }
    }

    fun search(query: String) {
        val translationId = _state.value.selectedTranslationId
        if (query.trim().isEmpty()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.search(translationId, query)
            }.onSuccess { results ->
                _state.value = _state.value.copy(searchResults = results)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: "Unable to search the Bible."
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

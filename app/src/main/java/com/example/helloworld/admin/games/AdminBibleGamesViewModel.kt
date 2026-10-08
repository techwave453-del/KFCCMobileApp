package com.example.helloworld.admin.games

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.admin.BibleGameQuestionAdmin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminBibleGamesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminRepositoryProvider.get(application)
    private val _questions = MutableStateFlow<List<BibleGameQuestionAdmin>>(emptyList())
    val questions: StateFlow<List<BibleGameQuestionAdmin>> = _questions.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            repository.getBibleGameQuestions()
                .onSuccess { _questions.value = it; _error.value = null }
                .onFailure { _error.value = it.message ?: "Unable to load Bible Games." }
            _loading.value = false
        }
    }

    fun save(question: BibleGameQuestionAdmin) {
        viewModelScope.launch {
            _saving.value = true
            _message.value = null
            _error.value = null
            val normalized = question.copy(
                question = question.question.trim(),
                options = question.options.map(String::trim).filter(String::isNotBlank),
                explanation = question.explanation.trim(),
                reference = question.reference.trim()
            )
            runCatching {
                require(normalized.question.isNotBlank()) { "Question is required." }
                require(normalized.options.size >= 2) { "At least two answer options are required." }
                require(normalized.correctAnswerIndex in normalized.options.indices) { "Select a valid correct answer." }
                if (normalized.id.isBlank()) repository.createBibleGameQuestion(normalized).getOrThrow()
                else repository.updateBibleGameQuestion(normalized).getOrThrow()
            }.onSuccess {
                _message.value = if (normalized.id.isBlank()) "Question created." else "Question updated."
                refresh()
            }.onFailure { _error.value = it.message ?: "Unable to save the question." }
            _saving.value = false
        }
    }

    fun togglePublished(question: BibleGameQuestionAdmin) {
        viewModelScope.launch {
            repository.setBibleGameQuestionPublished(question.id, !question.isPublished)
                .onSuccess { _message.value = if (!question.isPublished) "Question published." else "Question unpublished."; refresh() }
                .onFailure { _error.value = it.message ?: "Unable to change publication status." }
        }
    }

    fun delete(question: BibleGameQuestionAdmin) {
        viewModelScope.launch {
            repository.deleteBibleGameQuestion(question.id)
                .onSuccess { _message.value = "Question deleted."; refresh() }
                .onFailure { _error.value = it.message ?: "Unable to delete the question." }
        }
    }

    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AdminBibleGamesViewModel(application) as T
    }
}

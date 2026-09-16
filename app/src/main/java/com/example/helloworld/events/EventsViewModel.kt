package com.example.helloworld.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.getPublicEvents().onSuccess { _events.value = it }.onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
}

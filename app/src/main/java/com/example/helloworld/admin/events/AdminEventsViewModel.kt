package com.example.helloworld.admin.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.events.Event
import com.example.helloworld.events.EventInput
import com.example.helloworld.events.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminEventsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventsRepository(AdminRepositoryProvider.get(application))
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        repository.getAdminEvents().onSuccess { _events.value = it }.onFailure { _error.value = it.message }
        _loading.value = false
    }

    fun save(existing: Event?, input: EventInput) = viewModelScope.launch {
        _saving.value = true
        _error.value = null
        _saved.value = false
        val result = if (existing == null) repository.create(input) else repository.update(existing.id, input)
        result.onSuccess { updated ->
            _events.value = if (existing == null) _events.value + updated else _events.value.map { if (it.id == updated.id) updated else it }
            _saved.value = true
        }.onFailure { _error.value = it.message }
        _saving.value = false
    }

    fun delete(event: Event) = viewModelScope.launch {
        _error.value = null
        repository.delete(event.id).onSuccess { _events.value = _events.value.filterNot { it.id == event.id } }.onFailure { _error.value = it.message }
    }

    fun clearMessage() { _error.value = null; _saved.value = false }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminEventsViewModel(application) as T
    }
}

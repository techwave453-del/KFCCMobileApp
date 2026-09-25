package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.*
import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.events.Event
import com.example.helloworld.events.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChurchViewModel : ViewModel() {
    private val repository = ChurchRepository()
    private val contentRepository by lazy {
        KfccContentRepository(KfccDatabase.getInstance(KfccDataContext.appContext))
    }
    private val eventsRepository = EventsRepository()

    private val _churchInfo = MutableStateFlow<ChurchInfo>(ChurchContent.default)
    val churchInfo: StateFlow<ChurchInfo> = _churchInfo.asStateFlow()
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        observeCachedContent()
        refreshData()
    }

    /**
     * Room is the UI source of truth. Supabase sync workers update Room and
     * these streams refresh the UI automatically without another screen fetch.
     */
    private fun observeCachedContent() {
        viewModelScope.launch {
            contentRepository.observeChurchInfo().collect { _churchInfo.value = it }
        }
        viewModelScope.launch {
            contentRepository.observeMedia().collect { _mediaItems.value = it }
        }
        viewModelScope.launch {
            eventsRepository.observePublicEvents().collect { _events.value = it }
        }
    }

    /**
     * Seed an empty cache from Supabase. If cached data already exists, the
     * call returns immediately and the periodic worker remains responsible
     * for refreshing it.
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                repository.getSiteContent()
                repository.getMedia()
                eventsRepository.getPublicEvents()
            } catch (_: Exception) {
                // Cached/default data remains available while offline.
            }
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val response = repository.login(username, password)
            if (response.ok && response.user != null) {
                _currentUser.value = response.user
                onSuccess()
            } else {
                _loginError.value = response.error ?: "Invalid credentials"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
        }
    }

    fun updateSiteContent(content: ChurchInfo, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateSiteContent(content)
            if (success) refreshData()
            onComplete(success)
            _isLoading.value = false
        }
    }

    fun uploadMedia(
        title: String,
        description: String,
        category: String,
        type: String,
        fileBytes: ByteArray,
        fileName: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.uploadMedia(title, description, category, type, fileBytes, fileName)
            if (success) refreshData()
            onComplete(success)
            _isLoading.value = false
        }
    }
}

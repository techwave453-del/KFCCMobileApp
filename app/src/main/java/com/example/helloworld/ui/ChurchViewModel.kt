package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.*
import com.example.helloworld.events.Event
import com.example.helloworld.events.EventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChurchViewModel : ViewModel() {
    private val repository = ChurchRepository()
    private val eventsRepository = EventsRepository()

    private val _churchInfo = MutableStateFlow<ChurchInfo>(ChurchContent.default)
    val churchInfo: StateFlow<ChurchInfo> = _churchInfo.asStateFlow()
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()
    // This state is reserved for interactive/admin operations. Public startup
    // must not block on Supabase/network availability.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init { refreshData() }

    /**
     * Refresh public data in the background. The UI already has safe local
     * defaults, so a slow/offline Supabase connection never blocks app startup.
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                _churchInfo.value = repository.getSiteContent()
                _mediaItems.value = repository.getMedia()
                eventsRepository.getPublicEvents().onSuccess { _events.value = it }
            } catch (_: Exception) {
                // Keep the current/default public state when the network is unavailable.
            }
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true; _loginError.value = null
            val response = repository.login(username, password)
            if (response.ok && response.user != null) { _currentUser.value = response.user; onSuccess() }
            else _loginError.value = response.error ?: "Invalid credentials"
            _isLoading.value = false
        }
    }

    fun logout() { viewModelScope.launch { repository.logout(); _currentUser.value = null } }

    fun updateSiteContent(content: ChurchInfo, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateSiteContent(content)
            if (success) refreshData()
            onComplete(success); _isLoading.value = false
        }
    }

    fun uploadMedia(title: String, description: String, category: String, type: String, fileBytes: ByteArray, fileName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.uploadMedia(title, description, category, type, fileBytes, fileName)
            if (success) refreshData()
            onComplete(success); _isLoading.value = false
        }
    }
}

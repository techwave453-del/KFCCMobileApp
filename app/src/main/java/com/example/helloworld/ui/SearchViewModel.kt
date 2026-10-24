package com.example.helloworld.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.ChurchRepository
import com.example.helloworld.data.ChurchService
import com.example.helloworld.data.MediaItem
import com.example.helloworld.events.EventsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

sealed class SearchItem {
    data class Media(val item: MediaItem) : SearchItem()
    data class Event(val item: com.example.helloworld.events.Event) : SearchItem()
    data class Service(val item: ChurchService) : SearchItem()
}

class SearchViewModel : ViewModel() {
    private val churchRepository = ChurchRepository()
    private val eventsRepository = EventsRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults = _query
        .debounce(500)
        .filter { it.length >= 2 }
        .map { q ->
            _isSearching.value = true
            val results = performSearch(q)
            _isSearching.value = false
            results
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    private suspend fun performSearch(q: String): List<SearchItem> {
        val media = churchRepository.getMedia()
        val events = eventsRepository.getPublicEvents().getOrDefault(emptyList())
        val churchInfo = churchRepository.getSiteContent()
        
        val filteredMedia = media.filter { 
            it.title.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) 
        }.map { SearchItem.Media(it) }
        
        val filteredEvents = events.filter { 
            it.title.contains(q, ignoreCase = true) || it.short_description.contains(q, ignoreCase = true) 
        }.map { SearchItem.Event(it) }
        
        val filteredServices = churchInfo.services.filter { 
            it.title.contains(q, ignoreCase = true) 
        }.map { SearchItem.Service(it) }
        
        return filteredMedia + filteredEvents + filteredServices
    }
}

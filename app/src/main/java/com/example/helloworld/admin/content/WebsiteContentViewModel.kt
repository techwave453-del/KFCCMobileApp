package com.example.helloworld.admin.content

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChurchInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebsiteContentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WebsiteContentRepository()
    private val legacyRepository = AdminRepository(application)
    private val _content = MutableStateFlow(ChurchInfo())
    val content: StateFlow<ChurchInfo> = _content.asStateFlow()
    private val _pages = MutableStateFlow<List<CmsPage>>(emptyList())
    val pages: StateFlow<List<CmsPage>> = _pages.asStateFlow()
    private val _sections = MutableStateFlow<List<CmsSection>>(emptyList())
    val sections: StateFlow<List<CmsSection>> = _sections.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()
    private val _selectedPageId = MutableStateFlow<Long?>(null)
    val selectedPageId: StateFlow<Long?> = _selectedPageId.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        legacyRepository.loadSiteContent()
            .onSuccess { _content.value = it }
            .onFailure { _error.value = it.message ?: "Unable to load website content." }
        repository.loadPages()
            .onSuccess { pages ->
                _pages.value = pages
                val selected = _selectedPageId.value?.let { id -> pages.firstOrNull { it.id == id } }
                    ?: pages.firstOrNull()
                _selectedPageId.value = selected?.id
                if (selected != null) loadSections(selected.id)
            }
            .onFailure { _error.value = it.message ?: "Unable to load CMS pages." }
        _loading.value = false
    }

    fun selectPage(page: CmsPage) {
        if (_selectedPageId.value == page.id) return
        _selectedPageId.value = page.id
        loadSections(page.id)
    }

    private fun loadSections(pageId: Long) = viewModelScope.launch {
        _loading.value = true
        repository.loadSections(pageId)
            .onSuccess { _sections.value = it }
            .onFailure { _error.value = it.message ?: "Unable to load page sections." }
        _loading.value = false
    }

    fun update(content: ChurchInfo) {
        _content.value = content
        _saved.value = false
    }

    fun save() = viewModelScope.launch {
        _saving.value = true
        _error.value = null
        _saved.value = false
        legacyRepository.saveSiteContent(_content.value)
            .onSuccess { _content.value = it; _saved.value = true }
            .onFailure { _error.value = it.message ?: "Unable to save website content." }
        _saving.value = false
    }

    fun saveSection(section: CmsSection, heading: String, body: String, media: String, eyebrow: String) = viewModelScope.launch {
        _saving.value = true; _error.value = null; _saved.value = false
        repository.updateSection(section.id, heading.trim(), body.trim(), media.trim(), eyebrow.trim())
            .onSuccess {
                _saved.value = true
                _selectedPageId.value?.let { loadSections(it) }
            }
            .onFailure { _error.value = it.message ?: "Unable to save section." }
        _saving.value = false
    }

    fun setPageStatus(page: CmsPage, status: String) = viewModelScope.launch {
        _saving.value = true; _error.value = null; _saved.value = false
        repository.updatePageStatus(page.id, status)
            .onSuccess { _saved.value = true; refresh() }
            .onFailure { _error.value = it.message ?: "Unable to update page status." }
        _saving.value = false
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WebsiteContentViewModel(application) as T
    }
}

package com.example.helloworld.admin.content

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebsiteContentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WebsiteContentRepository(AdminRepositoryProvider.get(application))
    private val _content = MutableStateFlow(ChurchContent.default)
    val content: StateFlow<ChurchInfo> = _content.asStateFlow()
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
        repository.load().onSuccess { _content.value = it }.onFailure { _error.value = it.message }
        _loading.value = false
    }

    fun update(value: ChurchInfo) { _content.value = value; _saved.value = false }

    fun save() = viewModelScope.launch {
        _saving.value = true; _error.value = null; _saved.value = false
        repository.save(_content.value).onSuccess { _content.value = it; _saved.value = true }.onFailure { _error.value = it.message }
        _saving.value = false
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WebsiteContentViewModel(application) as T
    }
}

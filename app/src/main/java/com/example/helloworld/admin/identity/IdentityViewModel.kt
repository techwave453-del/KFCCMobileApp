package com.example.helloworld.admin.identity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloworld.admin.AdminRepositoryProvider
import com.example.helloworld.data.offline.KfccContentSyncScheduler
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.SiteContentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentityViewModel(application: Application) : AndroidViewModel(application) {
    // Reuse the same authenticated admin client/session as AdminViewModel.
    private val repository = IdentityRepository(AdminRepositoryProvider.get(application))
    private val db = KfccDatabase.getInstance(application)
    private val _identity = MutableStateFlow(ChurchIdentity())
    val identity: StateFlow<ChurchIdentity> = _identity.asStateFlow()
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
        repository.load()
            .onSuccess { _identity.value = it }
            .onFailure { _error.value = it.message ?: "Unable to load Church Identity." }
        _loading.value = false
    }

    fun update(value: ChurchIdentity) {
        _identity.value = value
        _saved.value = false
    }

    fun save() = viewModelScope.launch {
        _saving.value = true
        _error.value = null
        _saved.value = false
        repository.save(_identity.value)
            .onSuccess {
                val savedIdentity = it
                _identity.value = savedIdentity
                _saved.value = true

                // Update the local source of truth immediately so the public
                // app reflects the new identity without waiting for the next
                // periodic WorkManager sync.
                db.siteContentDao().upsertAll(
                    listOf(
                        SiteContentEntity("churchName", savedIdentity.churchName),
                        SiteContentEntity("logoUrl", savedIdentity.logoUrl),
                        SiteContentEntity("officialLogo", savedIdentity.officialLogo)
                    )
                )

                // Also schedule an authoritative cloud -> Room reconciliation.
                KfccContentSyncScheduler.syncNow(getApplication<Application>())
            }
            .onFailure { _error.value = it.message ?: "Unable to save Church Identity." }
        _saving.value = false
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = IdentityViewModel(application) as T
    }
}

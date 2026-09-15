package com.example.helloworld.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdminMediaItem(
    val id: Long = 0,
    val title: String = "",
    val description: String? = null,
    val type: String = "",
    val url: String? = null,
    val thumbnail_url: String? = null,
    val featured: Boolean = false,
    val category: String? = null
)

private class MediaAdminRepository(context: Context) {
    companion object { private const val BASE = "https://kingdomfellowshipchristianchurch.onrender.com/" }
    private val prefs = context.getSharedPreferences("kfcc_admin_session", Context.MODE_PRIVATE)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true }) }
        install(HttpCookies)
    }
    private fun cookie(): String? = prefs.getString("session_cookie", null)
    private fun io.ktor.client.request.HttpRequestBuilder.session() {
        cookie()?.let { io.ktor.client.request.cookie(it.substringBefore('='), it.substringAfter('=')) }
    }
    suspend fun list(): List<AdminMediaItem> = client.get(BASE + "api/media") { session() }.body()
    suspend fun feature(id: Long, value: Boolean): Boolean {
        val response = client.patch(BASE + "api/media/$id/featured") {
            session(); contentType(ContentType.Application.Json); setBody(mapOf("featured" to value))
        }
        return response.status.value in 200..299
    }
}

class MediaAdminViewModel(private val repository: MediaAdminRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<AdminMediaItem>>(emptyList())
    val items: StateFlow<List<AdminMediaItem>> = _items.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try { _items.value = repository.list() } catch (e: Exception) { _error.value = e.message ?: "Unable to load Media Center." }
        _loading.value = false
    }
    fun setFeatured(item: AdminMediaItem, featured: Boolean) = viewModelScope.launch {
        if (item.type.lowercase() != "video") return@launch
        try {
            if (repository.feature(item.id, featured)) refresh() else _error.value = "Unable to update Featured Video."
        } catch (e: Exception) { _error.value = e.message ?: "Unable to update Featured Video." }
    }
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = MediaAdminViewModel(MediaAdminRepository(context)) as T
    }
}

package com.example.helloworld.admin.media

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.contentType
import io.ktor.client.request.cookie
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.plugins.contentnegotiation.json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class MediaRepository(context: Context) {
    companion object {
        private const val PREFS = "kfcc_admin_session"
        private const val COOKIE_KEY = "session_cookie"
        private const val BASE_URL = "https://kingdomfellowshipchristianchurch.onrender.com/"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
        install(HttpCookies)
    }

    private fun cookiePair(): Pair<String, String>? {
        val raw = prefs.getString(COOKIE_KEY, null) ?: return null
        val name = raw.substringBefore('=')
        val value = raw.substringAfter('=', "")
        return if (name.isNotBlank()) name to value else null
    }

    suspend fun load(): Result<List<AdminMediaItem>> = runCatching {
        val response = client.get(BASE_URL + "api/media") {
            cookiePair()?.let { (name, value) -> cookie(name, value) }
        }
        if (response.status != HttpStatusCode.OK) error("Media service returned ${response.status.value}.")
        response.body<List<AdminMediaItem>>()
    }

    suspend fun update(id: Long, request: MediaUpdateRequest): Result<AdminMediaItem> = runCatching {
        val response = client.patch(BASE_URL + "api/media/$id") {
            cookiePair()?.let { (name, value) -> cookie(name, value) }
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) error("Unable to save media (${response.status.value}).")
        response.body()
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        val response = client.delete(BASE_URL + "api/media/$id") {
            cookiePair()?.let { (name, value) -> cookie(name, value) }
        }
        if (!response.status.isSuccess()) error("Unable to delete media (${response.status.value}).")
    }
}

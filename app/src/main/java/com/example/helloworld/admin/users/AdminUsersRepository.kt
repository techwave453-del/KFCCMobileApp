package com.example.helloworld.admin.users

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.plugins.contentnegotiation.json
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class AdminUsersRepository(context: Context) {
    companion object {
        private const val BASE_URL = "https://kingdomfellowshipchristianchurch.onrender.com/"
        private const val PREFS = "kfcc_admin_session"
        private const val COOKIE_KEY = "session_cookie"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true }) }
        install(HttpCookies)
    }

    private fun addSession(builder: io.ktor.client.request.HttpRequestBuilder) {
        val raw = prefs.getString(COOKIE_KEY, null) ?: return
        val name = raw.substringBefore('=')
        val value = raw.substringAfter('=', "")
        if (name.isNotBlank()) builder.cookie(name, value)
    }

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val response = client.get(BASE_URL + "api/admin/users") { addSession(this) }
        if (response.status != HttpStatusCode.OK) error("Administrator service returned ${response.status.value}.")
        response.body()
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        val response = client.get(BASE_URL + "api/admin/access/requests") { addSession(this) }
        if (response.status != HttpStatusCode.OK) error("Approval service returned ${response.status.value}.")
        response.body()
    }
}

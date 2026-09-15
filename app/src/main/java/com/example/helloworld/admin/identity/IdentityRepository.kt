package com.example.helloworld.admin.identity

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class IdentityRepository(context: Context) {
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

    suspend fun load(): Result<ChurchIdentity> = runCatching {
        val response = client.get(BASE_URL + "api/site/content") { addSession(this) }
        if (response.status != HttpStatusCode.OK) error("Identity service returned ${response.status.value}.")
        response.body<ChurchIdentity>()
    }

    suspend fun save(identity: ChurchIdentity): Result<ChurchIdentity> = runCatching {
        val body = mapOf(
            "churchName" to identity.churchName.trim(),
            "officialName" to identity.officialName.trim(),
            "logo" to identity.logo.trim(),
            "logoUrl" to identity.logoUrl.trim(),
            "officialLogo" to identity.officialLogo.trim(),
            "registrationDetails" to identity.registrationDetails.trim()
        )
        val response = client.put(BASE_URL + "api/site/content") {
            addSession(this)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.Created)) {
            val message = try { response.body<Map<String, String>>() ["error"] } catch (_: Exception) { null }
            error(message ?: "Unable to save Church Identity (${response.status.value}).")
        }
        response.body<ChurchIdentity>()
    }
}

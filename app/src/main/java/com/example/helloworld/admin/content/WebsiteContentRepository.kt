package com.example.helloworld.admin.content

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WebsiteContentRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kfcc_admin_session", Context.MODE_PRIVATE)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    }

    companion object { private const val BASE_URL = "https://kingdomfellowshipchristianchurch.onrender.com/" }

    private fun session(): String? = prefs.getString("session_cookie", null)

    suspend fun get(): Result<WebsiteContent> = runCatching {
        val request = client.get(BASE_URL + "api/site/content") {
            session()?.let { cookie("connect.sid", it) }
        }
        if (!request.status.isSuccess()) error("Unable to load website content (${request.status.value}).")
        Json.decodeFromString<WebsiteContent>(request.bodyAsText())
    }

    suspend fun save(content: WebsiteContent): Result<Unit> = runCatching {
        val request = client.put(BASE_URL + "api/site/content") {
            session()?.let { cookie("connect.sid", it) }
            contentType(ContentType.Application.Json)
            setBody(content)
        }
        if (!request.status.isSuccess()) error(request.bodyAsText().ifBlank { "Unable to save website content (${request.status.value})." })
    }
}

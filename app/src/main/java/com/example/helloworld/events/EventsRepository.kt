package com.example.helloworld.events

import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class EventsRepository(private val adminRepository: AdminRepository? = null) {
    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true }) }
    }
    private val baseUrl = "https://kingdomfellowshipchristianchurch.onrender.com"

    suspend fun getPublicEvents(): Result<List<Event>> = runCatching {
        val response = client.get("$baseUrl/api/events") { header(HttpHeaders.Accept, ContentType.Application.Json.toString()) }
        if (response.status != HttpStatusCode.OK) error("Unable to load events (${response.status.value}).")
        response.body()
    }

    suspend fun getAdminEvents(): Result<List<Event>> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedGet("api/events")
        if (response.status != HttpStatusCode.OK) error("Unable to load events (${response.status.value}).")
        response.body()
    }

    suspend fun create(input: EventInput): Result<Event> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedPost("api/admin/events", input)
        if (response.status != HttpStatusCode.Created) error(response.body<String>())
        response.body()
    }

    suspend fun update(id: Long, input: EventInput): Result<Event> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedPut("api/admin/events/$id", input)
        if (response.status != HttpStatusCode.OK) error(response.body<String>())
        response.body()
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedDelete("api/admin/events/$id")
        if (response.status != HttpStatusCode.OK) error("Unable to delete event (${response.status.value}).")
    }
}

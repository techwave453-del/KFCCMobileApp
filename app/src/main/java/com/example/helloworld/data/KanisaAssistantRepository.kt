package com.example.helloworld.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class KanisaAssistantMessage(
    val role: String,
    val content: String
)

@Serializable
data class KanisaAssistantRequest(
    val message: String,
    val provider: String = "cloud",
    val conversation: List<KanisaAssistantMessage> = emptyList()
)

@Serializable
data class KanisaAssistantResponse(
    val assistant_name: String = "Kanisa Assistant",
    val answer: String,
    val bible_references: List<String> = emptyList(),
    val provider: String = "cloud",
    val model: String? = null,
    val error: String? = null
)

class KanisaAssistantRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun ask(
        message: String,
        conversation: List<KanisaAssistantMessage> = emptyList()
    ): Result<KanisaAssistantResponse> = runCatching {
        val token = SupabaseProvider.client.auth.currentAccessTokenOrNull()
            ?: error("Please sign in to use Kanisa Assistant.")

        val response = client.post("$FUNCTIONS_URL/kanisa-assistant") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(KanisaAssistantRequest(message = message, conversation = conversation))
        }

        if (response.status.value !in 200..299) {
            val errorBody = runCatching { response.body<KanisaAssistantResponse>() }.getOrNull()
            error(errorBody?.error ?: "Kanisa Assistant is temporarily unavailable.")
        }

        response.body()
    }

    companion object {
        private const val FUNCTIONS_URL =
            "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}

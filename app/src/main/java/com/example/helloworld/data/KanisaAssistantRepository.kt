package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
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
private data class ChatAssistantJwtPayload(val sub: String)

@Serializable
data class KanisaAssistantMessage(
    val role: String,
    val content: String
)

@Serializable
data class KanisaAssistantRequest(
    val message: String,
    val provider: String = "cloud",
    val conversation: List<KanisaAssistantMessage> = emptyList(),
    val room_id: String? = null,
    val reply_to_message_id: String? = null
)

@Serializable
data class StoredKanisaAssistantMessage(
    val id: Long,
    val user_id: String,
    val role: String,
    val content: String,
    val bible_references: List<String> = emptyList(),
    val bible_quotes: List<KanisaAssistantBibleQuote> = emptyList(),
    val created_at: String
)

@Serializable
private data class SaveKanisaAssistantMessage(
    val user_id: String,
    val role: String,
    val content: String,
    val bible_references: List<String>,
    val bible_quotes: List<KanisaAssistantBibleQuote>
)

@Serializable
data class KanisaAssistantBibleQuote(
    val reference: String,
    val text: String,
    val translation: String = "KJV"
)

@Serializable
data class KanisaAssistantResponse(
    val assistant_name: String = "Kanisa Assistant",
    val answer: String,
    val bible_references: List<String> = emptyList(),
    val bible_quotes: List<KanisaAssistantBibleQuote> = emptyList(),
    val provider: String = "cloud",
    val model: String? = null,
    val room_message_id: String? = null,
    val error: String? = null
)

class KanisaAssistantRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun loadHistory(limit: Int = 80): Result<List<StoredKanisaAssistantMessage>> = runCatching {
        if (!SupabaseProvider.ensureSession()) {
            error("Please sign in to use Kanisa Assistant.")
        }
        val userId = currentUserId() ?: error("Please sign in to use Kanisa Assistant.")
        SupabaseProvider.client.from("kanisa_assistant_messages")
            .select {
                filter { eq("user_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList()
    }

    suspend fun saveMessage(
        role: String,
        content: String,
        bibleReferences: List<String> = emptyList(),
        bibleQuotes: List<KanisaAssistantBibleQuote> = emptyList()
    ): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Please sign in to use Kanisa Assistant.")
        require(role == "user" || role == "assistant") { "Invalid assistant message role." }
        require(content.isNotBlank()) { "Message cannot be empty." }

        SupabaseProvider.client.from("kanisa_assistant_messages").insert(
            SaveKanisaAssistantMessage(
                user_id = userId,
                role = role,
                content = content.trim(),
                bible_references = bibleReferences,
                bible_quotes = bibleQuotes
            )
        )
    }

    suspend fun deleteMessage(id: Long): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Please sign in again.")
        SupabaseProvider.client.from("kanisa_assistant_messages").delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    suspend fun clearHistory(): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Please sign in again.")
        SupabaseProvider.client.from("kanisa_assistant_messages").delete {
            filter { eq("user_id", userId) }
        }
    }

    private fun currentUserId(): String? {
        val auth = SupabaseProvider.client.auth
        val user = auth.currentUserOrNull() ?: auth.currentSessionOrNull()?.user
        if (user != null) return user.id

        val token = auth.currentAccessTokenOrNull() ?: return null
        return runCatching {
            val parts = token.split(".")
            if (parts.size != 3) return@runCatching null
            val payload = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE)
            kotlinx.serialization.json.Json
                .decodeFromString<ChatAssistantJwtPayload>(String(payload))
                .sub
        }.getOrNull()
    }

    suspend fun ask(
        message: String,
        conversation: List<KanisaAssistantMessage> = emptyList(),
        roomId: String? = null,
        replyToMessageId: String? = null
    ): Result<KanisaAssistantResponse> = runCatching {
        if (!SupabaseProvider.ensureSession()) {
            error("Your sign-in session is unavailable. Please sign in again.")
        }
        val token = SupabaseProvider.client.auth.currentAccessTokenOrNull()
            ?: error("Your sign-in session is unavailable. Please sign in again.")

        val response = client.post("$FUNCTIONS_URL/kanisa-assistant") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                KanisaAssistantRequest(
                    message = message,
                    conversation = conversation,
                    room_id = roomId,
                    reply_to_message_id = replyToMessageId
                )
            )
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

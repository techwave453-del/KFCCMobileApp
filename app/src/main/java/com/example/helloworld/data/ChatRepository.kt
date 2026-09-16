package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val message: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class ChatRoom(
    val id: String,
    val type: String,
    val title: String,
)

class ChatRepository {
    private val client get() = SupabaseProvider.client

    suspend fun joinCommunity(): Result<String> = runCatching {
        client.postgrest.rpc("join_kfcc_community").decodeSingle<String>()
    }

    suspend fun getCommunityRoom(): Result<ChatRoom> = runCatching {
        client.from("chat_rooms")
            .select { filter { eq("type", "community") } }
            .decodeList<ChatRoom>()
            .firstOrNull() ?: error("Community chat is not available yet.")
    }

    suspend fun getMessages(roomId: String): Result<List<ChatMessage>> = runCatching {
        client.from("chat_messages")
            .select {
                filter {
                    eq("room_id", roomId)
                }
            }
            .decodeList<ChatMessage>()
            .filter { it.deletedAt == null }
            .sortedBy { it.createdAt }
            .takeLast(100)
    }

    suspend fun sendMessage(roomId: String, message: String): Result<Unit> = runCatching {
        val text = message.trim()
        require(text.isNotEmpty()) { "Write a message first." }
        require(text.length <= 1000) { "Message is too long." }
        val senderId = client.auth.currentUserOrNull()?.id ?: error("Please sign in to chat.")

        client.from("chat_messages").insert(
            mapOf(
                "room_id" to roomId,
                "sender_id" to senderId,
                "message" to text,
            )
        )
    }
}

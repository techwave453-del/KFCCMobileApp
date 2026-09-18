package com.example.helloworld.data

import android.util.Base64
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
private data class ChatRepoJwtPayload(val sub: String)

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val message: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("chat_profiles") val senderProfile: ChatProfile? = null
)

@Serializable
data class ChatRoom(
    val id: String,
    val type: String,
    val title: String,
    @SerialName("join_mode") val joinMode: String = "open",
    @SerialName("created_by") val createdBy: String? = null,
)

data class ChatGroupJoinRequest(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
    @SerialName("requested_at") val requestedAt: String,
)

class ChatRepository {
    private val client get() = SupabaseProvider.client

    suspend fun joinCommunity(): Result<String> = runCatching {
        client.postgrest.rpc("join_kfcc_community").decodeAs<String>()
    }

    suspend fun getCommunityRoom(): Result<ChatRoom> = runCatching {
        client.from("chat_rooms")
            .select { filter { eq("type", "community") } }
            .decodeList<ChatRoom>()
            .firstOrNull() ?: error("Community chat is not available yet.")
    }

    suspend fun getMessages(roomId: String): Result<List<ChatMessage>> = runCatching {
        try {
            client.from("chat_messages")
                .select(columns = Columns.raw("*, chat_profiles!sender_id(*)")) {
                    filter { eq("room_id", roomId) }
                }
                .decodeList<ChatMessage>()
        } catch (_: Exception) {
            client.from("chat_messages")
                .select { filter { eq("room_id", roomId) } }
                .decodeList<ChatMessage>()
        }
    }.map { list ->
        list.filter { it.deletedAt == null }
            .sortedBy { it.createdAt }
            .takeLast(100)
    }

    fun observeMessages(roomId: String): Flow<PostgresAction> {
        val channelId = "chat_${roomId}_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "chat_messages"
            filter = "room_id=eq.$roomId"
        }.onStart {
            channel.subscribe()
        }.onCompletion {
            client.realtime.removeChannel(channel)
        }
    }

    suspend fun sendMessage(roomId: String, message: String, replyToId: String? = null): Result<Unit> = runCatching {
        val text = message.trim()
        require(text.isNotEmpty()) { "Write a message first." }
        require(text.length <= 1000) { "Message is too long." }

        val session = client.auth.currentSessionOrNull()

        // The username/admin login imports a session with user = null.
        // currentUserOrNull() can also briefly contain a stale user from the
        // previous session, so the JWT belonging to the current session is
        // the authoritative identity for a message insert.
        val senderId = session?.accessToken?.let { token ->
            try {
                val parts = token.split(".")
                if (parts.size != 3) null
                else {
                    val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                    Json { ignoreUnknownKeys = true }.decodeFromString<ChatRepoJwtPayload>(payload).sub
                }
            } catch (_: Exception) { null }
        } ?: client.auth.currentUserOrNull()?.id
          ?: session?.user?.id
          ?: error("Chat connection lost. Please sign in again.")

        val data = mutableMapOf(
            "room_id" to roomId,
            "sender_id" to senderId,
            "message" to text,
        )
        if (replyToId != null) data["reply_to_id"] = replyToId

        client.from("chat_messages").insert(data)
    }

    suspend fun editMessage(messageId: String, message: String): Result<Unit> = runCatching {
        val text = message.trim()
        require(text.isNotEmpty()) { "Message cannot be empty." }
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).format(Date())
        client.from("chat_messages").update(
            mapOf("message" to text, "edited_at" to now)
        ) {
            filter { eq("id", messageId) }
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).format(Date())
        client.from("chat_messages").update(mapOf("deleted_at" to now)) {
            filter { eq("id", messageId) }
        }
    }

    suspend fun createGroup(title: String): Result<ChatRoom> = runCatching {
        require(title.trim().isNotEmpty()) { "Group name cannot be empty." }

        val session = client.auth.currentSessionOrNull()
        val userId = client.auth.currentUserOrNull()?.id
            ?: session?.user?.id
            ?: session?.accessToken?.let { token ->
                try {
                    val parts = token.split(".")
                    val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                    Json { ignoreUnknownKeys = true }.decodeFromString<ChatRepoJwtPayload>(payload).sub
                } catch (_: Exception) { null }
            }
            ?: error("Chat connection lost. Please sign in again.")

        // PostgREST INSERT defaults to an empty response. Request the inserted
        // row explicitly before decoding it, otherwise decodeSingle() receives EOF.
        val newRoom = client.from("chat_rooms").insert(
            mapOf(
                "title" to title.trim(),
                "type" to "group",
                "created_by" to userId
            )
        ) {
            select()
        }.decodeSingle<ChatRoom>()

        // The room is intentionally private: the creator must be a member before
        // the normal room/message RLS policies allow access.
        client.from("chat_room_members").insert(
            mapOf(
                "room_id" to newRoom.id,
                "user_id" to userId,
                "role" to "member"
            )
        )

        newRoom
    }

    suspend fun getRooms(): Result<List<ChatRoom>> = runCatching {
        // Only return rooms the current member can actually enter. Group discovery
        // is handled separately by getDiscoverableGroups().
        val memberships = client.from("chat_room_members")
            .select { columns = Columns.raw("room_id") }
            .decodeList<RoomMembership>()
        val ids = memberships.map { it.roomId }
        if (ids.isEmpty()) emptyList()
        else client.from("chat_rooms")
            .select { filter { isIn("id", ids) } }
            .decodeList<ChatRoom>()
            .sortedBy { it.title.lowercase() }
    }

    suspend fun getDiscoverableGroups(): Result<List<ChatRoom>> = runCatching {
        client.from("chat_rooms")
            .select { filter { eq("type", "group") } }
            .decodeList<ChatRoom>()
            .sortedBy { it.title.lowercase() }
    }

    suspend fun joinGroup(roomId: String): Result<String> = runCatching {
        client.postgrest.rpc("join_chat_group", mapOf("p_room_id" to roomId)).decodeAs<String>()
    }

    suspend fun requestGroupJoin(roomId: String): Result<String> = runCatching {
        client.postgrest.rpc("request_chat_group_join", mapOf("p_room_id" to roomId)).decodeAs<String>()
    }

    suspend fun getMyJoinRequest(roomId: String): Result<ChatGroupJoinRequest?> = runCatching {
        client.from("chat_group_join_requests")
            .select()
            .decodeList<ChatGroupJoinRequest>()
            .firstOrNull { it.roomId == roomId }
    }

    @Serializable
    private data class RoomMembership(
        @SerialName("room_id") val roomId: String
    )
}

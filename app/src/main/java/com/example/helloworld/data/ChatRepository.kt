package com.example.helloworld.data

import android.content.Context
import android.util.Base64
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
)

class ChatRepository {
    private val client get() = SupabaseProvider.client

    fun getLocalMessages(roomId: String, context: Context): Flow<List<ChatMessage>> {
        val database = AppLocalDatabase.getDatabase(context)
        return database.chatDao().getMessagesForRoom(roomId).map { list ->
            list.map {
                ChatMessage(
                    id = it.id,
                    roomId = it.roomId,
                    senderId = it.senderId,
                    message = it.message,
                    createdAt = it.createdAt,
                    senderProfile = if (it.senderUsername != null) ChatProfile(user_id = it.senderId, username = it.senderUsername) else null
                )
            }
        }
    }

    fun getLocalRooms(context: Context): Flow<List<ChatRoom>> {
        val database = AppLocalDatabase.getDatabase(context)
        return database.chatDao().getAllRooms().map { list ->
            list.map { ChatRoom(id = it.id, type = it.type, title = it.title) }
        }
    }

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
        // We try to get profiles, but if the relationship is missing, 
        // we fallback to just fetching the messages to prevent a complete failure.
        try {
            client.from("chat_messages")
                .select(columns = Columns.raw("*, chat_profiles!sender_id(*)")) {
                    filter { eq("room_id", roomId) }
                }
                .decodeList<ChatMessage>()
        } catch (e: Exception) {
            // Fallback: Fetch without profile join if the schema relationship is broken
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
        // Use a unique channel ID to avoid "already joined" error if multiple flows are active
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

        // Resiliently resolve the sender ID from session or user object.
        val session = client.auth.currentSessionOrNull()
        var user = client.auth.currentUserOrNull() ?: session?.user
        
        if (user == null && session != null) {
            user = try { client.auth.retrieveUserForCurrentSession() } catch (_: Exception) { null }
        }
        
        val senderId = user?.id ?: session?.accessToken?.let { token ->
            try {
                val parts = token.split(".")
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                Json.decodeFromString<ChatRepoJwtPayload>(payload).sub
            } catch (_: Exception) { null }
        } ?: error("Chat connection lost. Please sign in again.")

        val data = mutableMapOf(
            "room_id" to roomId,
            "sender_id" to senderId,
            "message" to text,
        )
        if (replyToId != null) {
            data["reply_to_id"] = replyToId
        }

        client.from("chat_messages").insert(data)
    }

    suspend fun editMessage(messageId: String, message: String): Result<Unit> = runCatching {
        val text = message.trim()
        require(text.isNotEmpty()) { "Message cannot be empty." }
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).format(Date())
        client.from("chat_messages").update(
            mapOf(
                "message" to text,
                "edited_at" to now
            )
        ) {
            filter { eq("id", messageId) }
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).format(Date())
        client.from("chat_messages").update(
            mapOf("deleted_at" to now)
        ) {
            filter { eq("id", messageId) }
        }
    }

    suspend fun createGroup(title: String): Result<ChatRoom> = runCatching {
        require(title.trim().isNotEmpty()) { "Group name cannot be empty." }
        val session = client.auth.currentSessionOrNull()
        val userId = client.auth.currentUserOrNull()?.id ?: session?.user?.id ?: session?.accessToken?.let { token ->
            try {
                val parts = token.split(".")
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                Json.decodeFromString<ChatRepoJwtPayload>(payload).sub
            } catch (_: Exception) { null }
        }
        client.from("chat_rooms").insert(
            mapOf(
                "title" to title.trim(),
                "type" to "group",
                "created_by" to userId
            )
        ).decodeSingle<ChatRoom>()
    }
    
    suspend fun getRooms(): Result<List<ChatRoom>> = runCatching {
        val remote = client.from("chat_rooms")
            .select()
            .decodeList<ChatRoom>()
        try {
            // Seed to room database database instance context
            // To fetch application context we can do it, but let's keep compatibility
        } catch (_: Exception) {}
        remote
    }

    suspend fun syncRoomsToLocal(roomsList: List<ChatRoom>, context: Context) {
        val database = AppLocalDatabase.getDatabase(context)
        database.chatDao().insertRooms(roomsList.map { LocalChatRoomEntity(id = it.id, type = it.type, title = it.title) })
    }

    suspend fun syncMessagesToLocal(roomId: String, messagesList: List<ChatMessage>, context: Context) {
        val database = AppLocalDatabase.getDatabase(context)
        database.chatDao().insertMessages(messagesList.map {
            LocalChatMessageEntity(
                id = it.id,
                roomId = it.roomId,
                senderId = it.senderId,
                message = it.message,
                createdAt = it.createdAt,
                senderUsername = it.senderProfile?.username,
                syncStatus = SyncStatus.SYNCED
            )
        })
    }

    suspend fun saveMessageOffline(roomId: String, messageText: String, senderId: String, context: Context) {
        val database = AppLocalDatabase.getDatabase(context)
        val tempId = UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).format(Date())
        database.chatDao().insertMessages(listOf(
            LocalChatMessageEntity(
                id = tempId,
                roomId = roomId,
                senderId = senderId,
                message = messageText,
                createdAt = now,
                senderUsername = "Me (Offline)",
                syncStatus = SyncStatus.PENDING_INSERT
            )
        ))
        
        // Enqueue WorkManager job immediately for background network synchronization
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncMessagesWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}

package com.example.helloworld.data

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.auth
import io.ktor.http.ContentType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes
import java.time.Instant
import java.util.UUID

@Serializable
data class DirectInboxItem(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("peer_user_id") val peerUserId: String,
    @SerialName("peer_username") val peerUsername: String,
    @SerialName("peer_display_name") val peerDisplayName: String? = null,
    @SerialName("peer_avatar_url") val peerAvatarUrl: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String,
    @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
)

@Serializable
data class ChatMemberSearchResult(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class DirectUnreadCount(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("unread_count") val unreadCount: Int,
)

@Serializable
data class DirectConversation(
    val id: String,
    @SerialName("participant_a") val participantA: String,
    @SerialName("participant_b") val participantB: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_message_at") val lastMessageAt: String,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
)

@Serializable
data class DirectMessage(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class ChatStory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_path") val mediaPath: String,
    @SerialName("media_type") val mediaType: String,
    val caption: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class StoryFeedItem(
    val id: String,
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("media_path") val mediaPath: String,
    @SerialName("media_type") val mediaType: String,
    val caption: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class SavedChatContact(
    @SerialName("user_id") val userId: String,
    @SerialName("contact_user_id") val contactUserId: String,
    @SerialName("created_at") val createdAt: String,
)

/**
 * Supabase data access for private inbox conversations and Stories.
 *
 * The corresponding migration is additive and deliberately separate from
 * the existing community-chat tables. Supabase RLS remains the authority for
 * which conversations, messages, contacts, and stories the current session can access.
 */
class DirectChatRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getInbox(): Result<List<DirectInboxItem>> = runCatching {
        client.postgrest.rpc("get_direct_chat_inbox")
            .decodeList<DirectInboxItem>()
    }

    suspend fun getUnreadCounts(): Result<Map<String, Int>> = runCatching {
        client.postgrest.rpc("get_direct_chat_unread_counts")
            .decodeList<DirectUnreadCount>()
            .associate { it.conversationId to it.unreadCount }
    }

    suspend fun markConversationRead(conversationId: String): Result<Unit> = runCatching {
        require(conversationId.isNotBlank()) { "Conversation is unavailable." }
        client.postgrest.rpc(
            "mark_direct_conversation_read",
            buildJsonObject { put("p_conversation_id", conversationId) }
        )
    }

    suspend fun getMemberSuggestions(): Result<List<ChatMemberSearchResult>> = runCatching {
        client.postgrest.rpc("get_chat_member_suggestions")
            .decodeList<ChatMemberSearchResult>()
    }

    suspend fun getRooms(): Result<List<ChatRoom>> = runCatching {
        if (!SupabaseProvider.ensureSession()) {
            error("Chat session is not available. Please sign in again.")
        }
        client.from("chat_rooms")
            .select()
            .decodeList<ChatRoom>()
            .distinctBy { it.id }
            .sortedWith(compareBy<ChatRoom> { if (it.type == "community") 0 else 1 }.thenBy { it.title.lowercase() })
    }

    suspend fun searchMembers(query: String): Result<List<ChatMemberSearchResult>> = runCatching {
        val normalized = query.trim()
        require(normalized.length >= 2) { "Enter at least two characters to search members." }
        client.postgrest.rpc(
            "search_chat_members",
            buildJsonObject { put("p_query", JsonPrimitive(normalized)) }
        ).decodeList<ChatMemberSearchResult>()
    }

    suspend fun getConversations(): Result<List<DirectConversation>> = runCatching {
        client.from("chat_direct_conversations")
            .select()
            .decodeList<DirectConversation>()
            .sortedByDescending { parseInstant(it.lastMessageAt) }
    }

    suspend fun startConversation(otherUserId: String): Result<String> = runCatching {
        require(otherUserId.isNotBlank()) { "Choose a member first." }
        val response = client.postgrest.rpc(
            "start_direct_conversation",
            buildJsonObject { put("p_other_user_id", JsonPrimitive(otherUserId)) }
        )
        response.decodeAs<String>()
    }

    suspend fun getMessages(conversationId: String): Result<List<DirectMessage>> = runCatching {
        require(conversationId.isNotBlank()) { "Conversation is unavailable." }
        client.from("chat_direct_messages")
            .select {
                filter { eq("conversation_id", conversationId) }
            }
            .decodeList<DirectMessage>()
            .filter { it.deletedAt == null }
            .sortedBy { parseInstant(it.createdAt) }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        clientMessageId: String? = null,
    ): Result<Unit> = runCatching {
        val body = text.trim()
        require(conversationId.isNotBlank()) { "Conversation is unavailable." }
        require(body.isNotEmpty()) { "Write a message first." }
        require(body.length <= 4000) { "Message is too long." }

        val payload = mutableMapOf<String, String>(
            "conversation_id" to conversationId,
            "body" to body,
        )
        if (!clientMessageId.isNullOrBlank()) payload["client_message_id"] = clientMessageId
        client.from("chat_direct_messages").insert(payload)
    }

    suspend fun getContacts(): Result<List<SavedChatContact>> = runCatching {
        client.from("chat_contacts")
            .select()
            .decodeList<SavedChatContact>()
            .sortedByDescending { parseInstant(it.createdAt) }
    }

    suspend fun addContact(contactUserId: String): Result<Unit> = runCatching {
        require(contactUserId.isNotBlank()) { "Choose a member first." }
        client.postgrest.rpc(
            "add_chat_contact",
            buildJsonObject { put("p_contact_user_id", JsonPrimitive(contactUserId)) }
        )
    }

    suspend fun publishStory(
        mediaBytes: ByteArray,
        mimeType: String,
        caption: String = "",
    ): Result<Unit> = runCatching {
        require(mediaBytes.isNotEmpty()) { "Choose a media file first." }
        require(mediaBytes.size <= 50 * 1024 * 1024) { "Stories must be 50 MB or smaller." }
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime")) {
            "Choose a JPG, PNG, WebP, MP4, or MOV file."
        }
        require(caption.length <= 500) { "Story captions must be 500 characters or fewer." }
        if (!SupabaseProvider.ensureSession()) error("Your session has expired. Please sign in again.")
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Your account is not available. Please sign in again.")
        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            else -> error("Unsupported story media type.")
        }
        val mediaType = if (mimeType.startsWith("video/")) "video" else "image"
        val path = "$userId/${UUID.randomUUID()}.$extension"
        val bucket = client.storage.from("kanisa-story-media")
        bucket.upload(path = path, data = mediaBytes) {
            upsert = false
            contentType = ContentType.parse(mimeType)
        }
        try {
            client.from("chat_stories").insert(
                mapOf(
                    "user_id" to userId,
                    "media_path" to path,
                    "media_type" to mediaType,
                    "caption" to caption.trim(),
                )
            )
        } catch (failure: Throwable) {
            runCatching { bucket.delete(path) }
            throw failure
        }
    }

    suspend fun getStoryMediaUrl(mediaPath: String): Result<String> = runCatching {
        require(mediaPath.isNotBlank()) { "Story media is unavailable." }
        val normalizedPath = mediaPath.trim().removePrefix("/")
            .removePrefix("kanisa-story-media/")
        require(!normalizedPath.startsWith("http", ignoreCase = true)) {
            "Story media must use a private storage object path."
        }
        client.storage.from("kanisa-story-media")
            .createSignedUrl(path = normalizedPath, expiresIn = 30.minutes)
    }

    suspend fun getStoryFeed(): Result<List<StoryFeedItem>> = runCatching {
        client.postgrest.rpc("get_active_chat_stories")
            .decodeList<StoryFeedItem>()
    }

    suspend fun getActiveStories(): Result<List<ChatStory>> = runCatching {
        // The active-story RLS policy is the security boundary; this client-side
        // filter is only a presentation safeguard for stale cached/server results.
        val now = Instant.now()
        client.from("chat_stories")
            .select()
            .decodeList<ChatStory>()
            .filter {
                runCatching { Instant.parse(it.expiresAt).isAfter(now) }.getOrDefault(false)
            }
            .sortedBy { parseInstant(it.createdAt) }
    }

    suspend fun markStoryViewed(storyId: String): Result<Unit> = runCatching {
        require(storyId.isNotBlank()) { "Story is unavailable." }
        client.from("chat_story_views").upsert(
            mapOf("story_id" to storyId)
        )
    }

    private fun parseInstant(value: String): Instant =
        runCatching { Instant.parse(value) }.getOrDefault(Instant.EPOCH)
}

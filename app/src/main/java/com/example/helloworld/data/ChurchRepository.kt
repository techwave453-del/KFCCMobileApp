package com.example.helloworld.data

import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase

/** Public church data repository. Offline data is served first, then Supabase fills the cache. */
class ChurchRepository {
    private val offline by lazy {
        KfccContentRepository(KfccDatabase.getInstance(KfccDataContext.appContext))
    }

    suspend fun getSiteContent(): ChurchInfo = offline.getChurchInfo()

    suspend fun getMedia(): List<MediaItem> = offline.getMedia()

    suspend fun getEvents(): List<EventItem> = offline.getEvents()

    suspend fun getNotifications(userId: String? = null): List<AppNotification> =
        offline.getNotifications(userId)

    suspend fun login(username: String, password: String): LoginResponse =
        LoginResponse(ok = false, error = "Use the administrator authentication flow.")

    suspend fun logout() = Unit

    suspend fun updateSiteContent(content: ChurchInfo): Boolean = false

    suspend fun uploadMedia(
        title: String,
        description: String,
        category: String,
        type: String,
        fileBytes: ByteArray,
        fileName: String
    ): Boolean = false
}

@kotlinx.serialization.Serializable
data class LoginRequest(val username: String, val password: String)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val user: UserInfo? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class UserInfo(val id: Long, val username: String, val role: String)

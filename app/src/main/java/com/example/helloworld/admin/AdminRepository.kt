package com.example.helloworld.admin

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.KfccOutboxRepository
import com.example.helloworld.data.offline.NotificationEntity
import com.example.helloworld.data.offline.SiteContentEntity
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class NotificationSyncPayload(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("show_on_install") val showOnInstall: Boolean = false,
    @SerialName("show_on_sign_in") val showOnSignIn: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
private data class NotificationManagementUpdate(
    val title: String? = null,
    val message: String? = null,
    val type: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean? = null,
    @SerialName("show_on_install") val showOnInstall: Boolean? = null,
    @SerialName("show_on_sign_in") val showOnSignIn: Boolean? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class AdminSessionResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: AdminSessionUser
)

@Serializable
private data class AdminSessionUser(
    val id: String,
    val username: String,
    val email: String? = null,
    val role: String,
    val is_active: Boolean,
    val permissions: List<String> = emptyList()
)

class AdminRepository(context: Context) {
    private val appContext = context.applicationContext
    private val offlineDb = KfccDatabase.getInstance(appContext)
    private val outbox = KfccOutboxRepository(appContext, offlineDb)
    private val client = SupabaseProvider.client

    // The admin-login edge function returns the authoritative administrator
    // record separately from the Supabase Auth user. The imported session may
    // legitimately contain user = null, so keep the authoritative admin record
    // here until restoreSession() can reconstruct it from the authenticated app.
    @Volatile
    private var authenticatedAdmin: AdminUser? = null

    // Keep the last administrator session in process memory as a recovery source.
    // Android can temporarily lose the Auth plugin's in-memory session during
    // lifecycle/storage transitions even though the administrator is still signed in.
    @Volatile
    private var lastImportedSession: UserSession? = null

    private val adminLoginClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        val normalized = username.trim()
        if (normalized.isBlank() || password.isBlank()) {
            return AdminLoginResponse(
                ok = false,
                error = "Enter your administrator username and password."
            )
        }

        return try {
            val response = adminLoginClient.post("$SUPABASE_FUNCTIONS_URL/admin-login") {
                header("Content-Type", ContentType.Application.Json.toString())
                setBody(AdminLoginRequest(normalized, password))
            }

            if (response.status.value !in 200..299) {
                val error = runCatching {
                    response.body<AdminErrorResponse>().error
                }.getOrNull()

                AdminLoginResponse(
                    ok = false,
                    error = error ?: "Invalid administrator username or password."
                )
            } else {
                val session = response.body<AdminSessionResponse>()
                val adminUser = AdminUser(
                    id = session.user.id,
                    username = session.user.username,
                    email = session.user.email,
                    role = session.user.role,
                    is_active = session.user.is_active,
                    permissions = session.user.permissions
                )

                // Auth storage initialization can otherwise finish after this
                // external login and overwrite the newly imported session with the
                // empty stored state. Wait until the shared Auth plugin has completed
                // initialization before installing the administrator session.
                client.auth.awaitInitialization()

                val importedSession = UserSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    expiresIn = session.expiresIn.toLong(),
                    tokenType = session.tokenType,
                    user = null
                )
                client.auth.importSession(importedSession)
                lastImportedSession = importedSession
                SupabaseProvider.rememberImportedSession(importedSession)

                // importSession() is the supported way to install a session
                // returned by an external login flow. Verify that the shared
                // Supabase client actually retained it before reporting login
                // success. If the imported session was not retained, force one
                // refresh using the returned refresh token; this also ensures
                // the Auth plugin persists a usable session for other modules.
                if (client.auth.currentSessionOrNull() == null) {
                    client.auth.refreshSession(refreshToken = session.refreshToken)
                }

                if (client.auth.currentSessionOrNull() == null) {
                    error("Administrator authentication succeeded, but the Supabase session could not be installed.")
                }

                // Do not retrieve/update the Auth user here. The Auth session has
                // just been imported and other ViewModels may react to the
                // Authenticated event at the same time. A user retrieval here would
                // introduce a second session operation and can race refresh-token
                // rotation. restoreSession() remains the single recovery path that
                // needs the Auth user object.
                authenticatedAdmin = adminUser

                AdminLoginResponse(ok = true, user = adminUser)
            }
        } catch (e: Exception) {
            AdminLoginResponse(
                ok = false,
                error = e.message ?: "Unable to sign in as administrator."
            )
        }
    }

    suspend fun ensureAdminSession(): Boolean {
        client.auth.awaitInitialization()

        if (client.auth.currentSessionOrNull() != null) {
            return true
        }

        // First try the SDK's persisted session. This is the normal recovery path.
        runCatching {
            client.auth.loadFromStorage(autoRefresh = true)
        }

        if (client.auth.currentSessionOrNull() != null) {
            return true
        }

        // If Android cleared the in-memory Auth state while this process is still
        // alive, reinstall the session returned by the last successful admin login.
        val cached = lastImportedSession ?: return false
        return runCatching {
            client.auth.importSession(cached)
            client.auth.currentSessionOrNull() != null
        }.getOrDefault(false)
    }

    suspend fun restoreSession(): AdminUser? {
        // 1. Check if we have an authoritative admin from the current session's memory.
        authenticatedAdmin?.let { cached ->
            if (client.auth.currentAccessTokenOrNull() != null && cached.is_active) {
                return cached
            }
        }

        // 2. Check if there is an active session in the Supabase SDK.
        val session = client.auth.currentSessionOrNull() ?: return null

        // Sessions imported by admin-login intentionally use user = null.
        // After an app restart the persisted session can still be in that form,
        // so retrieve the current Auth user before reading administrator metadata.
        val user = session.user ?: runCatching {
            client.auth.retrieveUserForCurrentSession()
        }.getOrNull() ?: return null

        // 3. Extract identity from metadata. Administrators have specific app_metadata.
        val metadata = user.appMetadata ?: return null
        if (metadata["kfcc_admin"]?.toString()?.trim('"') != "true") return null

        val username = metadata["admin_username"]?.toString()?.trim('"').orEmpty()
        val role = metadata["admin_role"]?.toString()?.trim('"').orEmpty()
        val permissions = metadata["admin_permissions"]?.toString()
            ?.let { raw -> runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull() }
            .orEmpty()

        if (username.isBlank() || role.isBlank()) return null

        return AdminUser(
            id = metadata["admin_user_id"]?.toString()?.trim('"').orEmpty(),
            username = username,
            email = user.email,
            role = role,
            is_active = true,
            permissions = permissions
        ).also { authenticatedAdmin = it }
    }

    suspend fun logout() {
        authenticatedAdmin = null
        lastImportedSession = null
        SupabaseProvider.clearRememberedSession()
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Signing out locally is best-effort.
        }
    }

    suspend fun loadSiteContent(): Result<ChurchInfo> = runCatching {
        val rows = client.from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()

        decodeChurchInfo(rows) ?: ChurchContent.default
    }

    suspend fun saveSiteContent(content: ChurchInfo): Result<ChurchInfo> = runCatching {
        // Church identity is managed exclusively by the Super Admin through
        // IdentityRepository. Do not enqueue protected identity keys from
        // generic Website Content/Services saves.
        val rows = listOf(
            SiteContentEntity("tagline", content.tagline),
            SiteContentEntity("title", content.title),
            SiteContentEntity("subtitle", content.subtitle),
            SiteContentEntity("aboutTitle", content.aboutTitle),
            SiteContentEntity("aboutText", content.aboutText),
            SiteContentEntity("givingUrl", content.givingUrl),
            SiteContentEntity("services", Json.encodeToString(content.services)),
            SiteContentEntity("links", Json.encodeToString(content.links)),
            SiteContentEntity("membershipClasses", Json.encodeToString(content.membershipClasses))
        )
        offlineDb.siteContentDao().upsertAll(rows)
        rows.forEach { row ->
            outbox.enqueue(
                entityType = "site_content",
                operationType = "UPSERT",
                entityId = row.key,
                payload = Json.encodeToString(mapOf("key" to row.key, "value" to row.value))
            )
        }
        content
    }

    suspend fun saveServices(services: List<com.example.helloworld.data.ChurchService>): Result<Unit> = runCatching {
        val value = Json.encodeToString(services)
        offlineDb.siteContentDao().upsertAll(listOf(SiteContentEntity("services", value)))
        outbox.enqueue(
            entityType = "site_content",
            operationType = "UPSERT",
            entityId = "services",
            payload = Json.encodeToString(mapOf("key" to "services", "value" to value))
        )
    }

    suspend fun saveGivingUrl(givingUrl: String): Result<Unit> = runCatching {
        val value = givingUrl.trim()
        offlineDb.siteContentDao().upsertAll(listOf(SiteContentEntity("givingUrl", value)))
        outbox.enqueue(
            entityType = "site_content",
            operationType = "UPSERT",
            entityId = "givingUrl",
            payload = Json.encodeToString(mapOf("key" to "givingUrl", "value" to value))
        )
    }

    private fun decodeChurchInfo(rows: List<SiteContentRow>): ChurchInfo? {
        val values = rows.associate { it.key to it.value }
        if (values.isEmpty()) return null

        return ChurchInfo(
            churchName = values["churchName"].orEmpty(),
            tagline = values["tagline"].orEmpty(),
            title = values["title"].orEmpty(),
            subtitle = values["subtitle"].orEmpty(),
            aboutTitle = values["aboutTitle"].orEmpty(),
            aboutText = values["aboutText"].orEmpty(),
            phone = values["phone"].orEmpty(),
            email = values["email"].orEmpty(),
            givingUrl = values["givingUrl"].orEmpty(),
            services = decode(values["services"], emptyList()),
            links = decode(values["links"], emptyList()),
            membershipClasses = decode(values["membershipClasses"], emptyList()),
            liveStream = decode(values["liveStream"], LiveStream())
        )
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T =
        try {
            if (raw.isNullOrBlank()) fallback else Json.decodeFromString(raw)
        } catch (_: Exception) {
            fallback
        }

    suspend fun updateSiteContent(key: String, value: String): Result<Unit> = runCatching {
        offlineDb.siteContentDao().upsertAll(listOf(SiteContentEntity(key, value)))
        outbox.enqueue(
            entityType = "site_content",
            operationType = "UPSERT",
            entityId = key,
            payload = Json.encodeToString(mapOf("key" to key, "value" to value))
        )
    }

    suspend fun getAppUpdateConfig(): Result<AppUpdateConfig> = runCatching {
        client.from("app_update_config")
            .select()
            .decodeSingle<AppUpdateConfig>()
    }

    suspend fun saveAppUpdateConfig(config: AppUpdateConfig): Result<Unit> = runCatching {
        require(config.versionCode > 0) { "Version code must be greater than zero." }
        require(config.versionName.isNotBlank()) { "Version name is required." }
        require(config.downloadUrl.isNotBlank()) { "APK download URL is required." }
        client.from("app_update_config").update(
            mapOf(
                "version_code" to config.versionCode,
                "version_name" to config.versionName.trim(),
                "download_url" to config.downloadUrl.trim(),
                "release_notes" to config.releaseNotes.trim(),
                "is_enabled" to config.isEnabled
            )
        ) {
            filter { eq("id", AppUpdateConfig.SINGLETON_ID) }
        }
    }

    suspend fun publishLatestSuccessfulBuild(
        downloadUrl: String,
        releaseNotes: String,
        isEnabled: Boolean
    ): Result<Unit> = runCatching {
        val latest = getAppUpdateConfig().getOrThrow()
        require(latest.latestBuildVersionCode != null) { "No successful release build has been recorded yet." }
        require(latest.latestBuildVersionName?.isNotBlank() == true) { "The latest successful build has no version name." }
        require(downloadUrl.isNotBlank()) { "APK download URL is required." }
        require(latest.latestBuildVersionCode > latest.versionCode) {
            "The latest successful build must have a higher version code than the currently published version."
        }

        client.from("app_update_config").update(
            mapOf(
                "version_code" to latest.latestBuildVersionCode,
                "version_name" to latest.latestBuildVersionName,
                "download_url" to downloadUrl.trim(),
                "release_notes" to releaseNotes.trim(),
                "is_enabled" to isEnabled
            )
        ) {
            filter { eq("id", AppUpdateConfig.SINGLETON_ID) }
        }
    }

    suspend fun getBibleGameQuestions(): Result<List<BibleGameQuestionAdmin>> = runCatching {
        client.from("bible_game_questions")
            .select()
            .decodeList<BibleGameQuestionAdmin>()
            .sortedWith(compareBy<BibleGameQuestionAdmin> { !it.isPublished }.thenBy { it.category }.thenBy { it.sortOrder })
    }

    suspend fun createBibleGameQuestion(question: BibleGameQuestionAdmin): Result<Unit> = runCatching {
        client.from("bible_game_questions").insert(
            mapOf(
                "category" to question.category,
                "question" to question.question.trim(),
                "options" to question.options,
                "correct_answer_index" to question.correctAnswerIndex,
                "explanation" to question.explanation.trim(),
                "reference" to question.reference.trim(),
                "is_published" to question.isPublished,
                "sort_order" to question.sortOrder
            )
        )
    }

    suspend fun updateBibleGameQuestion(question: BibleGameQuestionAdmin): Result<Unit> = runCatching {
        require(question.id.isNotBlank()) { "Question ID is required." }
        client.from("bible_game_questions").update(
            mapOf(
                "category" to question.category,
                "question" to question.question.trim(),
                "options" to question.options,
                "correct_answer_index" to question.correctAnswerIndex,
                "explanation" to question.explanation.trim(),
                "reference" to question.reference.trim(),
                "is_published" to question.isPublished,
                "sort_order" to question.sortOrder
            )
        ) {
            filter { eq("id", question.id) }
        }
    }

    suspend fun deleteBibleGameQuestion(id: String): Result<Unit> = runCatching {
        require(id.isNotBlank()) { "Question ID is required." }
        client.from("bible_game_questions").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun setBibleGameQuestionPublished(id: String, published: Boolean): Result<Unit> = runCatching {
        client.from("bible_game_questions").update(
            mapOf("is_published" to published)
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.userId == null }
            .sortedByDescending { it.createdAt }
    }

    suspend fun updateNotification(
        id: String,
        title: String,
        message: String,
        type: String,
        isEnabled: Boolean,
        showOnInstall: Boolean,
        showOnSignIn: Boolean,
        imageUrl: String? = null
    ): Result<Unit> = runCatching {
        client.from("app_notifications").update(
            NotificationManagementUpdate(
                title = title.trim(),
                message = message.trim(),
                type = type.trim().ifBlank { "general" },
                isEnabled = isEnabled,
                showOnInstall = showOnInstall,
                showOnSignIn = showOnSignIn,
                imageUrl = imageUrl,
                updatedAt = java.time.Instant.now().toString()
            )
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteNotification(id: String): Result<Unit> = runCatching {
        client.from("app_notifications").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun postAnnouncement(
        title: String,
        message: String,
        type: String,
        showOnInstall: Boolean = false,
        showOnSignIn: Boolean = false,
        imageUrl: String? = null
    ): Result<Boolean> = runCatching {
        val id = UUID.randomUUID().toString()
        val createdAt = java.time.Instant.now().toString()
        val payload = NotificationSyncPayload(
            id = id,
            title = title,
            message = message,
            type = type,
            createdAt = createdAt,
            showOnInstall = showOnInstall,
            showOnSignIn = showOnSignIn,
            imageUrl = imageUrl,
            senderId = client.auth.currentUserOrNull()?.id
        )

        if (isNetworkAvailable()) {
            client.from("app_notifications").insert(payload)
            offlineDb.notificationDao().upsertAll(
                listOf(NotificationEntity(id, null, title, message, type, createdAt, true, imageUrl))
            )
            true
        } else {
            offlineDb.notificationDao().upsertAll(
                listOf(NotificationEntity(id, null, title, message, type, createdAt))
            )
            outbox.enqueue(
                entityType = "app_notifications",
                operationType = "INSERT",
                entityId = id,
                payload = Json.encodeToString(payload)
            )
            false
        }
    }

    suspend fun uploadNotificationImage(bytes: ByteArray, contentType: String): Result<String> = runCatching {
        require(bytes.isNotEmpty()) { "The selected image is empty." }
        require(bytes.size <= 1024 * 1024) { "Notification images must be 1 MB or smaller so they can be delivered reliably by FCM." }
        require(contentType.lowercase().startsWith("image/")) { "Please select an image file." }

        val extension = when (contentType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val path = "notifications/" + UUID.randomUUID().toString() + "." + extension
        val bucket = client.storage.from("notification-images")
        bucket.upload(path, bytes) {
            upsert = false
            this.contentType = ContentType.parse(contentType)
        }
        bucket.publicUrl(path)
    }

    suspend fun setInstallDefault(id: String, enabled: Boolean): Result<Unit> = runCatching {
        if (enabled) {
            client.from("app_notifications").update(NotificationManagementUpdate(showOnInstall = false)) {
                filter { eq("show_on_install", true) }
            }
        }
        client.from("app_notifications").update(
            NotificationManagementUpdate(
                showOnInstall = enabled,
                updatedAt = java.time.Instant.now().toString()
            )
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun setSignInDefault(id: String, enabled: Boolean): Result<Unit> = runCatching {
        if (enabled) {
            client.from("app_notifications").update(NotificationManagementUpdate(showOnSignIn = false)) {
                filter { eq("show_on_sign_in", true) }
            }
        }
        client.from("app_notifications").update(
            NotificationManagementUpdate(
                showOnSignIn = enabled,
                updatedAt = java.time.Instant.now().toString()
            )
        ) {
            filter { eq("id", id) }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        private const val SUPABASE_FUNCTIONS_URL =
            "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"

    }
}


@kotlinx.serialization.Serializable
data class BibleGameQuestionAdmin(
    val id: String = "",
    val category: String = "FAITH_AND_LIFE",
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    @kotlinx.serialization.SerialName("correct_answer_index")
    val correctAnswerIndex: Int = 0,
    val explanation: String = "",
    val reference: String = "",
    @kotlinx.serialization.SerialName("is_published")
    val isPublished: Boolean = false,
    @kotlinx.serialization.SerialName("sort_order")
    val sortOrder: Int = 0
)

@Serializable
data class AppUpdateConfig(
    @SerialName("id") val id: String = "android",
    @SerialName("version_code") val versionCode: Int = 1,
    @SerialName("version_name") val versionName: String = "1.0.0",
    @SerialName("download_url") val downloadUrl: String = "",
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("latest_build_version_code") val latestBuildVersionCode: Int? = null,
    @SerialName("latest_build_version_name") val latestBuildVersionName: String? = null,
    @SerialName("latest_build_at") val latestBuildAt: String? = null,
    @SerialName("latest_build_commit") val latestBuildCommit: String? = null
) {
    companion object {
        const val SINGLETON_ID = "android"
    }
}

@Serializable
private data class AdminErrorResponse(
    val error: String? = null
)

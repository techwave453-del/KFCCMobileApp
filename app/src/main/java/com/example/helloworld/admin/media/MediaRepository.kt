package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class FeaturedMediaRequest(val featured: Boolean)

@Serializable
private data class MediaUpdateRequest(
    val title: String,
    val description: String,
    val category: String,
    val published: Boolean? = null,
    val featured: Boolean? = null
)

@Serializable
private data class MediaUrlRequest(
    val title: String,
    val type: String,
    val category: String,
    val url: String,
    val description: String
)

@Serializable
private data class ApiError(val error: String? = null)

/** Server-authoritative Media Center API client. */
class MediaRepository(context: Context) {
    private val adminRepository = AdminRepository(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun serverError(response: io.ktor.client.statement.HttpResponse, fallback: String): String {
        return try {
            val payload = json.decodeFromString<ApiError>(response.bodyAsText())
            payload.error?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) { fallback }
    }

    suspend fun load(): Result<List<AdminMediaItem>> = try {
        val response = adminRepository.authenticatedGet("api/admin/media")
        when (response.status) {
            HttpStatusCode.OK -> Result.success(response.body())
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You do not have permission to view Media Center.")
            else -> error(serverError(response, "Media service returned ${response.status.value}."))
        }
    } catch (error: Exception) { Result.failure(error) }

    suspend fun addUrl(title: String, type: String, category: String, url: String, description: String): Result<AdminMediaItem> = try {
        val response = adminRepository.authenticatedPost(
            "api/media/url",
            MediaUrlRequest(title.trim(), type, category, url.trim(), description.trim())
        )
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> Result.success(response.body())
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You do not have permission to add media.")
            else -> error(serverError(response, "Unable to save the media URL (${response.status.value})."))
        }
    } catch (error: Exception) { Result.failure(error) }

    suspend fun update(id: Long, title: String, description: String, category: String, published: Boolean? = null, featured: Boolean? = null): Result<AdminMediaItem> = try {
        val response = adminRepository.authenticatedPatch("api/media/$id", MediaUpdateRequest(title, description, category, published, featured))
        when (response.status) {
            HttpStatusCode.OK -> Result.success(response.body())
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You do not have permission to edit media.")
            HttpStatusCode.NotFound -> error("Media item not found.")
            else -> error(serverError(response, "Unable to update media (${response.status.value})."))
        }
    } catch (error: Exception) { Result.failure(error) }

    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> = try {
        val response = adminRepository.authenticatedPatch("api/media/$id/featured", FeaturedMediaRequest(featured))
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.NoContent -> Result.success(Unit)
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You do not have permission to feature media.")
            else -> error(serverError(response, "Unable to update the Featured Video (${response.status.value})."))
        }
    } catch (error: Exception) { Result.failure(error) }

    suspend fun delete(id: Long): Result<Unit> = try {
        val response = adminRepository.authenticatedDelete("api/media/$id")
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.NoContent -> Result.success(Unit)
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You do not have permission to delete media.")
            HttpStatusCode.NotFound -> error("Media item not found.")
            else -> error(serverError(response, "Unable to delete media (${response.status.value})."))
        }
    } catch (error: Exception) { Result.failure(error) }

    suspend fun upload(bytes: ByteArray, fileName: String, mimeType: String, title: String, description: String, category: String, type: String): Result<AdminMediaItem> = try {
        val response = adminRepository.authenticatedMultipartUpload(
            path = "api/media", bytes = bytes, fileName = fileName, mimeType = mimeType,
            fields = mapOf("title" to title, "description" to description, "category" to category, "type" to type)
        )
        when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> Result.success(response.body())
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You do not have permission to upload media.")
            else -> error(serverError(response, "Media upload failed (${response.status.value})."))
        }
    } catch (error: Exception) { Result.failure(error) }
}

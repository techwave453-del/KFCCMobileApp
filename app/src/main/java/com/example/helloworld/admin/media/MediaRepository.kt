package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
private data class FeaturedMediaRequest(val featured: Boolean)

/** Server-authoritative Media Center API client. */
class MediaRepository(context: Context) {
    private val adminRepository = AdminRepository(context.applicationContext)

    suspend fun load(): Result<List<AdminMediaItem>> {
        return try {
            val response = adminRepository.authenticatedGet("api/media")
            if (response.status != HttpStatusCode.OK) {
                error("Media service returned ${response.status.value}.")
            }
            val media: List<AdminMediaItem> = response.body()
            Result.success(media)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedPatch(
                "api/media/$id/featured",
                FeaturedMediaRequest(featured)
            )
            if (response.status.value !in 200..299) {
                error("Unable to update featured media (${response.status.value}).")
            }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        description: String,
        category: String,
        type: String
    ): Result<AdminMediaItem> {
        return try {
            val response = adminRepository.authenticatedMultipartUpload(
                path = "api/media",
                bytes = bytes,
                fileName = fileName,
                mimeType = mimeType,
                fields = mapOf(
                    "title" to title,
                    "description" to description,
                    "category" to category,
                    "type" to type
                )
            )
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
                HttpStatusCode.Forbidden -> error("You do not have permission to upload media.")
                else -> error("Media upload failed (${response.status.value}).")
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

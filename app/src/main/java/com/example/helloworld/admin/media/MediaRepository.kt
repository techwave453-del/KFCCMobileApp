package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

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

    suspend fun update(
        id: Long,
        title: String,
        description: String,
        category: String,
        published: Boolean? = null,
        featured: Boolean? = null
    ): Result<AdminMediaItem> {
        return try {
            val response = adminRepository.authenticatedPatch(
                "api/media/$id",
                MediaUpdateRequest(title, description, category, published, featured)
            )
            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
                HttpStatusCode.Forbidden -> error("You do not have permission to edit media.")
                HttpStatusCode.NotFound -> error("Media item not found.")
                else -> error("Unable to update media (${response.status.value}).")
            }
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
            when (response.status) {
                in HttpStatusCode.OK..HttpStatusCode.IMUsed -> Result.success(Unit)
                HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
                HttpStatusCode.Forbidden -> error("You do not have permission to feature media.")
                else -> error("Unable to update the Featured Video (${response.status.value}).")
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun delete(id: Long): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedDelete("api/media/$id")
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> Result.success(Unit)
                HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
                HttpStatusCode.Forbidden -> error("You do not have permission to delete media.")
                HttpStatusCode.NotFound -> error("Media item not found.")
                else -> error("Unable to delete media (${response.status.value}).")
            }
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

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
            Result.success(response.body<List<AdminMediaItem>>())
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Marks/unmarks a video as featured. The server enforces video-only targets and uniqueness. */
    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedPatch(
                "api/media/$id/featured",
                FeaturedMediaRequest(featured)
            )
            if (response.status !in 200..299) {
                error("Unable to update featured media (${response.status.value}).")
            }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

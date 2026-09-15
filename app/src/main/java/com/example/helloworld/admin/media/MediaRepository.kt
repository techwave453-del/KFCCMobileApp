package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

/** Server-authoritative Media Center API client. */
class MediaRepository(context: Context) {
    private val adminRepository = AdminRepository(context.applicationContext)

    suspend fun load(): Result<List<AdminMediaItem>> = runCatching {
        val response = adminRepository.authenticatedGet("api/media")
        if (response.status != HttpStatusCode.OK) {
            error("Media service returned ${response.status.value}.")
        }
        response.body()
    }

    /**
     * Marks/unmarks a media item as featured.
     * The server remains responsible for enforcing media.edit permission.
     */
    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> = runCatching {
        val response = adminRepository.authenticatedPatch(
            "api/media/$id",
            mapOf("featured" to featured)
        )
        if (response.status !in 200..299) {
            error("Unable to update featured media (${response.status.value}).")
        }
    }
}

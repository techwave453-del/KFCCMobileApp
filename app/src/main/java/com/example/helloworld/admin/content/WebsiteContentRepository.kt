package com.example.helloworld.admin.content

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChurchInfo
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

class WebsiteContentRepository(private val adminRepository: AdminRepository) {
    private val path = "api/site/content"

    suspend fun load(): Result<ChurchInfo> = runCatching {
        val response = adminRepository.authenticatedGet(path)
        when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You are logged in, but you do not have permission to view Website Content.")
            else -> error("Website Content service returned ${response.status.value}.")
        }
    }

    suspend fun save(content: ChurchInfo): Result<ChurchInfo> = runCatching {
        val response = adminRepository.authenticatedPut(path, content)
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> response.body()
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You are logged in, but you do not have permission to edit Website Content.")
            else -> error("Unable to save Website Content (${response.status.value}).")
        }
    }
}

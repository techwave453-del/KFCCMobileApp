package com.example.helloworld.admin.identity

import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

class IdentityRepository(private val adminRepository: AdminRepository) {
    companion object {
        private const val CONTENT_PATH = "api/site/content"
    }

    suspend fun load(): Result<ChurchIdentity> = runCatching {
        val response = adminRepository.authenticatedGet(CONTENT_PATH)
        when (response.status) {
            HttpStatusCode.OK -> response.body<ChurchIdentity>()
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You are logged in, but you do not have permission to view Church Identity.")
            else -> error("Identity service returned ${response.status.value}.")
        }
    }

    suspend fun save(identity: ChurchIdentity): Result<ChurchIdentity> = runCatching {
        val body = mapOf(
            "churchName" to identity.churchName.trim(),
            "officialName" to identity.officialName.trim(),
            "logo" to identity.logo.trim(),
            "logoUrl" to identity.logoUrl.trim(),
            "officialLogo" to identity.officialLogo.trim(),
            "registrationDetails" to identity.registrationDetails.trim()
        )
        val response = adminRepository.authenticatedPut(CONTENT_PATH, body)
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> response.body<ChurchIdentity>()
            HttpStatusCode.Unauthorized -> error("Your administrator session has expired. Please login again.")
            HttpStatusCode.Forbidden -> error("You are logged in, but you do not have permission to edit Church Identity.")
            HttpStatusCode.NotFound -> error("The Church Identity API endpoint was not found.")
            else -> {
                val message = try { response.body<Map<String, String>>()["error"] } catch (_: Exception) { null }
                error(message ?: "Unable to save Church Identity (${response.status.value}).")
            }
        }
    }
}

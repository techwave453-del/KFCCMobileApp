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
        if (response.status != HttpStatusCode.OK) {
            error("Identity service returned ${response.status.value}.")
        }
        response.body<ChurchIdentity>()
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
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.Created)) {
            val message = try { response.body<Map<String, String>>()["error"] } catch (_: Exception) { null }
            error(message ?: "Unable to save Church Identity (${response.status.value}).")
        }
        response.body<ChurchIdentity>()
    }
}

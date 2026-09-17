package com.example.helloworld.auth

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChatAuthRepository

/**
 * Coordinates the two supported authentication backends without exposing
 * backend exceptions or credentials to the UI.
 *
 * Administrator authentication is attempted first because the admin API
 * accepts the same username/password pair. Member authentication remains
 * email/password until a server-side username-to-account resolver is added.
 */
class UnifiedAuthRepository(
    private val adminRepository: AdminRepository,
    private val chatRepository: ChatAuthRepository
) {
    suspend fun signIn(identifier: String, password: String): UnifiedAuthResult {
        val normalized = identifier.trim()
        require(normalized.isNotEmpty() && password.isNotEmpty()) { "Enter your username and password." }

        val admin = adminRepository.login(normalized, password)
        if (admin.ok && admin.user != null) {
            return UnifiedAuthResult.Administrator(admin.user.username)
        }

        if (normalized.contains("@")) {
            val member = chatRepository.signIn(normalized, password)
            if (member.success) {
                val profile = chatRepository.completeProfile()
                if (profile.success) {
                    val username = chatRepository.getProfile().getOrNull()?.username ?: normalized
                    return UnifiedAuthResult.Member(username)
                }
            }
        }

        throw IllegalArgumentException("Incorrect username or password.")
    }
}

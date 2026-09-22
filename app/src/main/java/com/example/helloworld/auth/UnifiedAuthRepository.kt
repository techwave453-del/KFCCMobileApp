package com.example.helloworld.auth

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChatAuthRepository

/**
 * Single sign-in coordinator.
 *
 * Registration uses an email only for email verification. After verification,
 * both account types authenticate with username + password:
 * - administrator username -> administrator session
 * - member username -> community session
 *
 * A successful member authentication never opens administration.
 */
class UnifiedAuthRepository(
    private val adminRepository: AdminRepository,
    private val chatRepository: ChatAuthRepository
) {
    suspend fun signIn(identifier: String, password: String): UnifiedAuthResult {
        val input = identifier.trim()
        require(input.isNotEmpty() && password.isNotEmpty()) {
            "Enter your email or username and password."
        }

        // 1. If it looks like an email, try direct member sign-in first.
        if (input.contains("@") && input.contains(".")) {
            val result = chatRepository.signIn(input, password)
            if (result.success) {
                chatRepository.completeProfile()
                val profile = chatRepository.getProfile().getOrNull()
                return UnifiedAuthResult.Member(profile?.username ?: input.substringBefore("@"))
            }
        }

        val username = input.removePrefix("@").lowercase()

        // 2. Administrator authentication via Edge Function.
        val admin = adminRepository.login(username, password)
        if (admin.ok && admin.user != null) {
            return UnifiedAuthResult.Administrator(admin.user.username)
        }

        // 3. Member authentication by username via Edge Function.
        val member = chatRepository.signInWithUsername(username, password)
        if (!member.success) {
            throw IllegalArgumentException(
                member.message ?: "Incorrect username or password."
            )
        }

        val profile = chatRepository.completeProfile()
        if (!profile.success) {
            throw IllegalArgumentException(
                profile.message ?: "Unable to complete your profile."
            )
        }

        val profileUsername = chatRepository.getProfile().getOrNull()?.username
            ?: username

        return UnifiedAuthResult.Member(profileUsername)
    }
}

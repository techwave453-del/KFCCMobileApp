package com.example.helloworld.auth

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChatAuthRepository

/**
 * Coordinates administrator and member authentication.
 *
 * The identifier determines which member authentication path is used:
 * - email: Supabase email/password authentication
 * - username: the server-side chat-login resolver
 *
 * Administrator username authentication is still attempted first so an
 * administrator can use the same sign-in form.
 */
class UnifiedAuthRepository(
    private val adminRepository: AdminRepository,
    private val chatRepository: ChatAuthRepository
) {
    suspend fun signIn(identifier: String, password: String): UnifiedAuthResult {
        val normalized = identifier.trim()
        require(normalized.isNotEmpty() && password.isNotEmpty()) {
            "Enter your username and password."
        }

        // Preserve administrator sign-in through the existing admin auth path.
        val admin = adminRepository.login(normalized, password)
        if (admin.ok && admin.user != null) {
            return UnifiedAuthResult.Administrator(admin.user.username)
        }

        // Members may sign in with either email or username.
        val member = if (normalized.contains("@")) {
            chatRepository.signIn(normalized, password)
        } else {
            chatRepository.signInWithUsername(normalized, password)
        }

        if (!member.success) {
            throw IllegalArgumentException(member.message ?: "Incorrect username or password.")
        }

        // Complete the member profile only after authentication has produced
        // a valid Supabase session.
        val profile = chatRepository.completeProfile()
        if (!profile.success) {
            throw IllegalArgumentException(
                profile.message ?: "Unable to complete your profile."
            )
        }

        val username = chatRepository.getProfile().getOrNull()?.username
            ?: normalized.removePrefix("@").lowercase()

        return UnifiedAuthResult.Member(username)
    }
}

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
        val username = identifier.trim().removePrefix("@").lowercase()
        require(username.isNotEmpty() && password.isNotEmpty()) {
            "Enter your username and password."
        }

        // Administrator authentication is handled by the Supabase admin-login
        // function. It validates the legacy admin username/password and creates
        // a normal Supabase Auth session for the mobile app.
        val admin = adminRepository.login(username, password)
        if (admin.ok && admin.user != null) {
            return UnifiedAuthResult.Administrator(admin.user.username)
        }

        // If it is not an administrator account, authenticate as a regular
        // community member using the username resolver. Email is deliberately
        // not used as the member sign-in identifier.
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

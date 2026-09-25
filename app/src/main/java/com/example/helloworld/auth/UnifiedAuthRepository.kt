package com.example.helloworld.auth

import android.util.Log
import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.ChatAuthRepository
import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.notifications.DeviceTokenRepository
import com.example.helloworld.notifications.KfccNotificationScheduler
import kotlinx.coroutines.delay

/**
 * Single sign-in coordinator.
 */
class UnifiedAuthRepository(
    private val adminRepository: AdminRepository,
    private val chatRepository: ChatAuthRepository
) {
    private suspend fun registerDeviceForPushNotifications() {
        val repository = DeviceTokenRepository()
        val firstAttempt = repository.registerCurrentToken()
        if (firstAttempt.isSuccess) return

        delay(750)
        repository.registerCurrentToken().onFailure {
            Log.e(TAG, "Push registration still failed after login", it)
        }
    }

    private fun scheduleSignInNotification() {
        KfccNotificationScheduler.deliverSignInDefault(KfccDataContext.appContext)
    }

    suspend fun signIn(identifier: String, password: String): UnifiedAuthResult {
        val input = identifier.trim()
        require(input.isNotEmpty() && password.isNotEmpty()) {
            "Enter your email or username and password."
        }

        if (input.contains("@") && input.contains(".")) {
            val result = chatRepository.signIn(input, password)
            if (result.success) {
                chatRepository.completeProfile()
                val profile = chatRepository.getProfile().getOrNull()
                registerDeviceForPushNotifications()
                scheduleSignInNotification()
                return UnifiedAuthResult.Member(profile?.username ?: input.substringBefore("@"))
            }
        }

        val username = input.removePrefix("@").lowercase()

        val admin = adminRepository.login(username, password)
        if (admin.ok && admin.user != null) {
            registerDeviceForPushNotifications()
            scheduleSignInNotification()
            return UnifiedAuthResult.Administrator(admin.user.username)
        }

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

        val profileUsername = chatRepository.getProfile().getOrNull()?.username ?: username

        registerDeviceForPushNotifications()
        scheduleSignInNotification()
        return UnifiedAuthResult.Member(profileUsername)
    }

    private companion object {
        const val TAG = "KfccAuth"
    }
}

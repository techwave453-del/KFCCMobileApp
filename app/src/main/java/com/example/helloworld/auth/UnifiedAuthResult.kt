package com.example.helloworld.auth

/** The destination selected after a successful unified sign-in. */
sealed interface UnifiedAuthResult {
    data class Administrator(val username: String) : UnifiedAuthResult
    data class Member(val username: String) : UnifiedAuthResult
}

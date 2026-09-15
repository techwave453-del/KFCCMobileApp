package com.example.helloworld.config

/**
 * Central application configuration.
 *
 * Public endpoints belong here; secrets must never be committed to the APK.
 * Supabase credentials should be supplied through a secure build configuration
 * once the mobile data layer is enabled.
 */
object AppConfig {
    const val APP_NAME = "Kingdom Fellowship Christian Church"
    const val SHORT_NAME = "KFCC"

    const val ADMIN_API_BASE_URL =
        "https://kingdomfellowshipchristianchurch.onrender.com/"

    /** Canonical origin sent by the native client for the server's same-origin checks. */
    const val ADMIN_API_ORIGIN =
        "https://kingdomfellowshipchristianchurch.onrender.com"

    /**
     * The web server currently uses __Host-kfc.sid in production and kfc.sid
     * in development. Keep the native client aware of both names, while also
     * accepting the older connect.sid name for backwards compatibility.
     */
    val ADMIN_SESSION_COOKIE_NAMES = listOf(
        "__Host-kfc.sid",
        "kfc.sid",
        "connect.sid"
    )

    const val ADMIN_ME_PATH = "api/admin/me"
    const val ADMIN_LOGIN_PATH = "api/admin/login"
    const val ADMIN_LOGOUT_PATH = "api/admin/logout"
}

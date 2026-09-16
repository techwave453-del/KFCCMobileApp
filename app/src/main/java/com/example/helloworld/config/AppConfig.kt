package com.example.helloworld.config

import com.example.helloworld.BuildConfig

/**
 * Central application configuration.
 *
 * Public endpoints belong here. The Supabase publishable key is injected at
 * build time from local.properties or the CI environment; never use a
 * service-role/secret key in the Android app.
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

    /** Supabase client key. This must be a publishable/anon key, never a secret key. */
    val SUPABASE_PUBLISHABLE_KEY: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY
}

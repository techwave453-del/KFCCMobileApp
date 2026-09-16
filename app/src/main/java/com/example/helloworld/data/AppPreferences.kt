package com.example.helloworld.data

import android.content.Context

/**
 * Small persistent preference store for app-level UI and notification settings.
 * Uses Android SharedPreferences so these values survive app restarts without
 * introducing another persistence dependency.
 */
class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var theme: String
        get() = preferences.getString(KEY_THEME, "System") ?: "System"
        set(value) = preferences.edit().putString(KEY_THEME, value).apply()

    var audioAutoplay: Boolean
        get() = preferences.getBoolean(KEY_AUDIO_AUTOPLAY, true)
        set(value) = preferences.edit().putBoolean(KEY_AUDIO_AUTOPLAY, value).apply()

    var chatNotifications: Boolean
        get() = preferences.getBoolean(KEY_CHAT_NOTIFICATIONS, true)
        set(value) = preferences.edit().putBoolean(KEY_CHAT_NOTIFICATIONS, value).apply()

    var churchNotifications: Boolean
        get() = preferences.getBoolean(KEY_CHURCH_NOTIFICATIONS, true)
        set(value) = preferences.edit().putBoolean(KEY_CHURCH_NOTIFICATIONS, value).apply()

    private companion object {
        const val PREFS_NAME = "kfcc_app_preferences"
        const val KEY_THEME = "theme"
        const val KEY_AUDIO_AUTOPLAY = "audio_autoplay"
        const val KEY_CHAT_NOTIFICATIONS = "chat_notifications"
        const val KEY_CHURCH_NOTIFICATIONS = "church_notifications"
    }
}

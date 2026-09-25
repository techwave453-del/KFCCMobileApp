package com.example.helloworld.data

import android.content.Context

/** Small persistent store for user-facing app preferences. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("kfcc_preferences", Context.MODE_PRIVATE)

    var theme: String
        get() = prefs.getString(KEY_THEME, "System") ?: "System"
        set(value) { prefs.edit().putString(KEY_THEME, value).apply() }

    var audioAutoplay: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_AUTOPLAY, true)
        set(value) { prefs.edit().putBoolean(KEY_AUDIO_AUTOPLAY, value).apply() }

    var chatNotifications: Boolean
        get() = prefs.getBoolean(KEY_CHAT_NOTIFICATIONS, true)
        set(value) { prefs.edit().putBoolean(KEY_CHAT_NOTIFICATIONS, value).apply() }

    var churchNotifications: Boolean
        get() = prefs.getBoolean(KEY_CHURCH_NOTIFICATIONS, true)
        set(value) { prefs.edit().putBoolean(KEY_CHURCH_NOTIFICATIONS, value).apply() }

    var installationId: String?
        get() = prefs.getString(KEY_INSTALLATION_ID, null)
        set(value) { prefs.edit().putString(KEY_INSTALLATION_ID, value).apply() }

    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_FCM_TOKEN, value).apply() }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_AUDIO_AUTOPLAY = "audio_autoplay"
        private const val KEY_CHAT_NOTIFICATIONS = "chat_notifications"
        private const val KEY_CHURCH_NOTIFICATIONS = "church_notifications"
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}

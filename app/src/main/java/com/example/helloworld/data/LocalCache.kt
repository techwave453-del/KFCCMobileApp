package com.example.helloworld.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Small persistent cache for public Supabase data.
 * SharedPreferences keeps this dependency-free and survives process death.
 */
object LocalCache {
    private const val PREFS = "kfcc_public_cache"
    private const val CHURCH_INFO = "church_info"
    private const val MEDIA_ITEMS = "media_items"

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var prefs: android.content.SharedPreferences

    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun ready(): Boolean = ::prefs.isInitialized

    fun readChurchInfo(): ChurchInfo? {
        if (!ready()) return null
        return prefs.getString(CHURCH_INFO, null)?.let {
            runCatching { json.decodeFromString<ChurchInfo>(it) }.getOrNull()
        }
    }

    fun writeChurchInfo(value: ChurchInfo) {
        if (!ready()) return
        prefs.edit().putString(CHURCH_INFO, json.encodeToString(value)).apply()
    }

    fun readMedia(): List<MediaItem>? {
        if (!ready()) return null
        return prefs.getString(MEDIA_ITEMS, null)?.let {
            runCatching { json.decodeFromString<List<MediaItem>>(it) }.getOrNull()
        }
    }

    fun writeMedia(value: List<MediaItem>) {
        if (!ready()) return
        prefs.edit().putString(MEDIA_ITEMS, json.encodeToString(value)).apply()
    }
}

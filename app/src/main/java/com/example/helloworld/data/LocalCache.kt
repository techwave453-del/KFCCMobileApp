package com.example.helloworld.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LocalCache {
    private const val PREFS_NAME = "kfcc_cache"
    private const val KEY_CHURCH_INFO = "church_info"
    private const val KEY_MEDIA_ITEMS = "media_items"
    
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun writeChurchInfo(info: ChurchInfo) {
        prefs.edit().putString(KEY_CHURCH_INFO, json.encodeToString(info)).apply()
    }

    fun readChurchInfo(): ChurchInfo? {
        val raw = prefs.getString(KEY_CHURCH_INFO, null) ?: return null
        return try { json.decodeFromString(raw) } catch (_: Exception) { null }
    }

    fun writeMedia(media: List<MediaItem>) {
        prefs.edit().putString(KEY_MEDIA_ITEMS, json.encodeToString(media)).apply()
    }

    fun readMedia(): List<MediaItem>? {
        val raw = prefs.getString(KEY_MEDIA_ITEMS, null) ?: return null
        return try { json.decodeFromString(raw) } catch (_: Exception) { null }
    }
}

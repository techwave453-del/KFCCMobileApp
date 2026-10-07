package com.example.helloworld.updates

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.helloworld.BuildConfig
import com.example.helloworld.admin.AppUpdateConfig
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUpdateManager {
    private const val PREFS = "kfcc_app_updates"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_VERSION_CODE = "version_code"

    suspend fun checkAndSchedule(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching {
                val config = SupabaseProvider.client
                    .from("app_update_config")
                    .select()
                    .decodeSingle<AppUpdateConfig>()

                if (!config.isEnabled || config.versionCode <= BuildConfig.VERSION_CODE) return@runCatching

                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val existingId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
                val existingVersion = prefs.getInt(KEY_VERSION_CODE, -1)

                if (existingId != -1L && existingVersion == config.versionCode) return@runCatching

                val url = normalizeDownloadUrl(config.downloadUrl)
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("Kanisa ${config.versionName}")
                    .setDescription("Downloading the latest app update")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalFilesDir(
                        context,
                        Environment.DIRECTORY_DOWNLOADS,
                        "kanisa-${config.versionCode}.apk"
                    )

                val manager = context.getSystemService(DownloadManager::class.java)
                val id = manager.enqueue(request)
                prefs.edit()
                    .putLong(KEY_DOWNLOAD_ID, id)
                    .putInt(KEY_VERSION_CODE, config.versionCode)
                    .apply()
            }
        }
    }

    fun getTrackedDownloadId(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_DOWNLOAD_ID, -1L)

    fun clearTrackedDownload(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DOWNLOAD_ID)
            .remove(KEY_VERSION_CODE)
            .apply()
    }

    fun normalizeDownloadUrl(raw: String): String {
        val value = raw.trim()
        val match = Regex("""drive\.google\.com/file/d/([^/]+)""").find(value)
        val id = match?.groupValues?.getOrNull(1)
            ?: Regex("""[?&]id=([^&]+)""").find(value)?.groupValues?.getOrNull(1)

        return if (id != null) {
            "https://drive.google.com/uc?export=download&id=$id"
        } else {
            value
        }
    }
}

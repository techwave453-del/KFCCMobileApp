package com.example.helloworld.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
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

    suspend fun getMandatoryUpdate(): AppUpdateConfig? = withContext(Dispatchers.IO) {
        val config = SupabaseProvider.client
            .from("app_update_config")
            .select()
            .decodeSingle<AppUpdateConfig>()

        if (config.isEnabled && config.versionCode > BuildConfig.VERSION_CODE) config else null
    }

    suspend fun checkAndSchedule(context: Context): AppUpdateConfig? {
        val config = getMandatoryUpdate() ?: return null
        withContext(Dispatchers.IO) {
            scheduleDownloadIfNeeded(context.applicationContext, config)
        }
        return config
    }

    private fun scheduleDownloadIfNeeded(context: Context, config: AppUpdateConfig) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existingId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        val existingVersion = prefs.getInt(KEY_VERSION_CODE, -1)

        if (existingId != -1L && existingVersion == config.versionCode) {
            val manager = context.getSystemService(DownloadManager::class.java)
            val query = DownloadManager.Query().setFilterById(existingId)
            manager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_PENDING,
                        DownloadManager.STATUS_RUNNING,
                        DownloadManager.STATUS_PAUSED,
                        DownloadManager.STATUS_SUCCESSFUL -> return
                    }
                }
            }
            clearTrackedDownload(context)
        }

        val url = normalizeDownloadUrl(config.downloadUrl)
        require(url.isNotBlank()) { "The published update does not have a download URL." }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Kanisa ${config.versionName}")
            .setDescription("Required app update")
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

    fun installTrackedDownload(context: Context): Boolean {
        val downloadId = getTrackedDownloadId(context)
        if (downloadId == -1L) return false

        val manager = context.getSystemService(DownloadManager::class.java)
        val query = DownloadManager.Query().setFilterById(downloadId)

        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) {
                clearTrackedDownload(context)
                return false
            }

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) return false

            val uri = manager.getUriForDownloadedFile(downloadId) ?: return false

            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return false
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            return true
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

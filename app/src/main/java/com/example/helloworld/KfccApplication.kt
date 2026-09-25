package com.example.helloworld

import android.app.Application
import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.data.LocalCache
import com.example.helloworld.data.bible.BibleOfflineSeeder
import com.example.helloworld.data.bible.BibleSyncScheduler
import com.example.helloworld.data.offline.KfccContentSyncScheduler
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.notifications.KfccNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class KfccApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KfccDataContext.initialize(this)
        LocalCache.initialize(this)
        KfccDatabase.getInstance(this)

        runBlocking(Dispatchers.IO) {
            BibleOfflineSeeder.seedIfNeeded(this@KfccApplication)
        }

        BibleSyncScheduler.schedule(this)
        KfccContentSyncScheduler.schedule(this)
        KfccNotificationScheduler.schedule(this)
    }
}

package com.example.helloworld

import android.app.Application
import com.example.helloworld.data.LocalCache
import com.example.helloworld.data.bible.BibleSyncScheduler
import com.example.helloworld.data.offline.KfccDatabase

class KfccApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalCache.initialize(this)
        KfccDatabase.getInstance(this)
        BibleSyncScheduler.schedule(this)
    }
}

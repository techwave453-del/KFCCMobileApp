package com.example.helloworld.data

import android.content.Context

object KfccDataContext {
    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}

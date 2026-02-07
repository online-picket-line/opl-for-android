package com.onlinepicketline.opl

import android.app.Application

/**
 * Application class for initialization.
 */
class OplApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OplApplication
            private set
    }
}

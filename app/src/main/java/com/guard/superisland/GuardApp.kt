package com.guard.superisland

import android.app.Application
import com.guard.superisland.notify.Notifier

class GuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
    }
}

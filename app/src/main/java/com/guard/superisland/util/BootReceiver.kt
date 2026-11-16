package com.guard.superisland.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.guard.superisland.service.GuardService

/** 开机自启守护服务 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            GuardService.start(context)
        }
    }
}

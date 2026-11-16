package com.guard.superisland.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.guard.superisland.service.GuardService

/** 备用触发通道：收到广播后转交 GuardService */
class TriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = intent.getStringExtra(Triggers.EXTRA_REASON) ?: Triggers.REASON_APP_LOCK
        GuardService.trigger(context, reason)
    }
}

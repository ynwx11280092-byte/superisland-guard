package com.guard.superisland.util

object Triggers {
    /** 本应用内部：服务触发动作 */
    const val ACTION_TRIGGER = "com.guard.superisland.action.TRIGGER"
    /** 广播：应用锁进入（备用通道） */
    const val ACTION_APP_LOCK = "com.guard.superisland.action.APP_LOCK"
    const val EXTRA_REASON = "reason"
    const val REASON_APP_LOCK = "applock"
    const val REASON_MANUAL = "manual"
}

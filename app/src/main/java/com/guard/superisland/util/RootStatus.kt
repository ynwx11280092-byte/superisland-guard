package com.guard.superisland.util

import android.content.Context
import java.io.File

/**
 * 本机 Root / LSPosed 状态检测，供界面状态卡展示。
 */
object RootStatus {

    private const val PREFS_HOOK = "hook_status"
    private const val KEY_HOOK_ACTIVE_AT = "hook_active_at"

    /** 是否存在 su 二进制（Root 可用性粗略判断） */
    fun suBinaryPresent(): Boolean {
        val paths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/data/local/su", "/data/local/bin/su", "/vendor/bin/su", "/system/sbin/su"
        )
        return paths.any { File(it).exists() }
    }

    /** LSPosed 管理器是否已安装 */
    fun isLsposedInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("org.lsposed.manager", 0); true
        } catch (_: Throwable) {
            try {
                pm.getPackageInfo("io.github.lsposed.manager", 0); true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** LSPosed hook 在安全中心进程上报「已注入」 */
    fun reportHookActive(context: Context) {
        context.getSharedPreferences(PREFS_HOOK, Context.MODE_PRIVATE)
            .edit().putLong(KEY_HOOK_ACTIVE_AT, System.currentTimeMillis()).apply()
    }

    fun hookActiveAt(context: Context): Long =
        context.getSharedPreferences(PREFS_HOOK, Context.MODE_PRIVATE)
            .getLong(KEY_HOOK_ACTIVE_AT, 0L)

    /** 24 小时内上报过即认为 hook 生效 */
    fun isHookActive(context: Context): Boolean {
        val at = hookActiveAt(context)
        return at > 0 && (System.currentTimeMillis() - at) < 24L * 3600_000L
    }
}

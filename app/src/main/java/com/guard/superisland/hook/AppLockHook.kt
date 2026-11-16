package com.guard.superisland.hook

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * 在「小米安全中心」进程内 hook Activity.onResume，
 * 检测进入应用锁页 com.miui.applicationlock.AppLockActivity。
 *
 * 注意：这里只做「检测 + 触发」，不直接碰摄像头；
 * 抓拍由本应用的 GuardService 在前台服务中执行（带可见提示）。
 */
object AppLockHook {

    private const val TARGET_ACTIVITY = "com.miui.applicationlock.AppLockActivity"
    private const val PROVIDER_URI = "content://com.guard.superisland.trigger"

    private var lastTrigger = 0L

    fun hook(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        if (activity.javaClass.name == TARGET_ACTIVITY) {
                            trigger(activity)
                        }
                    }
                }
            )
            XposedBridge.log("[SuperIslandGuard] Activity.onResume hook installed in securitycenter")
            reportHookActive()
        } catch (t: Throwable) {
            XposedBridge.log("[SuperIslandGuard] hook failed: ${t.message}")
        }
    }

    /** 通过 ContentProvider 向本应用上报「hook 已注入」，供界面状态展示 */
    private fun reportHookActive() {
        try {
            val at = Class.forName("android.app.ActivityThread")
                .getMethod("currentActivityThread").invoke(null)
            val app = at.javaClass.getMethod("getApplication").invoke(at) as? android.content.Context
                ?: return
            app.contentResolver.call(Uri.parse(PROVIDER_URI), "hookAlive", null, null)
        } catch (t: Throwable) {
            XposedBridge.log("[SuperIslandGuard] reportHookActive failed: ${t.message}")
        }
    }

    private fun trigger(activity: Activity) {
        val now = System.currentTimeMillis()
        // 去抖：短时间内重复 onResume 只触发一次
        if (now - lastTrigger < 3000L) return
        lastTrigger = now
        try {
            val cr: ContentResolver = activity.contentResolver
            cr.call(
                Uri.parse(PROVIDER_URI),
                "appLockEntered",
                null,
                Bundle().apply { putLong("timestamp", now) }
            )
            XposedBridge.log("[SuperIslandGuard] appLock trigger sent")
        } catch (t: Throwable) {
            XposedBridge.log("[SuperIslandGuard] trigger failed: ${t.message}")
        }
    }
}

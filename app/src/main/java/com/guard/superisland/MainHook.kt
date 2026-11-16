package com.guard.superisland

import com.guard.superisland.hook.AppLockHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LSPosed 模块入口。
 * 只在「小米安全中心」进程内注入，检测进入应用锁页面。
 */
class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.miui.securitycenter") {
            try {
                AppLockHook.hook(lpparam.classLoader)
            } catch (t: Throwable) {
                XposedBridge.log("[SuperIslandGuard] handleLoadPackage error: ${t.message}")
            }
        }
    }
}

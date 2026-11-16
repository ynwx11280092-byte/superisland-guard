package com.guard.superisland

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import com.guard.superisland.service.GuardService
import com.guard.superisland.util.Triggers

/**
 * 供 LSPosed hook（运行在 com.miui.securitycenter 进程）调用的触发入口。
 * 用 ContentResolver.call 而不是广播，避开 Android 14+ 的隐式广播限制。
 */
class TriggerProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        // 只允许「小米安全中心」或本应用触发，防止任意第三方应用滥用
        val caller = ctx.packageManager.getNameForUid(Binder.getCallingUid())
        val allowed = caller == "com.miui.securitycenter" || caller == "com.guard.superisland"
        if (!allowed) return null

        if (method == "appLockEntered") {
            GuardService.trigger(ctx, Triggers.REASON_APP_LOCK)
        }
        return Bundle()
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

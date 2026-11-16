package com.guard.superisland.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.guard.superisland.config.GuardConfig
import com.guard.superisland.notify.Notifier
import com.guard.superisland.util.Triggers

/**
 * 常驻守护服务（specialUse 前台类型）：
 * - 空闲：只挂一个低优先级常驻通知，零摄像头、零唤醒锁 → 省电；
 * - 收到触发（ContentProvider 调用 / 广播）时，拉起 CaptureService 真正抓拍。
 */
class GuardService : Service() {

    companion object {
        private var instance: GuardService? = null

        fun start(context: Context) {
            val i = Intent(context, GuardService::class.java)
            ContextCompat.startForegroundService(context, i)
        }

        fun trigger(context: Context, reason: String) {
            val i = Intent(context, GuardService::class.java).apply {
                action = Triggers.ACTION_TRIGGER
                putExtra(Triggers.EXTRA_REASON, reason)
            }
            if (instance != null) {
                context.startService(i)
            } else {
                try {
                    ContextCompat.startForegroundService(context, i)
                } catch (_: Throwable) {
                    context.startService(i)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(Notifier.ID_GUARD, Notifier.showIdle(this))

        if (intent?.action == Triggers.ACTION_TRIGGER) {
            val reason = intent.getStringExtra(Triggers.EXTRA_REASON) ?: Triggers.REASON_APP_LOCK
            startCapture(reason)
        }
        return START_STICKY
    }

    private fun startCapture(reason: String) {
        if (!GuardConfig.enabled(this)) return
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 无相机权限：不抓拍（守护继续运行）。首次启动请在界面授权。
            return
        }
        val i = Intent(this, CaptureService::class.java).apply {
            putExtra(Triggers.EXTRA_REASON, reason)
        }
        ContextCompat.startForegroundService(this, i)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

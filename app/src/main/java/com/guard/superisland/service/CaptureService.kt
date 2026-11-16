package com.guard.superisland.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.guard.superisland.camera.CaptureEngine
import com.guard.superisland.config.GuardConfig
import com.guard.superisland.island.IslandOverlay
import com.guard.superisland.notify.Notifier
import com.guard.superisland.util.FlashOverlay

/**
 * 抓拍服务（camera 前台类型）：仅触发期间运行。
 * 可见提示：快门声(引擎内) + 全屏闪光 + 常驻「正在取证」通知 + 超级岛通知。
 * 结束后立即释放摄像头与唤醒锁并自我停止（省电）。
 */
class CaptureService : Service() {

    private val engine by lazy { CaptureEngine(this) }
    private var wakelock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(Notifier.ID_CAPTURE, Notifier.showCapture(this, 0, 0))
        beginCapture()
        return START_NOT_STICKY
    }

    private fun beginCapture() {
        acquireWakelock()
        vibrate()
        if (GuardConfig.flashOverlay(this)) FlashOverlay.flash(this)

        IslandOverlay.show(this, GuardConfig.theme(this), "准备取证…")
        Notifier.notifyCapture(this, 0, 0)

        engine.start(this, object : CaptureEngine.Callback {
            override fun onPhoto(count: Int, seconds: Int) {
                if (GuardConfig.flashOverlay(this@CaptureService)) {
                    FlashOverlay.flash(this@CaptureService)
                }
                Notifier.notifyCapture(this@CaptureService, count, seconds)
                IslandOverlay.update("已抓拍 $count 张 · ${seconds}s")
            }

            override fun onFinished(totalCount: Int, totalSeconds: Int, ok: Boolean, err: String?) {
                releaseWakelock()
                Notifier.cancelCapture(this@CaptureService)
                if (ok) {
                    IslandOverlay.update("取证完成：$totalCount 项 / ${totalSeconds}s")
                } else {
                    IslandOverlay.update("取证失败：${err ?: "未知错误"}")
                }
                IslandOverlay.hideDelayed(3000)
                stopSelf()
            }
        })
    }

    private fun vibrate() {
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {
        }
    }

    private fun acquireWakelock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "superisland:capture")
        wakelock?.acquire((GuardConfig.maxDurationSec(this) + 10) * 1000L)
    }

    private fun releaseWakelock() {
        try {
            if (wakelock?.isHeld == true) wakelock?.release()
        } catch (_: Throwable) {
        }
        wakelock = null
    }

    override fun onDestroy() {
        engine.stop()
        releaseWakelock()
        FlashOverlay.remove()
        IslandOverlay.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

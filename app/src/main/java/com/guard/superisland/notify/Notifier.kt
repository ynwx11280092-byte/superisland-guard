package com.guard.superisland.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.guard.superisland.R
import com.guard.superisland.ui.MainActivity

/**
 * 通知层：
 * 1) 空闲守护通知（常驻、低优先级）
 * 2) 取证拍摄通知（高优先级、常驻，显示 张数/秒数）
 *
 * 与 HyperIsland 的桥接：这里发的是标准 Android 通知，
 * HyperIsland（已安装并授权）会把这些通知拦截并渲染成
 * 原生超级岛样式（柔光 / 液态玻璃由 HyperIsland 材质决定）。
 */
object Notifier {

    const val CHANNEL_GUARD = "guard"
    const val CHANNEL_CAPTURE = "capture"
    const val ID_GUARD = 1001
    const val ID_CAPTURE = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GUARD,
                context.getString(R.string.notif_channel_guard),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val capture = NotificationChannel(
            CHANNEL_CAPTURE,
            context.getString(R.string.notif_channel_capture),
            NotificationManager.IMPORTANCE_HIGH
        )
        capture.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(capture)
    }

    private fun contentIntent(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showIdle(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_GUARD)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_guard_title))
            .setContentText(context.getString(R.string.notif_guard_text))
            .setContentIntent(contentIntent(context))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    fun showCapture(context: Context, count: Int, seconds: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_capture_title))
            .setContentText(context.getString(R.string.notif_capture_text, count, seconds))
            .setContentIntent(contentIntent(context))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    fun notifyCapture(context: Context, count: Int, seconds: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ID_CAPTURE, showCapture(context, count, seconds))
    }

    fun cancelCapture(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID_CAPTURE)
    }
}

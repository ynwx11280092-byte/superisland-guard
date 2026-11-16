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
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

/**
 * 通知层（原生超级岛方案）：
 * - 空闲守护通知（常驻、低优先级）
 * - 取证拍摄通知：在支持 HyperOS 原生超级岛的设备上，用官方 hyperisland_kit
 *   发「焦点通知」，由系统 SystemUI 原生渲染成超级岛（非悬浮窗）；
 *   不支持时降级为普通通知。
 */
object Notifier {

    const val CHANNEL_GUARD = "guard"
    const val CHANNEL_CAPTURE = "capture"
    const val ID_GUARD = 1001
    const val ID_CAPTURE = 1002

    private const val ISLAND_BUSINESS = "superisland_guard"

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

    /** 设备是否支持 HyperOS 原生超级岛（焦点通知） */
    fun isIslandSupported(context: Context): Boolean =
        try {
            HyperIslandNotification.isSupported(context)
        } catch (_: Throwable) {
            false
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

    /** 构建取证通知：优先原生超级岛，不支持则普通通知 */
    fun buildCaptureNotification(context: Context, count: Int, seconds: Int): Notification {
        val title = context.getString(R.string.notif_capture_title)
        val text = context.getString(R.string.notif_capture_text, count, seconds)
        if (isIslandSupported(context)) {
            buildIslandNotification(context, title, text)?.let { return it }
        }
        return buildRegularNotification(context, title, text)
    }

    private fun buildRegularNotification(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent(context))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    /** 用官方 hyperisland_kit 构建原生超级岛（焦点）通知 */
    private fun buildIslandNotification(context: Context, title: String, text: String): Notification? {
        return try {
            val builder = HyperIslandNotification.Builder(context, ISLAND_BUSINESS, title)
            builder.addPicture(HyperPicture("guard_icon", context, R.drawable.ic_launcher))
            builder.setSmallIsland("guard_icon")
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(type = 1, pic = "guard_icon"),
                    textInfo = TextInfo(title = title),
                ),
                right = ImageTextInfoRight(
                    type = 2,
                    textInfo = TextInfo(title = text),
                ),
            )
            builder.setIslandConfig(timeout = 5, dismissible = true)
            builder.setShowNotification(true)

            val extras = builder.buildResourceBundle()
            val jsonParam = builder.buildJsonParam()

            val notif = Notification.Builder(context, CHANNEL_CAPTURE)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(contentIntent(context))
                .build()

            notif.extras.putAll(extras)
            notif.extras.putString("miui.focus.param", jsonParam)
            notif.extras.putBoolean("hyperisland_focus_proxy", true)
            notif
        } catch (_: Throwable) {
            null
        }
    }

    fun notifyCapture(context: Context, count: Int, seconds: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ID_CAPTURE, buildCaptureNotification(context, count, seconds))
    }

    fun cancelCapture(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ID_CAPTURE)
    }
}

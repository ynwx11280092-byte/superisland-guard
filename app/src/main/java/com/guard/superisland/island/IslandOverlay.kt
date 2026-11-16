package com.guard.superisland.island

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * 独立超级岛兜底：一个顶部居中的胶囊悬浮气泡，
 * 显示「已抓拍 N 张 · T 秒」等进度。需要悬浮窗权限。
 * 与 HyperIsland 并存时，HyperIsland 提供原生超级岛，此为补充。
 */
object IslandOverlay {

    private var root: View? = null
    private var textView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    fun show(context: Context, theme: String, text: String) {
        handler.post {
            removeNow()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val tv = TextView(context).apply {
                this.text = text
                setTextColor(0xFF101418.toInt())
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                minWidth = dp(context, 150)
                minHeight = dp(context, 34)
                setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8))
                background = IslandStyle.background(theme, dp(context, 17).toFloat())
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= 26)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.y = dp(context, 8)
            try {
                wm.addView(tv, lp)
                root = tv
                textView = tv
            } catch (_: Throwable) {
                // 无悬浮窗权限：静默，仍靠通知 + HyperIsland 岛
            }
        }
    }

    fun update(text: String) {
        handler.post { textView?.text = text }
    }

    fun hideDelayed(delayMs: Long) {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = Runnable { removeNow() }
        handler.postDelayed(hideRunnable!!, delayMs)
    }

    fun hide() {
        handler.post { removeNow() }
    }

    private fun removeNow() {
        val v = root ?: return
        try {
            val wm = v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(v)
        } catch (_: Throwable) {
        }
        root = null
        textView = null
    }

    private fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}

package com.guard.superisland.util

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout

/**
 * 全屏闪光提示：抓拍时闪一下白屏，明确的可见提示（需要悬浮窗权限）。
 * 无权限时静默跳过，仍保留快门声 + 通知 + 系统相机绿点等提示。
 */
object FlashOverlay {

    private var overlay: View? = null

    @Suppress("DEPRECATION")
    fun flash(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val v = FrameLayout(context).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        try {
            wm.addView(v, lp)
            overlay = v
            val anim = AlphaAnimation(0.9f, 0f)
            anim.duration = 500
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {}
                override fun onAnimationEnd(a: Animation?) = remove()
                override fun onAnimationRepeat(a: Animation?) {}
            })
            v.startAnimation(anim)
        } catch (_: Throwable) {
            // 无悬浮窗权限：忽略，其余提示仍生效
        }
    }

    fun remove() {
        val v = overlay ?: return
        try {
            val wm = v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(v)
        } catch (_: Throwable) {
        }
        overlay = null
    }
}

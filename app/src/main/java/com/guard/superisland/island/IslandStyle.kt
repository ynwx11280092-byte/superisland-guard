package com.guard.superisland.island

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.guard.superisland.config.GuardConfig

/**
 * 超级岛两种材质（自带的轻量兜底实现）：
 * - 柔光 SoftGlass：半透明白雾渐变 + 细描边，柔和发光；
 * - 液态玻璃 LiquidGlass：多段折射渐变（顶部高光 + 底部淡蓝折射），仿玻璃反光。
 *
 * 说明：这是「无 HyperIsland 时的独立岛兜底」。装了 HyperIsland 后，
 * 真正的原生超级岛 + 高清柔光/液态玻璃材质由 HyperIsland 渲染，
 * 本模块的 theme 设置可与之对应切换。
 */
object IslandStyle {

    fun background(theme: String, cornerRadiusPx: Float): Drawable =
        if (theme == GuardConfig.THEME_LIQUID) liquidGlass(cornerRadiusPx)
        else softGlass(cornerRadiusPx)

    private fun softGlass(r: Float): Drawable =
        GradientDrawable().apply {
            cornerRadius = r
            colors = intArrayOf(0x59FFFFFF.toInt(), 0x21FFFFFF.toInt())
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke(1, 0x80FFFFFF.toInt())
        }

    private fun liquidGlass(r: Float): Drawable =
        GradientDrawable().apply {
            cornerRadius = r
            // 左上高光 → 通透中部 → 淡蓝折射 → 底部透白
            colors = intArrayOf(
                0x99FFFFFF.toInt(), 0x22FFFFFF.toInt(),
                0x3DB4E5FF.toInt(), 0x14FFFFFF.toInt()
            )
            orientation = GradientDrawable.Orientation.TL_BR
            setStroke(1, 0xAAFFFFFF.toInt())
        }
}


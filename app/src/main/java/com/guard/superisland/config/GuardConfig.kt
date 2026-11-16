package com.guard.superisland.config

import android.content.Context
import androidx.preference.PreferenceManager

object GuardConfig {
    const val KEY_ENABLED = "enabled"
    const val KEY_MODE = "mode"
    const val KEY_INTERVAL = "interval_sec"
    const val KEY_MAX_COUNT = "max_count"
    const val KEY_MAX_DURATION = "max_duration_sec"
    const val KEY_THEME = "theme"
    const val KEY_FLASH = "flash_overlay"
    const val KEY_SHUTTER = "shutter_sound"
    const val KEY_GALLERY = "save_to_gallery"

    const val MODE_PHOTO = "photo"
    const val MODE_VIDEO = "video"
    const val THEME_SOFT = "softglass"
    const val THEME_LIQUID = "liquidglass"

    data class Config(
        val mode: String,
        val intervalSec: Int,
        val maxCount: Int,
        val maxDurationSec: Int,
        val saveToGallery: Boolean,
        val shutterSound: Boolean,
    )

    fun sp(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun enabled(context: Context) = sp(context).getBoolean(KEY_ENABLED, true)
    fun mode(context: Context) = sp(context).getString(KEY_MODE, MODE_PHOTO) ?: MODE_PHOTO
    fun intervalSec(context: Context) = sp(context).getInt(KEY_INTERVAL, 2)
    fun maxCount(context: Context) = sp(context).getInt(KEY_MAX_COUNT, 8)
    fun maxDurationSec(context: Context) = sp(context).getInt(KEY_MAX_DURATION, 30)
    fun theme(context: Context) = sp(context).getString(KEY_THEME, THEME_LIQUID) ?: THEME_LIQUID
    fun flashOverlay(context: Context) = sp(context).getBoolean(KEY_FLASH, true)
    fun shutterSound(context: Context) = sp(context).getBoolean(KEY_SHUTTER, true)
    fun saveToGallery(context: Context) = sp(context).getBoolean(KEY_GALLERY, true)
}

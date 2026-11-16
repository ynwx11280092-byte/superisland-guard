package com.guard.superisland.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.guard.superisland.R
import com.guard.superisland.notify.Notifier
import com.guard.superisland.service.GuardService
import com.guard.superisland.util.RootStatus
import com.guard.superisland.util.Triggers

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SettingsFragment())
                .commit()
        }
        // 进入界面即拉起守护服务
        GuardService.start(this)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val tv = findViewById<TextView>(R.id.status_text)
        val root = mark(RootStatus.suBinaryPresent())
        val lsp = mark(RootStatus.isLsposedInstalled(this))
        val hook = if (RootStatus.isHookActive(this)) "✓ 已注入" else "… 待触发"
        val island = mark(Notifier.isIslandSupported(this))
        val cam = mark(
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
        tv.text = "Root $root   LSPosed $lsp   Hook $hook\n原生超级岛 $island   相机 $cam"
    }

    private fun mark(ok: Boolean): String = if (ok) "✓" else "✗"

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 1)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference<Preference>("test_capture")?.setOnPreferenceClickListener {
                GuardService.trigger(requireContext(), Triggers.REASON_MANUAL)
                true
            }

            findPreference<Preference>("overlay_permission")?.setOnPreferenceClickListener {
                val i = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivity(i)
                true
            }
        }
    }
}

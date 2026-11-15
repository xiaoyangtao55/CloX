package com.yzmm.clox

import android.app.Application
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.yzmm.clox.crash.CrashHandler
import com.yzmm.clox.qs.privilege.ShizukuOperations
import com.yzmm.clox.util.MonetBackport
import com.google.android.material.color.DynamicColors
import rikka.shizuku.Shizuku

class CloXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        applyDarkModePreference()
        DynamicColors.applyToActivitiesIfAvailable(this)
        MonetBackport.applyToActivitiesIfAvailable(this)
        CrashHandler.install(this)
        initShizuku()
    }

    /**
     * Applies the Appearance setting "dark mode" (system / on / off) before any
     * activity is created. Default is follow-system, matching the DayNight theme.
     */
    private fun applyDarkModePreference() {
        val mode = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("dark_mode", "system")
        AppCompatDelegate.setDefaultNightMode(when (mode) {
            "on"  -> AppCompatDelegate.MODE_NIGHT_YES
            "off" -> AppCompatDelegate.MODE_NIGHT_NO
            else  -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }

    private fun initShizuku() {
        // Bind the Shizuku UserService as soon as Shizuku server is available
        Shizuku.addBinderReceivedListenerSticky {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuOperations.bind()
            }
        }
        Shizuku.addRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuOperations.bind()
            }
        }
        // If already available at startup
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuOperations.bind()
            }
        } catch (_: Exception) {}
    }

    companion object {
        lateinit var instance: CloXApp
            private set

        /** True while any CloX activity is resumed (foreground). */
        @Volatile var foreground: Boolean = false
            private set
    }
}

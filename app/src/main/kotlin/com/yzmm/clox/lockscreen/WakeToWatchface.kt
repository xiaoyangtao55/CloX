package com.yzmm.clox.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.preference.PreferenceManager
import com.yzmm.clox.MainActivity
import com.yzmm.clox.rtos.RtosManager
import com.yzmm.clox.util.safeStartActivity

/**
 * Makes the display wake up on the watch face instead of the system lock screen.
 *
 * The trick is *when* the launcher is raised: as the display turns off, not when it turns on.
 * At that moment the screen is already dark, so the task switch is invisible — and, crucially,
 * no wake lock is involved, so the display stays off and there is no "screen off then on
 * again" flicker (which is what made the old AOD simulation feel wrong).
 *
 * By the time the user presses the button to wake the device, MainActivity is the top activity
 * of the home task and carries `setShowWhenLocked(true)` — so it is drawn over the keyguard
 * instead of the lock screen UI. Swiping up from there asks the system to dismiss the keyguard
 * (see `MainActivity.requestKeyguardUnlock`); CloX never validates credentials itself.
 *
 * Android blocks background activity starts, so this needs the "display over other apps"
 * permission ([SYSTEM_ALERT_WINDOW]); the grant is the only thing the permission is used for —
 * no overlay window is ever created. Without it the receiver does nothing at all, which is why
 * the switch's summary flips to a "grant the permission" prompt.
 */
object WakeToWatchface {

    const val PREF = "wake_to_watchface"

    private const val TAG = "CloX/Wake"

    private var registered = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            val context = ctx?.applicationContext ?: return
            if (!enabled(context)) return
            // Raising the launcher while the face is *not* drawn over the keyguard would swap
            // the unlock landing spot for no visible gain: the user would still see the system
            // lock screen on wake-up. Follow the overlay switch.
            if (!showOverKeyguard(context)) return
            if (RtosManager.isActive) return
            // Starting an activity from the background without this grant would be dropped by
            // the system anyway; skip the attempt instead of logging a stack trace per screen-off.
            if (!hasOverlayPermission(context)) return
            raiseLauncher(context)
        }
    }

    /** The user's choice for this feature, independent of anything it depends on. */
    fun enabled(ctx: Context): Boolean = PreferenceManager
        .getDefaultSharedPreferences(ctx)
        .getBoolean(PREF, true)

    /** Whether the watch face is drawn over the keyguard — this feature is pointless without it. */
    fun showOverKeyguard(ctx: Context): Boolean = PreferenceManager
        .getDefaultSharedPreferences(ctx)
        .getBoolean(MainActivity.PREF_SHOW_OVER_KEYGUARD, true)

    fun hasOverlayPermission(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    /** Registers the screen-off receiver for the lifetime of the process. Idempotent. */
    fun register(ctx: Context) {
        if (registered) return
        registered = runCatching {
            ctx.applicationContext.registerReceiver(
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF)
            )
            true
        }.getOrElse {
            Log.w(TAG, "register failed", it)
            false
        }
    }

    /**
     * Opens the system page where the user can allow "display over other apps". Falls back to
     * the app's details page on ROMs that do not implement the overlay page.
     */
    fun requestOverlayPermission(ctx: Context) {
        val direct = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${ctx.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!ctx.safeStartActivity(direct, showError = false)) {
            ctx.safeStartActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                showError = false
            )
        }
    }

    /**
     * Sends the launcher a HOME intent. MainActivity is `singleTask`, so this either creates it
     * or delivers onNewIntent; either way [MainActivity.EXTRA_CLOSE_OVERLAYS] makes it collapse
     * to the bare watch face (and returns early, so the intent is not mistaken for a HOME press
     * that should toggle the app list).
     */
    private fun raiseLauncher(ctx: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_CLOSE_OVERLAYS, true)
        }
        if (!ctx.safeStartActivity(intent, showError = false)) {
            Log.w(TAG, "could not raise launcher")
        }
    }
}

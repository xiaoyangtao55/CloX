package com.yzmm.clox.qs.privilege

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.yzmm.clox.BuildConfig
import com.yzmm.clox.qs.IQsUserService
import com.yzmm.clox.qs.QsUserService
import rikka.shizuku.Shizuku

object ShizukuOperations {

    private const val TAG = "ShizukuOperations"

    @Volatile private var service: IQsUserService? = null

    private val serviceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, QsUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("qs_user_service")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
    }

    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                service = IQsUserService.Stub.asInterface(binder)
                Log.i(TAG, "QsUserService connected")
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            Log.w(TAG, "QsUserService disconnected")
        }
    }

    fun bind() {
        if (!isAvailable()) return
        try {
            Shizuku.bindUserService(serviceArgs, serviceConn)
        } catch (e: Exception) {
            Log.e(TAG, "bindUserService failed: ${e.message}")
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(serviceArgs, serviceConn, true)
        } catch (_: Exception) {}
        service = null
    }

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun isConnected() = service != null

    // ── Shell helpers ──────────────────────────────────────────────────────────

    /** Ensure service is bound; blocks up to [timeoutMs] ms waiting for connection. */
    private fun ensureConnected(timeoutMs: Long = 3000): Boolean {
        if (isConnected()) return true
        if (!isAvailable()) return false
        bind()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!isConnected() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        return isConnected()
    }

    /** Run command via the UserService; auto-binds if not yet connected. Call on IO thread. */
    private fun exec(command: String): Boolean {
        if (!ensureConnected()) return false
        return try {
            service?.execShell(command) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "exec failed: ${e.message}")
            service = null  // service died; clear so next call rebinds
            false
        }
    }

    // ── Public operations ─────────────────────────────────────────────────────
    //
    // Every parameter that reaches a command string goes through [ShellArgs] first: the
    // UserService runs `Runtime.exec(["sh", "-c", command])`, so an unescaped parameter is
    // shell syntax executed with the Shizuku shell's uid (CWE-78). Arguments with a known
    // alphabet are whitelisted and rejected; free-form values are single-quoted.

    /**
     * Writes one `settings` value. [namespace] and [key] are validated, [value] is quoted —
     * see the note above.
     */
    fun writeSettings(namespace: String, key: String, value: String): Boolean {
        val ns = ShellArgs.namespace(namespace) ?: return false
        val k = ShellArgs.settingsKey(key) ?: return false
        return exec("settings put $ns $k ${ShellArgs.shQuote(value)}")
    }

    /** Official shell path for notification-listener access (API 30+). */
    fun allowNotificationListener(component: String): Boolean {
        val c = ShellArgs.component(component) ?: return false
        return exec("cmd notification allow_listener $c")
    }

    /**
     * Clears Android 13+'s "restricted settings" lock for [pkg] by allowing the
     * ACCESS_RESTRICTED_SETTINGS app-op, so its Accessibility / Notification-listener toggles become
     * grantable on a sideloaded build. Falls back to the numeric op code if the name isn't recognised.
     */
    fun clearRestrictedSettings(pkg: String): Boolean {
        val p = ShellArgs.pkg(pkg) ?: return false
        return exec("appops set $p ACCESS_RESTRICTED_SETTINGS allow") ||
            exec("appops set $p 119 allow")
    }
}

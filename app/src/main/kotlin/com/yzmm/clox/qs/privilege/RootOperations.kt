package com.yzmm.clox.qs.privilege

import com.topjohnwu.superuser.Shell

/**
 * Root shell operations that survive the control-center removal: notification-listener access and
 * lifting Android 13's "restricted settings" lock.
 *
 * Every parameter that ends up in a command string is routed through [ShellArgs] first — libsu
 * hands the string to `sh`, so an unescaped parameter would be **executed as root** (CWE-78).
 */
object RootOperations {

    /**
     * Writes one `settings` value.
     *
     * [namespace] must be system / secure / global and [key] must be a well-formed settings key;
     * both are rejected (returning false) rather than escaped, because a malformed name would not
     * resolve to anything anyway. [value] is free-form — a notification-listener list, for
     * example — so it is quoted as a single shell word.
     */
    fun writeSettings(namespace: String, key: String, value: String): Boolean {
        val ns = ShellArgs.namespace(namespace) ?: return false
        val k = ShellArgs.settingsKey(key) ?: return false
        return Shell.cmd("settings put $ns $k ${ShellArgs.shQuote(value)}").exec().isSuccess
    }

    /** Official shell path for notification-listener access (API 30+). */
    fun allowNotificationListener(component: String): Boolean {
        val c = ShellArgs.component(component) ?: return false
        return Shell.cmd("cmd notification allow_listener $c").exec().isSuccess
    }

    /** Clears Android 13+'s "restricted settings" lock for [pkg] (see ShizukuOperations equivalent). */
    fun clearRestrictedSettings(pkg: String): Boolean {
        val p = ShellArgs.pkg(pkg) ?: return false
        return Shell.cmd("appops set $p ACCESS_RESTRICTED_SETTINGS allow").exec().isSuccess ||
            Shell.cmd("appops set $p 119 allow").exec().isSuccess
    }
}

package com.yzmm.clox.qs.privilege

/**
 * Argument hygiene for everything that reaches the privilege shell.
 *
 * [RootOperations] hands strings to libsu's `Shell.cmd(...)`; [ShizukuOperations] hands them to
 * `QsUserService.execShell`, which runs `Runtime.exec(["sh", "-c", command])`. Either way the
 * string is parsed by a shell, so anything interpolated into it is **syntax, not data**: a `;`,
 * `&&`, `$()` or backtick inside a package name, settings key or value executes as the uid the
 * shell runs under — root, or the Shizuku shell uid. That is a direct privilege escalation
 * (CWE-78), not a cosmetic lint finding.
 *
 * Two layers, applied at every call site:
 *
 *  1. [pkg] / [permission] / [component] / [settingsKey] / [namespace] — arguments with a known
 *     character set are **whitelisted and rejected**, not escaped. An invalid package name means
 *     nothing to `pm` anyway, so failing closed is both safer and easier to reason about.
 *
 *  2. [shQuote] — POSIX single-quote escaping for free-form values that must legitimately reach
 *     the shell (a Settings value may contain spaces, `:` and `/`; see
 *     `enabled_notification_listeners`, which is a `pkg/cls:pkg/cls` list).
 *
 * New shell call sites must go through these helpers — never interpolate a raw parameter into a
 * command string again.
 */
internal object ShellArgs {

    // A package segment starts with a letter, digit or underscore; dots separate segments.
    private val PKG = Regex("^[A-Za-z0-9_][A-Za-z0-9_.]*$")

    // Permission names use the same alphabet (android.permission.READ_HEALTH_DATA).
    private val PERMISSION = Regex("^[A-Za-z0-9_][A-Za-z0-9_.]*$")

    // ComponentName.flattenToString() → "pkg/cls"; inner classes keep their '$'.
    private val COMPONENT = Regex("^[A-Za-z0-9_][A-Za-z0-9_.]*/[A-Za-z0-9_.$][A-Za-z0-9_.$]*$")

    // Settings keys: screen_brightness, airplane_mode_on, do_not_disturb_mode… (never spaces).
    private val SETTINGS_KEY = Regex("^[A-Za-z0-9_][A-Za-z0-9_.-]*$")

    private val NAMESPACES = setOf("system", "secure", "global")

    /** A package name, or null when [value] is not one. */
    fun pkg(value: String): String? = value.takeIf { PKG.matches(it) }

    /** A permission name, or null when [value] is not one. */
    fun permission(value: String): String? = value.takeIf { PERMISSION.matches(it) }

    /** A flattened ComponentName (`pkg/cls`), or null when [value] is not one. */
    fun component(value: String): String? = value.takeIf { COMPONENT.matches(it) }

    /** A `settings` key, or null when [value] is not one. */
    fun settingsKey(value: String): String? = value.takeIf { SETTINGS_KEY.matches(it) }

    /** One of system / secure / global, or null otherwise. */
    fun namespace(value: String): String? = value.takeIf { NAMESPACES.contains(it) }

    /**
     * POSIX single-quote escaping. The result is always exactly one word, whatever the input
     * contains: an embedded `'` becomes `'\''` (close quote, escaped quote, reopen). An empty
     * input yields `''` so the shell still sees an empty argument instead of dropping it.
     */
    fun shQuote(value: String): String =
        if (value.isEmpty()) "''" else "'" + value.replace("'", "'\\''") + "'"
}

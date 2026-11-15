package com.yzmm.clox.gesture

object GestureLockManager {

    enum class LockSource { NONE, MANUAL }

    var lockSource: LockSource = LockSource.NONE
        private set

    val isLocked: Boolean get() = lockSource != LockSource.NONE

    var onLockChanged: ((locked: Boolean, source: LockSource) -> Unit)? = null

    fun lock(source: LockSource) {
        if (lockSource == source) return
        lockSource = source
        onLockChanged?.invoke(true, source)
    }

    fun unlock() {
        if (lockSource == LockSource.NONE) return
        lockSource = LockSource.NONE
        onLockChanged?.invoke(false, LockSource.NONE)
    }

    fun toggleManual() {
        if (lockSource == LockSource.MANUAL) unlock() else lock(LockSource.MANUAL)
    }
}

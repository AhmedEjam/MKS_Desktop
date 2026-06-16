package com.ahmedyejam.mks.data.focus

/**
 * Platform-agnostic Focus Mode manager.
 *
 * Android: Controls Do Not Disturb (DND) settings.
 * Desktop: Typically a no-op or controls system notifications if possible.
 */
interface FocusManager {
    fun hasNotificationPolicyAccess(): Boolean
    fun requestNotificationPolicyAccess()
    fun enableFocusMode(): Boolean
    fun disableFocusMode()
}

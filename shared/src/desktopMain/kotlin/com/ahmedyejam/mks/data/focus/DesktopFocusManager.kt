package com.ahmedyejam.mks.data.focus

class DesktopFocusManager : FocusManager {
    override fun hasNotificationPolicyAccess(): Boolean = true
    override fun requestNotificationPolicyAccess() {}
    override fun enableFocusMode(): Boolean = true
    override fun disableFocusMode() {}
}

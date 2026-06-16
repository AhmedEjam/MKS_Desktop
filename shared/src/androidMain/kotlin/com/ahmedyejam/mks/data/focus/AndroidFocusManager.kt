package com.ahmedyejam.mks.data.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.ahmedyejam.mks.util.MksLogger

class AndroidFocusManager(
    private val context: Context,
) : FocusManager {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var previousFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL

    override fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    override fun requestNotificationPolicyAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun enableFocusMode(): Boolean {
        if (!hasNotificationPolicyAccess()) return false

        try {
            previousFilter = notificationManager.currentInterruptionFilter
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            return true
        } catch (e: Exception) {
            MksLogger.w("FocusManager", "Unable to enable focus mode", e)
            return false
        }
    }

    override fun disableFocusMode() {
        if (!hasNotificationPolicyAccess()) return

        try {
            notificationManager.setInterruptionFilter(previousFilter)
        } catch (e: Exception) {
            MksLogger.w("FocusManager", "Unable to restore focus mode interruption filter", e)
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}

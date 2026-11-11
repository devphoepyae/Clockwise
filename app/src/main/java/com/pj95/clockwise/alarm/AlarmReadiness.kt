package com.pj95.clockwise.alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

/**
 * The platform grants an alarm app depends on, read at one moment.
 *
 * Shown in Settings as status rows rather than requested in a burst on first
 * launch: each one is a trip to a system screen, and the reason for it is much
 * clearer when the user is looking at an alarm that failed to ring.
 */
data class AlarmReadiness(
    /** Android 12/12L "Alarms & reminders"; always true from Android 13. */
    val exactAlarmsAllowed: Boolean = true,
    /** Notifications are enabled for the app. */
    val notificationsAllowed: Boolean = true,
    /** The alarm channel is allowed to interrupt Do Not Disturb. */
    val dndBypassAllowed: Boolean = true,
    /** The app is exempt from battery optimisation. */
    val batteryOptimizationIgnored: Boolean = true,
    /**
     * Android 14+ allows this app to open a full-screen activity from a
     * notification. Always true below 14, where the permission does not exist.
     */
    val fullScreenIntentAllowed: Boolean = true,
)

/** Reads the current state from the platform. Never throws. */
fun readAlarmReadiness(context: Context): AlarmReadiness {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)
    val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)

    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        alarmManager?.canScheduleExactAlarms() == true

    val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()

    val policyAccess = notificationManager?.isNotificationPolicyAccessGranted == true
    val channel = notificationManager?.getNotificationChannel(
        AlarmNotifications.RINGING_CHANNEL_ID,
    )
    val dnd = channel?.canBypassDnd() ?: policyAccess

    val battery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    // Android 14 lets the user switch full-screen alarms off per app. When they
    // have, the ring still happens but lands as a heads-up instead of taking
    // over the lock screen, which is exactly the case worth surfacing.
    val fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        notificationManager?.canUseFullScreenIntent() == true

    return AlarmReadiness(
        exactAlarmsAllowed = exact,
        notificationsAllowed = notifications,
        dndBypassAllowed = dnd,
        batteryOptimizationIgnored = battery,
        fullScreenIntentAllowed = fullScreen,
    )
}

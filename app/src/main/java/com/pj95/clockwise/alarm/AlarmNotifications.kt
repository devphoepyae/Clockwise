package com.pj95.clockwise.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.common.alarm.timeText
import com.pj95.clockwise.model.alarm.Alarm

/**
 * The notification a ringing alarm shows, and the channel it lives on.
 *
 * Three things changed in v4.1.0 and they are worth stating, because the shape
 * of this file follows from them:
 *
 *  - **The service is the ring, not the channel.** [AlarmRingingService] owns a
 *    looping `MediaPlayer` on the alarm stream, so the channel is silent. A
 *    channel's sound is fixed at creation and cannot be muted later, which is
 *    why the ringing channel is a new id rather than a re-used one.
 *  - **Nothing is posted while an alarm is merely armed.** The status-bar alarm
 *    icon comes from `AlarmManager.setAlarmClock`, and that is the only
 *    indication an armed alarm should leave. This notification exists only
 *    between the trigger and Dismiss.
 *  - **The notification is the full-screen intent's carrier.** A foreground
 *    service must show one, and it is also what lets the ringing screen appear
 *    over the keyguard, so the two requirements are served by one object.
 */
object AlarmNotifications {

    /**
     * The ringing channel.
     *
     * Deliberately not `alarms-ringing`: [deleteLegacyChannels] removes every
     * `alarms*` channel left by v4.0.0, and a name that cannot collide is one
     * less way to delete the wrong thing.
     */
    const val RINGING_CHANNEL_ID = "alarm-ringing"

    /** The single channel id v4.0.0 used, and the prefix of its per-sound ones. */
    private const val LEGACY_CHANNEL_ID = "alarms"

    /**
     * The ringing screen's champagne gold.
     *
     * Kept in sync with `AlarmRingingPalette.primary`; the notification is not a
     * Compose surface, so it cannot read the theme's composition local.
     */
    private const val RINGING_ACCENT: Int = 0xFFC9A86A.toInt()

    /**
     * Puts the ringing channel on the device. Safe to call repeatedly.
     *
     * Also cleans up the v4.0.0 channels: they carried the audio, and now that
     * the service does, leaving them behind would show the user settings that no
     * longer control anything.
     */
    fun ensureRingingChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        deleteLegacyChannels(manager)

        val existing = manager.getNotificationChannel(RINGING_CHANNEL_ID)
        if (existing != null) {
            // A channel's sound is fixed, but the app still owns whether its
            // alarms may interrupt Do Not Disturb. Re-creating the same channel
            // updates that one flag and leaves the user's own settings alone.
            if (!existing.canBypassDnd()) {
                existing.setBypassDnd(true)
                manager.createNotificationChannel(existing)
            }
            return
        }

        val channel = NotificationChannel(
            RINGING_CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alarm_channel_description)
            // Silent by design: the service plays the alarm on USAGE_ALARM, and
            // two sources would ring twice for a second each.
            setSound(null, null)
            // The service drives a repeating waveform on the alarm stream, so a
            // channel vibration would only add a second, weaker pattern.
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun deleteLegacyChannels(manager: NotificationManager) {
        manager.notificationChannels
            .map { it.id }
            .filter { it == LEGACY_CHANNEL_ID || it.startsWith("$LEGACY_CHANNEL_ID-") }
            .forEach { legacy -> runCatching { manager.deleteNotificationChannel(legacy) } }
    }

    /**
     * The notification a ringing alarm shows.
     *
     * Two direct actions, Snooze and Stop, so the user can deal with the alarm
     * from the shade without finding the full-screen activity first. Both use
     * the ringing screen's own methods, so the two entry points cannot drift.
     */
    fun ringingNotification(context: Context, alarm: Alarm): Notification {
        val title = alarm.label.ifBlank { context.getString(R.string.alarm_default_label) }
        val use24Hour = DateFormat.is24HourFormat(context)
        val time = alarm.timeText(use24Hour)
        val show = ringingIntent(context, alarm, AlarmRingingActivity.ACTION_SHOW)
        val stop = ringingIntent(context, alarm, AlarmRingingActivity.ACTION_DISMISS)
        val snooze = ringingIntent(context, alarm, AlarmRingingActivity.ACTION_SNOOZE)

        val builder = NotificationCompat.Builder(context, RINGING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(time)
            // The single gold accent from the ringing screen, so the shade and
            // the screen it belongs to read as one moment.
            .setColor(RINGING_ACCENT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            // The service is the sound. Without this the channel and the player
            // would both try to be, and the shade would flash on every loop.
            .setSilent(true)
            // A foreground service's notification is otherwise deferred until
            // the app leaves the foreground; a ringing alarm cannot wait.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(show)
            .setFullScreenIntent(show, true)
            .setDeleteIntent(stop)

        builder.addAction(
            R.drawable.ic_snooze,
            context.getString(R.string.alarm_snooze),
            snooze,
        )
        builder.addAction(
            R.drawable.ic_stop,
            context.getString(R.string.alarm_stop),
            stop,
        )

        return builder.build()
    }

    /** Removes the ringing notification once the alarm is dismissed. */
    fun cancel(context: Context, alarm: Alarm) {
        NotificationManagerCompat.from(context).cancel(notificationId(alarm.id))
    }

    fun cancel(context: Context, alarmId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(alarmId))
    }

    /**
     * One notification per alarm; ids autoincrement from 1.
     *
     * Shared with [AlarmRingingService] so `startForeground` and [cancel] always
     * name the same notification.
     */
    fun notificationId(alarmId: Long): Int = alarmId.toInt()

    /**
     * A Pi for one of the ringing screen's actions.
     *
     * The activity handles every action itself: it is already the thing that
     * rings, and keeping the actions on it means the notification and the
     * on-screen controls run exactly the same code.
     */
    private fun ringingIntent(
        context: Context,
        alarm: Alarm,
        action: String,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode(alarm.id, action),
        Intent(context, AlarmRingingActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, alarm.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Distinct request codes per alarm and per action, so the full-screen
     * intent, Snooze and Stop never collapse into one another.
     */
    private fun requestCode(alarmId: Long, action: String): Int = when (action) {
        AlarmRingingActivity.ACTION_DISMISS -> alarmId.toInt() * 4 + 2
        AlarmRingingActivity.ACTION_SNOOZE -> alarmId.toInt() * 4 + 1
        else -> alarmId.toInt() * 4
    }

    /** True when the platform will actually show notifications. */
    fun notificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** True on Android 13+ until the user grants `POST_NOTIFICATIONS`. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsEnabled(context)
}

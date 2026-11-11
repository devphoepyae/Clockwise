package com.pj95.clockwise.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.model.timer.TimerFormat

/**
 * The two timer notifications: a quiet one while counting, a loud one at zero.
 *
 * The running notification uses the platform's own countdown chronometer
 * (`setUsesChronometer` + `setChronometerCountDown`) rather than a foreground
 * service redrawing every second. The system ticks it, so the notification is
 * accurate while the app is asleep and costs nothing in between.
 */
object TimerNotifications {

    /** Quiet, ongoing, and never makes a sound. */
    const val RUNNING_CHANNEL_ID = "timer-progress"

    /** The ring at zero, on the alarm stream so it is heard in Do Not Disturb. */
    const val ALERT_CHANNEL_ID = "timer-alert"

    private const val RUNNING_NOTIFICATION_ID = 9_001
    private const val FINISHED_NOTIFICATION_ID = 9_002

    /**
     * Creates both channels. Safe to call repeatedly, and called at process
     * start so a receiver that only exists to ring already has them.
     */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        if (manager.getNotificationChannel(RUNNING_CHANNEL_ID) == null) {
            val running = NotificationChannel(
                RUNNING_CHANNEL_ID,
                context.getString(R.string.timer_channel_running_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.timer_channel_running_description)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            manager.createNotificationChannel(running)
        }

        // An alarm channel the user may have tuned in system settings is kept as
        // it is, except for the one property the app owns: whether a timer may
        // interrupt Do Not Disturb. Re-creating the channel with the same id
        // updates that flag without resetting its sound or importance.
        val existing = manager.getNotificationChannel(ALERT_CHANNEL_ID)
        if (existing == null) {
            val alert = NotificationChannel(
                ALERT_CHANNEL_ID,
                context.getString(R.string.timer_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.timer_channel_alert_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 300, 700, 300, 700)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            manager.createNotificationChannel(alert)
        } else if (!existing.canBypassDnd()) {
            existing.setBypassDnd(true)
            manager.createNotificationChannel(existing)
        }
    }

    /** Shows the quiet countdown, or refreshes it after a pause/resume. */
    fun showRunning(context: Context, timer: CountdownTimer, remainingMillis: Long) {
        val endAt = timer.endAtEpochMillis ?: return
        if (!timer.running) return
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val notification = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(timer.label.ifBlank { context.getString(R.string.timer_default_label) })
            .setContentText(TimerFormat.clock(remainingMillis))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(endAt)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(openAppIntent(context))
            .addAction(0, context.getString(R.string.timer_pause), actionIntent(context, TimerReceiver.ACTION_PAUSE))
            .addAction(0, context.getString(R.string.timer_cancel), actionIntent(context, TimerReceiver.ACTION_CANCEL))
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(RUNNING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and the call.
        }
    }

    /**
     * The ring at zero. It carries a full-screen intent so a locked phone shows
     * the timer screen, and a Dismiss action that also clears the stored timer.
     */
    fun showFinished(context: Context, timer: CountdownTimer) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return

        val title = context.getString(R.string.timer_finished_title)
        val body = timer.label.ifBlank {
            context.getString(R.string.timer_finished_body, TimerFormat.summary(timer.durationMillis))
        }

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setContentIntent(ringingIntent(context))
            .setFullScreenIntent(ringingIntent(context), true)
            .addAction(
                0,
                context.getString(R.string.timer_dismiss),
                actionIntent(context, TimerReceiver.ACTION_DISMISS),
            )
            .setDeleteIntent(actionIntent(context, TimerReceiver.ACTION_DISMISS))
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(FINISHED_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and the call.
        }
    }

    fun cancelRunning(context: Context) {
        NotificationManagerCompat.from(context).cancel(RUNNING_NOTIFICATION_ID)
    }

    fun cancelFinished(context: Context) {
        NotificationManagerCompat.from(context).cancel(FINISHED_NOTIFICATION_ID)
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /** Tapping the countdown opens the app at the alarm/timer tab. */
    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_APP_REQUEST,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_TIMER, true),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ringingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        RINGING_REQUEST,
        Intent(context, TimerRingingActivity::class.java)
            .setAction(TimerRingingActivity.ACTION_SHOW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun actionIntent(context: Context, action: String): PendingIntent {
        val request = when (action) {
            TimerReceiver.ACTION_PAUSE -> PAUSE_REQUEST
            TimerReceiver.ACTION_CANCEL -> CANCEL_REQUEST
            else -> DISMISS_REQUEST
        }
        return PendingIntent.getBroadcast(
            context,
            request,
            Intent(context, TimerReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val OPEN_APP_REQUEST = 9_010
    private const val RINGING_REQUEST = 9_011
    private const val PAUSE_REQUEST = 9_012
    private const val CANCEL_REQUEST = 9_013
    private const val DISMISS_REQUEST = 9_014
}

package com.pj95.clockwise.alarm

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.pj95.clockwise.model.alarm.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The sound of a ringing alarm.
 *
 * v4.0.0 rang through the notification channel, which plays once and then
 * stops — and which the platform can silence with a channel setting the app
 * cannot read back. A phone that has to wake someone up cannot depend on that,
 * so the ring moved here:
 *
 *  - **A foreground service**, because the ring has to survive the app being
 *    swiped away, the screen going off and the process being trimmed. The
 *    notification it is required to show is the same one that carries the
 *    full-screen intent, so nothing extra appears in the shade.
 *  - **A looping `MediaPlayer` on `USAGE_ALARM`**, which is the stream the
 *    platform treats as an alarm: audible in Doze, in silent mode and through
 *    Do Not Disturb, exactly like the system clock.
 *  - **`setWakeMode` plus an explicit wake lock**, so the CPU stays awake long
 *    enough to start playing and the display turning off cannot cut the sound.
 *  - **A repeating vibration** on the alarm stream, because a phone face-down on
 *    a bedside table is felt before it is heard.
 *
 * The service stops on Dismiss, or when the ring is stopped from the
 * notification. It also silences itself after five minutes: a phone that rings
 * all night because a bug left the screen up is worse than one that stops a
 * little early, and the ringing screen stays up so the alarm can still be
 * stopped.
 */
class AlarmRingingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    /** Written on the IO thread while preparing, read on the main thread. */
    @Volatile
    private var player: MediaPlayer? = null

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /** True once the five-minute cap has fired; a late `prepare` must not start. */
    @Volatile
    private var silenced = false

    private var ringingAlarmId: Long = NO_ALARM

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, NO_ALARM)
        val notification = IntentCompat.getParcelableExtra(
            intent,
            EXTRA_NOTIFICATION,
            Notification::class.java,
        )
        if (alarmId == NO_ALARM || notification == null) {
            // Nothing to ring and nothing to show: never leave a foreground
            // service behind without its notification.
            stopSelf()
            return START_NOT_STICKY
        }

        if (ringingAlarmId != NO_ALARM) {
            // A repeat delivery, or a second alarm while one is ringing: the
            // newest request wins rather than two players overlapping.
            silenceAudio()
            silenced = false
        }
        ringingAlarmId = alarmId

        startInForeground(alarmId, notification)
        acquireWakeLock()
        startRinging(
            soundUri = intent.getStringExtra(EXTRA_SOUND_URI).orEmpty(),
            vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true),
        )

        // Not sticky: a restarted service would have no notification and no
        // alarm row to read, and a half-ring is worse than a late one.
        return START_NOT_STICKY
    }

    /**
     * Swiping the app away must not silence an alarm, so the service is
     * deliberately left running. `android:stopWithTask="false"` is the manifest
     * half of the same decision.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "Task removed while ringing; keeping the alarm alive")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        silenceAudio()
        releaseWakeLock()
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startInForeground(alarmId: Long, notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            AlarmNotifications.notificationId(alarmId),
            notification,
            type,
        )
    }

    private fun startRinging(soundUri: String, vibrate: Boolean) {
        if (vibrate) startVibration()
        handler.postDelayed(::silenceAudio, MAX_RING_MILLIS)

        scope.launch {
            val prepared = runCatching { createPlayer(soundUri) }
                .onFailure { error -> Log.w(TAG, "Alarm sound could not be prepared", error) }
                .getOrNull()
                ?: return@launch

            if (silenced) {
                prepared.release()
                return@launch
            }

            player = prepared
            runCatching { prepared.start() }
                .onFailure { error -> Log.w(TAG, "Alarm sound could not start", error) }
        }
    }

    /**
     * A player configured for an alarm, not a notification.
     *
     * `USAGE_ALARM` is the whole point: it routes to the alarm stream, which is
     * the one the platform keeps audible in silent mode and through Do Not
     * Disturb. `setWakeMode` holds a partial wake lock for as long as playback
     * lasts, so a screen turning off cannot starve the CPU mid-ring.
     */
    private fun createPlayer(soundUri: String): MediaPlayer {
        val uri = resolveAlarmSoundUri(this, soundUri)
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setWakeMode(this@AlarmRingingService, PowerManager.PARTIAL_WAKE_LOCK)
            setDataSource(this@AlarmRingingService, uri)
            isLooping = true
            prepare()
        }
    }

    // The two-argument `vibrate` is deprecated in API 33 in favour of
    // `VibrationAttributes`, but the deprecated overload is the one that still
    // routes the pattern to the alarm stream on every API from 26 up, and the
    // ringing pattern must not be muted by the phone's notification policy.
    @Suppress("DEPRECATION")
    private fun startVibration() {
        val device = vibratorFor(this) ?: return
        if (!device.hasVibrator()) return
        vibrator = device
        runCatching {
            device.vibrate(
                VibrationEffect.createWaveform(VIBRATION_PATTERN, REPEAT_FROM_START),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build(),
            )
        }.onFailure { error -> Log.w(TAG, "Alarm vibration could not start", error) }
    }

    private fun vibratorFor(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /**
     * Stops the noise, keeps the screen.
     *
     * Called on Dismiss, on the five-minute cap and from `onDestroy`.
     * Idempotent: the ring can end through several of those at once.
     */
    private fun silenceAudio() {
        silenced = true

        val current = player
        player = null
        if (current != null) {
            runCatching { current.stop() }
            runCatching { current.release() }
        }

        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun acquireWakeLock() {
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            runCatching { acquire(WAKE_LOCK_TIMEOUT_MILLIS) }
        }
    }

    private fun releaseWakeLock() {
        val lock = wakeLock
        wakeLock = null
        if (lock?.isHeld == true) runCatching { lock.release() }
    }

    companion object {

        /** Stops the ring without dismissing the alarm (the screen stays up). */
        const val ACTION_STOP: String = "com.pj95.clockwise.action.STOP_ALARM_RING"

        const val EXTRA_ALARM_ID: String = "com.pj95.clockwise.extra.RINGING_ALARM_ID"
        const val EXTRA_SOUND_URI: String = "com.pj95.clockwise.extra.RINGING_SOUND_URI"
        const val EXTRA_VIBRATE: String = "com.pj95.clockwise.extra.RINGING_VIBRATE"
        const val EXTRA_NOTIFICATION: String = "com.pj95.clockwise.extra.RINGING_NOTIFICATION"

        /** Five minutes, then the audio stops on its own. */
        private const val MAX_RING_MILLIS = 5 * 60 * 1000L

        /** A minute of slack over the ring cap, so the lock outlives the audio. */
        private const val WAKE_LOCK_TIMEOUT_MILLIS = MAX_RING_MILLIS + 60_000L

        private const val WAKE_LOCK_TAG = "Chronos:alarmRing"

        /** Pulse, pause, pulse: recognisable without being a car alarm. */
        private val VIBRATION_PATTERN = longArrayOf(0, 600, 400, 600, 400)
        private const val REPEAT_FROM_START = 0

        private const val NO_ALARM = -1L
        private const val TAG = "AlarmRingingService"

        /**
         * Starts the ring for [alarm].
         *
         * The notification is built here rather than in the service so that
         * `startForeground` happens on the first frame of `onStartCommand`: the
         * platform gives a foreground service five seconds, and a database read
         * would spend part of that budget for no reason.
         */
        fun ring(context: Context, alarm: Alarm) {
            val intent = Intent(context, AlarmRingingService::class.java)
                .putExtra(EXTRA_ALARM_ID, alarm.id)
                .putExtra(EXTRA_SOUND_URI, alarm.soundUri)
                .putExtra(EXTRA_VIBRATE, alarm.vibrate)
                .putExtra(
                    EXTRA_NOTIFICATION,
                    AlarmNotifications.ringingNotification(context, alarm),
                )
            ContextCompat.startForegroundService(context, intent)
        }

        /** Asks a running service to stop ringing. Safe when it is not running. */
        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AlarmRingingService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}

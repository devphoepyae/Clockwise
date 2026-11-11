package com.pj95.clockwise.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import com.pj95.clockwise.alarm.readAlarmReadiness
import java.util.Locale
import java.util.TimeZone

/**
 * "Send feedback": a mailto Intent with a short, transparent diagnostic block.
 *
 * The app has no network permission, so nothing is sent by itself. The address,
 * subject and body are handed to whichever mail app the user already uses, and
 * they can read and edit every line before sending. If there is no mail app,
 * [copyDiagnostics] puts the same text on the clipboard instead.
 */
object Feedback {

    /** Where feedback goes. There is no server behind it. */
    const val EMAIL: String = "pj95studio@gmail.com"

    /** Opens a mail app. Returns false when the device has none. */
    fun send(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL"))
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(com.pj95.clockwise.core.ui.R.string.feedback_subject))
            .putExtra(Intent.EXTRA_TEXT, diagnostics(context))

        return try {
            context.startActivity(
                Intent.createChooser(intent, context.getString(com.pj95.clockwise.core.ui.R.string.feedback_chooser))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** Puts the diagnostic block on the clipboard. */
    fun copyDiagnostics(context: Context) {
        val clipboard = context.getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(com.pj95.clockwise.core.ui.R.string.feedback_diagnostics_label),
                diagnostics(context),
            ),
        )
    }

    /**
     * Everything in here is already on the device and visible to the user; none
     * of it identifies them personally.
     */
    fun diagnostics(context: Context): String {
        val readiness = readAlarmReadiness(context)
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "unknown"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "unknown"
        }

        return buildString {
            appendLine("App: ${context.getString(com.pj95.clockwise.core.ui.R.string.app_name)} $versionName ($versionCode)")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Locale: ${Locale.getDefault()}")
            appendLine("Time zone: ${TimeZone.getDefault().id}")
            appendLine("Exact alarms: ${if (readiness.exactAlarmsAllowed) "allowed" else "not allowed"}")
            appendLine("Notifications: ${if (readiness.notificationsAllowed) "on" else "off"}")
            appendLine("Do Not Disturb bypass: ${if (readiness.dndBypassAllowed) "allowed" else "not allowed"}")
            appendLine("Full-screen alarms: ${if (readiness.fullScreenIntentAllowed) "allowed" else "not allowed"}")
            appendLine("Battery optimisation: ${if (readiness.batteryOptimizationIgnored) "exempt" else "optimised"}")
            appendLine()
            appendLine(context.getString(com.pj95.clockwise.core.ui.R.string.feedback_body_placeholder))
        }
    }
}

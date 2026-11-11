package com.pj95.clockwise.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pj95.clockwise.core.ui.R

/**
 * One sound the alarm editor can offer.
 *
 * @param uri the ringtone URI, or `""` for the device's own alarm sound
 * @param title the name the platform gives the sound, e.g. "Bright Morning"
 */
data class AlarmSoundOption(
    val uri: String,
    val title: String,
) {
    /** True for the "device default" entry, which has no URI of its own. */
    val isDefault: Boolean get() = uri.isEmpty()
}

/**
 * The alarm sounds available on this device, and a way to hear one.
 *
 * An interface because the picker is rendered in tests and previews where there
 * is no ringtone store: the editor is given a list and a preview callback
 * rather than reaching for `RingtoneManager` itself, so its layout can be
 * checked without a device.
 */
interface AlarmSoundCatalog {

    /** The default entry first, then the device's alarm tones by name. */
    fun options(): List<AlarmSoundOption>

    /** Plays [uri] once, replacing whatever was playing. */
    fun preview(uri: String)

    /** Stops the preview. Safe to call when nothing is playing. */
    fun stopPreview()
}

/**
 * The real catalog, backed by `RingtoneManager`.
 *
 * Only `TYPE_ALARM` tones are offered: a notification blip cannot wake anyone
 * up, and the point of this list is to choose something that will. A device
 * that returns an empty cursor — some OEM ROMs do — still gets the default
 * entry, so the picker is never blank.
 */
class AndroidAlarmSoundCatalog(private val context: Context) : AlarmSoundCatalog {

    private var ringtone: Ringtone? = null

    override fun options(): List<AlarmSoundOption> {
        val defaultTitle = context.getString(R.string.alarm_sound_default)
        val tones = runCatching { queryAlarmTones() }.getOrDefault(emptyList())
        return buildList {
            add(AlarmSoundOption(uri = "", title = defaultTitle))
            tones.forEach { tone ->
                // The default itself is already the first row.
                if (tone.uri != defaultToneUri()) add(tone)
            }
        }
    }

    override fun preview(uri: String) {
        stopPreview()
        ringtone = runCatching {
            RingtoneManager.getRingtone(context, resolveAlarmSoundUri(context, uri))
        }.getOrNull()?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = false
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
    }

    override fun stopPreview() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    private fun queryAlarmTones(): List<AlarmSoundOption> {
        val manager = RingtoneManager(context).apply {
            setType(RingtoneManager.TYPE_ALARM)
        }
        val cursor = manager.cursor ?: return emptyList()
        return buildList {
            while (cursor.moveToNext()) {
                val uri = manager.getRingtoneUri(cursor.position) ?: continue
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    ?.takeIf { it.isNotBlank() }
                    ?: continue
                add(AlarmSoundOption(uri = uri.toString(), title = title))
            }
        }
    }

    private fun defaultToneUri(): String? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
}

/** The catalog the editor uses at runtime, scoped to the current context. */
@Composable
fun rememberAlarmSoundCatalog(): AlarmSoundCatalog {
    val context = LocalContext.current
    return remember(context) { AndroidAlarmSoundCatalog(context) }
}

/**
 * The URI to ring for a stored [soundUri].
 *
 * Falls back to the device's alarm sound when the stored one is empty or can no
 * longer be resolved — a tone the user deleted since saving, or a URI from a
 * device this account was restored onto. Ringing nothing would be worse than
 * ringing the default.
 */
fun resolveAlarmSoundUri(context: Context, soundUri: String): Uri {
    val stored = soundUri
        .takeIf { it.isNotEmpty() }
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    if (stored != null && runCatching { RingtoneManager.getRingtone(context, stored) }.getOrNull() != null) {
        return stored
    }
    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: Uri.EMPTY
}

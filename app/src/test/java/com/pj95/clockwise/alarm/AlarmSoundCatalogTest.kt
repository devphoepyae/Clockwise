package com.pj95.clockwise.alarm

import android.content.Context
import android.media.RingtoneManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.alarm.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The two rules the alarm sound has to keep: the list is never empty, and a
 * stored sound that cannot be played falls back to the device's own alarm.
 *
 * Runs on Robolectric because both rules are about `RingtoneManager`, which has
 * no JVM implementation.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AlarmSoundCatalogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theListAlwaysStartsWithTheDeviceDefault() {
        // Some OEM ROMs return an empty alarm cursor. The default entry has no
        // URI of its own, so the picker still has something to show and save.
        val options = AndroidAlarmSoundCatalog(context).options()

        assertTrue("the picker is never blank", options.isNotEmpty())
        assertTrue(options.first().isDefault)
        assertEquals("", options.first().uri)
        assertEquals(context.getString(R.string.alarm_sound_default), options.first().title)
    }

    @Test
    fun theDefaultUriIsUsedForAnAlarmWithNoSound() {
        val default = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        assertEquals(default, resolveAlarmSoundUri(context, ""))
    }

    @Test
    fun aSoundThatCannotBePlayedFallsBackToTheDeviceDefault() {
        val missing = "content://com.pj95.clockwise.test/does-not-exist"

        // Ringing nothing would be worse than ringing the default: a tone the
        // user deleted, or one restored from another device, must not silence an
        // alarm.
        assertEquals(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            resolveAlarmSoundUri(context, missing),
        )
    }

    @Test
    fun theRingingChannelIsSilentAndOutsideTheLegacyNamespace() {
        // v4.1.0 moved the ring from the channel to a foreground service, so the
        // ringing channel is silent and the legacy per-sound channels are
        // deleted on upgrade. The new id must therefore not start with the
        // legacy prefix, or cleanup would delete the channel it just created.
        assertFalse(AlarmNotifications.RINGING_CHANNEL_ID.startsWith("alarms"))
        assertTrue(AlarmNotifications.RINGING_CHANNEL_ID.isNotBlank())
    }

    @Test
    fun aSavedAlarmCarriesItsSound() {
        // The domain default is the device sound, so an alarm written before the
        // picker existed needs no migration of its own value.
        assertEquals("", Alarm(hour = 7, minute = 0).soundUri)
        assertEquals(
            "content://tones/bright",
            Alarm(hour = 7, minute = 0, soundUri = "content://tones/bright").soundUri,
        )
    }
}

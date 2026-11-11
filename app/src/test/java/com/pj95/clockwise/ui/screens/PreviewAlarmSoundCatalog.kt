package com.pj95.clockwise.ui.screens

import com.pj95.clockwise.alarm.AlarmSoundCatalog
import com.pj95.clockwise.alarm.AlarmSoundOption

/**
 * A fake catalog for previews and screenshot tests.
 *
 * The real one reads the device's ringtone store, which does not exist off a
 * device, and a preview that played audio would be a preview nobody could run.
 */
class PreviewAlarmSoundCatalog : AlarmSoundCatalog {
    override fun options(): List<AlarmSoundOption> = listOf(
        AlarmSoundOption(uri = "", title = "Default alarm sound"),
        AlarmSoundOption(uri = "preview://bright", title = "Bright Morning"),
        AlarmSoundOption(uri = "preview://chimes", title = "Morning Chimes"),
        AlarmSoundOption(uri = "preview://bell", title = "Old Bell"),
    )

    override fun preview(uri: String) = Unit

    override fun stopPreview() = Unit
}

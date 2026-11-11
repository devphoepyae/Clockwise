package com.pj95.clockwise.domain.time

import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.common.time.ClockText
import com.pj95.clockwise.model.alarm.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

/**
 * The split between a time's digits and its "AM"/"PM".
 *
 * The alarm card, the editor preview and the ringing screen all render the two
 * parts with different sizes, so the boundary has to be right and stable. The
 * concatenated form is asserted too, because notifications and older callers
 * still read one string.
 */
class ClockTextTest {

    @Test
    fun twentyFourHourTimesHaveNoPeriod() {
        val clock = CityTimeFormatter.clockText(LocalTime.of(16, 40), use24Hour = true)

        assertEquals("16", clock.hourText)
        assertEquals("40", clock.minuteText)
        assertNull(clock.periodText)
        assertEquals("16:40", clock.text)
    }

    @Test
    fun twelveHourTimesCarryThePeriod() {
        val morning = CityTimeFormatter.clockText(LocalTime.of(6, 5), use24Hour = false)

        assertEquals("6", morning.hourText)
        assertEquals("05", morning.minuteText)
        assertEquals("AM", morning.periodText)
        assertEquals("6:05 AM", morning.text)
    }

    @Test
    fun midnightAndNoonReadAsTwelveNotZero() {
        val midnight = CityTimeFormatter.clockText(LocalTime.MIDNIGHT, use24Hour = false)
        val noon = CityTimeFormatter.clockText(LocalTime.NOON, use24Hour = false)

        assertEquals("12:00 AM", midnight.text)
        assertEquals("12:00 PM", noon.text)
    }

    @Test
    fun theConcatenatedTextMatchesTheOlderSingleStringApi() {
        // `timeText` is what notifications and the solar rows already use; the
        // split must not change a single character of it.
        for (hour in 0..23) {
            for (minute in intArrayOf(0, 7, 30, 59)) {
                val time = LocalTime.of(hour, minute)
                assertEquals(
                    CityTimeFormatter.timeText(time, use24Hour = false),
                    CityTimeFormatter.clockText(time, use24Hour = false).text,
                )
                assertEquals(
                    CityTimeFormatter.timeText(time, use24Hour = true),
                    CityTimeFormatter.clockText(time, use24Hour = true).text,
                )
            }
        }
    }
}

package com.pj95.clockwise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pj95.clockwise.core.ui.R

/**
 * Inter is the only typeface in the app (spec section 8). The four static
 * weights are bundled under `res/font` so no font is downloaded at runtime.
 */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(
    sizeSp: Int,
    weight: FontWeight,
    lineHeightSp: Int = (sizeSp * 1.3f).toInt(),
    letterSpacingSp: Double = 0.0,
): TextStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = letterSpacingSp.sp,
)

/** Material 3 type scale, fully mapped onto Inter. */
val ClockwiseTypography = Typography(
    displayLarge = inter(57, FontWeight.Bold, lineHeightSp = 64, letterSpacingSp = -0.5),
    displayMedium = inter(45, FontWeight.Bold, lineHeightSp = 52),
    displaySmall = inter(36, FontWeight.Bold, lineHeightSp = 44),
    headlineLarge = inter(32, FontWeight.Bold, lineHeightSp = 40, letterSpacingSp = -0.6),
    headlineMedium = inter(28, FontWeight.Bold, lineHeightSp = 36, letterSpacingSp = -0.4),
    headlineSmall = inter(24, FontWeight.SemiBold, lineHeightSp = 32),
    titleLarge = inter(22, FontWeight.SemiBold, lineHeightSp = 28),
    titleMedium = inter(17, FontWeight.SemiBold, lineHeightSp = 24),
    titleSmall = inter(14, FontWeight.Medium, lineHeightSp = 20),
    bodyLarge = inter(16, FontWeight.Normal, lineHeightSp = 24),
    bodyMedium = inter(14, FontWeight.Normal, lineHeightSp = 20),
    bodySmall = inter(12, FontWeight.Normal, lineHeightSp = 16),
    labelLarge = inter(14, FontWeight.Medium, lineHeightSp = 20),
    labelMedium = inter(12, FontWeight.Medium, lineHeightSp = 16),
    labelSmall = inter(11, FontWeight.Medium, lineHeightSp = 14),
)

/**
 * Named styles for the distinctive pieces of the design. These sit on top of
 * [ClockwiseTypography] so screens can express intent instead of raw sizes.
 */
object ClockwiseTextStyles {
    /** Small uppercase label above a screen title, e.g. "THE WORLD, IN SYNC". */
    val Eyebrow = inter(11, FontWeight.Medium, lineHeightSp = 14, letterSpacingSp = 1.6)

    /** Large screen title, e.g. "World Time". */
    val ScreenTitle = inter(32, FontWeight.Bold, lineHeightSp = 38, letterSpacingSp = -0.6)

    /** Section header, e.g. "YOUR CITIES". */
    val SectionLabel = inter(11, FontWeight.SemiBold, lineHeightSp = 14, letterSpacingSp = 1.4)

    /** Muted count/label next to a section header, e.g. "5 CITIES". */
    val SectionCount = inter(11, FontWeight.Medium, lineHeightSp = 14, letterSpacingSp = 0.8)

    /** UTC offset line on a city card, e.g. "UTC+9". */
    val CityOffset = inter(11, FontWeight.Medium, lineHeightSp = 14, letterSpacingSp = 0.4)

    /** City name on a city card, e.g. "Los Angeles". */
    val CityName = inter(17, FontWeight.SemiBold, lineHeightSp = 22)

    /** Region line on a city card, e.g. "California, USA". */
    val CityRegion = inter(12, FontWeight.Normal, lineHeightSp = 16)

    /** Weekday abbreviation on a city card, e.g. "Thu". */
    val CityWeekday = inter(11, FontWeight.Medium, lineHeightSp = 14, letterSpacingSp = 0.8)

    /**
     * Time difference from the device's own time, under the card's clock, e.g.
     * "+2 hrs 30 mins ahead". Small and quiet: the clock is the card's anchor.
     */
    val CityDifference = inter(10, FontWeight.Medium, lineHeightSp = 13, letterSpacingSp = 0.3)

    /**
     * Live local time on a city card. Tabular figures keep the digits from
     * shifting sideways as the clock ticks.
     */
    val CityTime = inter(28, FontWeight.Bold, lineHeightSp = 32, letterSpacingSp = -0.5)
        .copy(fontFeatureSettings = "tnum")

    /** Small caption under the city list. */
    val DstCaption = inter(10, FontWeight.Medium, lineHeightSp = 15, letterSpacingSp = 1.2)

    /** Label inside the 12h/24h segmented control. */
    val SegmentLabel = inter(12, FontWeight.SemiBold, lineHeightSp = 16, letterSpacingSp = 0.2)

    /** The large live clock on the city detail screen. */
    val DetailClock = inter(64, FontWeight.Bold, lineHeightSp = 68, letterSpacingSp = -2.0)
        .copy(fontFeatureSettings = "tnum")

    /** The "AM"/"PM" suffix beside the large detail clock. */
    val DetailClockPeriod = inter(16, FontWeight.SemiBold, lineHeightSp = 20, letterSpacingSp = 0.5)

    /** Weekday and date under the detail clock, e.g. "Thu, 20 Mar". */
    val DetailDate = inter(16, FontWeight.Medium, lineHeightSp = 22)

    /** City name headline on the detail screen. */
    val DetailCityName = inter(32, FontWeight.Bold, lineHeightSp = 38, letterSpacingSp = -0.6)

    /** Region line under the detail city name. */
    val DetailCityRegion = inter(15, FontWeight.Normal, lineHeightSp = 20)

    /** The user's own label under the detail city name, e.g. "(Head Office)". */
    val DetailCityLabel = inter(15, FontWeight.Medium, lineHeightSp = 20)

    /** The time difference value on the detail screen, e.g. "+2 hrs ahead". */
    val DetailDifference = inter(18, FontWeight.SemiBold, lineHeightSp = 24)

    /** Device timezone line in the "your local time" block. */
    val DetailLocalZone = inter(13, FontWeight.Medium, lineHeightSp = 18)

    /** Device local time value. */
    val DetailLocalTime = inter(28, FontWeight.Bold, lineHeightSp = 32, letterSpacingSp = -0.5)
        .copy(fontFeatureSettings = "tnum")

    /** City name in a search result row. */
    val SearchResultName = inter(15, FontWeight.SemiBold, lineHeightSp = 20)

    /** Region line in a search result row. */
    val SearchResultRegion = inter(12, FontWeight.Normal, lineHeightSp = 16)

    /** Offset and time line in a search result row. */
    val SearchResultMeta = inter(12, FontWeight.Medium, lineHeightSp = 16)
        .copy(fontFeatureSettings = "tnum")

    // ---- World map ----

    /** City name pill in the map screen's horizontal city strip. */
    val MapStripLabel = inter(11, FontWeight.SemiBold, lineHeightSp = 14, letterSpacingSp = 0.6)

    /** Offset and weekday line above the large map clock, e.g. "UTC-8 · Sat". */
    val MapCityOffset = inter(11, FontWeight.Medium, lineHeightSp = 14, letterSpacingSp = 1.0)

    /** The large live clock for the selected city on the map screen. */
    val MapCityTime = inter(46, FontWeight.Bold, lineHeightSp = 50, letterSpacingSp = -1.5)
        .copy(fontFeatureSettings = "tnum")

    /** Selected city name on the map screen. */
    val MapCityName = inter(24, FontWeight.Bold, lineHeightSp = 30, letterSpacingSp = -0.4)

    /** Region line under the selected city name on the map screen. */
    val MapCityRegion = inter(13, FontWeight.Normal, lineHeightSp = 18)

    /** Time difference from the device's own time, under the map city region. */
    val MapCityDifference = inter(12, FontWeight.Medium, lineHeightSp = 16)

    /** Caption under the map explaining the marker and the shading. */
    val MapCaption = inter(10, FontWeight.Medium, lineHeightSp = 15, letterSpacingSp = 1.2)

    /** UTC offset ruler labels drawn inside the map canvas. */
    val MapRulerLabel = inter(9, FontWeight.Medium, lineHeightSp = 11, letterSpacingSp = 0.2)
        .copy(fontFeatureSettings = "tnum")

    // ---- Meeting planner ----

    /** The chosen slot at the top of the planner, read as a clock. */
    val PlannerClock = inter(48, FontWeight.Bold, lineHeightSp = 52, letterSpacingSp = -1.6)
        .copy(fontFeatureSettings = "tnum")

    /** City name in a planner row. */
    val PlannerRowName = inter(17, FontWeight.SemiBold, lineHeightSp = 22)

    /** Source city chip in the planner, e.g. "TOKYO" or "LOCAL". */
    val PlannerSourceChip = inter(11, FontWeight.SemiBold, lineHeightSp = 14, letterSpacingSp = 0.6)

    /** A city's local time in the planner, e.g. "11:30". */
    val PlannerRowTime = inter(20, FontWeight.Bold, lineHeightSp = 24, letterSpacingSp = -0.4)
        .copy(fontFeatureSettings = "tnum")

    /** A city's offset from the device, under its name in the planner. */
    val PlannerRowDifference = inter(11, FontWeight.Medium, lineHeightSp = 15)

    /** Hour labels under the planner's day slider, e.g. "06". */
    val PlannerAxisLabel = inter(9, FontWeight.Medium, lineHeightSp = 11, letterSpacingSp = 0.2)
        .copy(fontFeatureSettings = "tnum")

    /**
     * One number in the alarm time wheels.
     *
     * Tabular figures so a column of 8s and 1s does not shift sideways while it
     * scrolls, and large enough to be read at arm's length on a nightstand.
     */
    val WheelNumber = inter(30, FontWeight.SemiBold, lineHeightSp = 36, letterSpacingSp = -0.5)
        .copy(fontFeatureSettings = "tnum")

    /**
     * The countdown on the timer tab.
     *
     * Larger than a city clock because it is the only thing on that half of the
     * screen, and tabular so the digits do not shuffle every second.
     */
    val TimerClock = inter(56, FontWeight.Bold, lineHeightSp = 62, letterSpacingSp = -2.0)
        .copy(fontFeatureSettings = "tnum")

    /**
     * The time on an alarm card.
     *
     * Smaller than a city clock: an alarm list is scanned for a label and a
     * switch, and the time only has to be unmistakable, not monumental. Tabular
     * so a column of cards lines its digits up.
     */
    val AlarmClock = inter(28, FontWeight.Bold, lineHeightSp = 32, letterSpacingSp = -0.6)
        .copy(fontFeatureSettings = "tnum")

    /** The "AM"/"PM" beside an alarm card's time, set as a span of [AlarmClock]. */
    val AlarmClockPeriod = inter(12, FontWeight.SemiBold, lineHeightSp = 16, letterSpacingSp = 0.6)

    /**
     * The alarm's own label on the ringing screen.
     *
     * The hero of that screen: the user wrote it, so it is what tells them at a
     * glance whether this is the medication alarm or the running one.
     */
    val RingingLabel = inter(40, FontWeight.Bold, lineHeightSp = 46, letterSpacingSp = -0.8)

    /** The time under the label while an alarm rings. */
    val RingingTime = inter(22, FontWeight.SemiBold, lineHeightSp = 28, letterSpacingSp = -0.2)
        .copy(fontFeatureSettings = "tnum")

    /** The "AM"/"PM" beside the ringing time. */
    val RingingTimePeriod = inter(12, FontWeight.SemiBold, lineHeightSp = 16, letterSpacingSp = 0.5)

    /**
     * The desk-clock time. The largest type in the app: it is meant to be read
     * from a bed or a sofa, and the screen has nothing else to show.
     */
    val ScreenSaverClock = inter(88, FontWeight.Bold, lineHeightSp = 96, letterSpacingSp = -3.0)
        .copy(fontFeatureSettings = "tnum")

    /** The date under the desk clock. */
    val ScreenSaverDate = inter(17, FontWeight.Normal, lineHeightSp = 24, letterSpacingSp = 0.2)
}

package com.pj95.clockwise.ui.samples

import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.model.map.GeoPoint
import com.pj95.clockwise.domain.solar.SunriseSunsetCalculator
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.AlarmUiState
import com.pj95.clockwise.ui.MapUiState
import com.pj95.clockwise.ui.PlannerUiState
import com.pj95.clockwise.ui.ClockwiseUiState
import com.pj95.clockwise.ui.mapping.buildMapSolarSummary
import com.pj95.clockwise.ui.mapping.buildMapUiState
import com.pj95.clockwise.ui.mapping.buildPlannerUiState
import com.pj95.clockwise.ui.mapping.buildClockwiseUiState
import java.time.Instant
import java.time.ZoneId

/**
 * Deterministic screen states for previews and screenshot tests.
 *
 * Uses a fixed instant so renders are reproducible, and the real mapper so the
 * sample data is produced by exactly the same code path as the app.
 */
object PreviewSamples {

    /**
     * 20 Jan 2024, 16:40 UTC — a winter instant chosen so the default five
     * cities read exactly like the reference mockups (01:40 / 03:40 / 08:40 /
     * 11:40 / 16:40).
     */
    val referenceInstant: Instant = Instant.parse("2024-01-20T16:40:00Z")

    /**
     * 20 Jul 2024, 04:40 UTC — the opposite season, so the day/night terminator
     * covers the northern hemisphere instead of the southern one.
     */
    val summerInstant: Instant = Instant.parse("2024-07-20T04:40:00Z")

    private val deviceZone: ZoneId = ZoneId.of("Asia/Yangon")

    fun clockState(
        use24Hour: Boolean = true,
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        activeCityId: String? = CityDirectory.defaultActiveCityId,
        now: Instant = referenceInstant,
        cityLabels: Map<String, String> = emptyMap(),
        compareWithLocalTime: Boolean = false,
    ): ClockwiseUiState = buildClockwiseUiState(
        savedCityIds = savedCityIds,
        activeCityId = activeCityId,
        use24Hour = use24Hour,
        now = now,
        deviceZoneId = deviceZone,
        cityLabels = cityLabels,
        compareWithLocalTime = compareWithLocalTime,
    )

    /**
     * The default list with a couple of labels, for label renders.
     *
     * Both labels are short on purpose: a label is the user's own text, so a
     * very long one is allowed to ellipsize rather than push the city name off
     * the card, and the samples should show the everyday case.
     */
    fun labelledState(
        use24Hour: Boolean = true,
        compareWithLocalTime: Boolean = false,
    ): ClockwiseUiState = clockState(
        use24Hour = use24Hour,
        cityLabels = mapOf(
            "tokyo" to "Head Office",
            "new-york" to "HQ",
        ),
        compareWithLocalTime = compareWithLocalTime,
    )

    fun emptyState(use24Hour: Boolean = true): ClockwiseUiState = buildClockwiseUiState(
        savedCityIds = emptyList(),
        activeCityId = null,
        use24Hour = use24Hour,
        now = referenceInstant,
        deviceZoneId = deviceZone,
    )

    /**
     * The map at the reference instant, or at [travelMinute] of the device's
     * current day, with the active city's real solar events so the render shows
     * what the repository will show.
     */
    fun mapState(
        now: Instant = referenceInstant,
        travelMinute: MinuteOfDay? = null,
        use24Hour: Boolean = true,
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        activeCityId: String? = CityDirectory.defaultActiveCityId,
        cityLabels: Map<String, String> = emptyMap(),
    ): MapUiState {
        val clock = clockState(
            use24Hour = use24Hour,
            savedCityIds = savedCityIds,
            activeCityId = activeCityId,
            now = now,
            cityLabels = cityLabels,
        )
        val mapWithoutSolar = buildMapUiState(
            state = clock,
            deviceZoneId = deviceZone,
            travelMinute = travelMinute,
        )
        val solar = mapWithoutSolar.solarCityId
            ?.let(CityDirectory::find)
            ?.let { city ->
                mapWithoutSolar.solarDate?.let { date ->
                    val events = SunriseSunsetCalculator.events(
                        date = date,
                        point = GeoPoint(city.longitude, city.latitude),
                        zone = ZoneId.of(city.timeZone),
                    )
                    buildMapSolarSummary(events, city.id, use24Hour)
                }
            }
        return buildMapUiState(
            state = clock,
            deviceZoneId = deviceZone,
            travelMinute = travelMinute,
            solar = solar,
        )
    }

    /** The map with the sample labels, for the labelled map render. */
    fun labelledMapState(use24Hour: Boolean = true): MapUiState = mapState(
        use24Hour = use24Hour,
        cityLabels = mapOf(
            "tokyo" to "Head Office",
            "new-york" to "HQ",
        ),
    )

    fun emptyMapState(use24Hour: Boolean = true): MapUiState = buildMapUiState(
        state = emptyState(use24Hour),
        deviceZoneId = deviceZone,
    )

    /**
     * The meeting planner for the default list at a chosen slot.
     *
     * @param labels true to include the sample city labels, so the planner's
     *   rows show the same names as the labelled clock renders
     * @param sourceCityId the city whose zone reads the slot, or `null` for the
     *   device's own zone
     */
    fun plannerState(
        minuteOfDay: MinuteOfDay = MinuteOfDay.of(9, 0),
        labels: Boolean = false,
        use24Hour: Boolean = true,
        sourceCityId: String? = null,
    ): PlannerUiState = buildPlannerUiState(
        state = if (labels) labelledState(use24Hour) else clockState(use24Hour = use24Hour),
        minuteOfDay = minuteOfDay,
        deviceZoneId = deviceZone,
        sourceCityId = sourceCityId,
    )

    /** The planner with nothing saved. */
    fun emptyPlannerState(
        minuteOfDay: MinuteOfDay = MinuteOfDay.of(9, 0),
    ): PlannerUiState = buildPlannerUiState(
        state = emptyState(),
        minuteOfDay = minuteOfDay,
        deviceZoneId = deviceZone,
    )

    /** The zone the samples treat as the device's own; Asia/Yangon is UTC+6:30. */
    val sampleDeviceZoneId: ZoneId get() = deviceZone

    /**
     * Three alarms that show every state a card can be in: a repeating weekday
     * alarm, an every-day alarm and a switched-off one-shot.
     */
    fun alarmState(): AlarmUiState = AlarmUiState(
        isLoading = false,
        alarms = listOf(
            Alarm(
                id = 1,
                hour = 6,
                minute = 30,
                label = "Morning run",
                repeatDays = RepeatDays.WEEKDAYS,
            ),
            Alarm(id = 2, hour = 7, minute = 0, label = "Stand-up", repeatDays = RepeatDays.EVERY_DAY),
            Alarm(id = 3, hour = 18, minute = 45, enabled = false, repeatDays = RepeatDays.NONE),
        ),
    )
}

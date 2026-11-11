package com.pj95.clockwise.ui.mapping

import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.model.time.TimeDifference
import com.pj95.clockwise.domain.time.bestWorkingHoursSlot
import com.pj95.clockwise.domain.time.isWithinWorkingDay
import com.pj95.clockwise.ui.PlannerCityRow
import com.pj95.clockwise.ui.PlannerSource
import com.pj95.clockwise.ui.PlannerUiState
import com.pj95.clockwise.ui.ClockwiseUiState
import java.time.LocalTime
import java.time.ZoneId

/**
 * Builds the meeting planner's state for one slot.
 *
 * The slot is a time of day in [sourceCityId]'s zone — the device's own zone
 * when no source is chosen — so "09:00" means nine in the morning there, and
 * every other city is shown at that same instant. Selecting another source is
 * therefore a conversion: the wall-clock reading stays, the instant moves.
 * The source day is that city's current civil date, which keeps a city across
 * the date line on the date its user would call "today".
 *
 * Like [buildClockwiseUiState] this is pure, so the whole planner can be
 * exercised without a device and the screenshots are deterministic.
 *
 * @param minuteOfDay the chosen slot, in the source's timezone
 * @param sourceCityId the saved city to read the slot in, or `null` for the
 *   device; an id that is not saved falls back to the device
 */
fun buildPlannerUiState(
    state: ClockwiseUiState,
    minuteOfDay: MinuteOfDay,
    deviceZoneId: ZoneId,
    sourceCityId: String? = null,
): PlannerUiState {
    // A source is only real if it is both saved and in the directory. Anything
    // else is a stale route argument or a city removed while the planner was
    // open, so the planner quietly returns to the device rather than crashing.
    val sourceCard = sourceCityId?.let { id -> state.cities.firstOrNull { it.id == id } }
    val sourceCity = sourceCard?.let { CityDirectory.find(it.id) }
    val effectiveSourceCityId = if (sourceCity != null) sourceCard.id else null
    val sourceZoneId = sourceCity?.let { ZoneId.of(it.timeZone) } ?: deviceZoneId

    // Today where the slot is expressed. Every row is then read at the instant
    // that falls at this time in the source zone, which is what makes a city on
    // the other side of the date line show a different weekday.
    // LocalDate.ofInstant is API 34; atZone(zone).toLocalDate works on the
    // app's API 26 floor.
    val sourceDate = state.now.atZone(sourceZoneId).toLocalDate()
    val instant = minuteOfDay.onDate(sourceDate, sourceZoneId)

    val reference = CityTimeFormatter.snapshot(
        zoneId = sourceZoneId,
        instant = instant,
        use24Hour = state.use24Hour,
    )

    val cityZones = mutableListOf<ZoneId>()
    val rows = state.cities.mapNotNull { card ->
        val city = CityDirectory.find(card.id) ?: return@mapNotNull null
        val zoneId = ZoneId.of(city.timeZone)
        cityZones += zoneId
        val time = CityTimeFormatter.snapshot(
            zoneId = zoneId,
            instant = instant,
            use24Hour = state.use24Hour,
        )

        PlannerCityRow(
            id = card.id,
            displayName = card.displayName,
            timeText = time.timeText,
            weekdayLabel = time.weekdayShort,
            isWorkingHours = isWithinWorkingDay(instant, zoneId),
            // The difference is measured at the chosen slot rather than "now":
            // a daylight-saving change between the two would otherwise make the
            // row disagree with the clock beside it.
            difference = TimeDifference.between(zoneId, sourceZoneId, instant),
        )
    }

    val sources = if (rows.isEmpty()) {
        emptyList()
    } else {
        buildList {
            add(PlannerSource(cityId = null, name = ""))
            rows.forEach { row -> add(PlannerSource(cityId = row.id, name = row.displayName)) }
        }
    }

    // The recommendation is part of the state, not the screen: the same scan
    // has to hold for a source in a half-hour zone and for cities on opposite
    // sides of the date line, and the answer must be identical wherever it is
    // computed.
    val bestSlot = if (rows.size >= 2) {
        bestWorkingHoursSlot(
            date = sourceDate,
            sourceZoneId = sourceZoneId,
            cityZoneIds = cityZones,
        )
    } else {
        null
    }
    val bestTimeText = bestSlot?.let { overlap ->
        CityTimeFormatter.timeText(
            time = LocalTime.of(overlap.minuteOfDay.hours, overlap.minuteOfDay.minutes),
            use24Hour = state.use24Hour,
        )
    }.orEmpty()

    return PlannerUiState(
        minuteOfDay = minuteOfDay,
        sourceCityId = effectiveSourceCityId,
        sourceName = sourceCard
            ?.takeIf { effectiveSourceCityId != null }
            ?.displayName,
        sources = sources,
        referenceTimeText = reference.timeText,
        referenceDateText = "${reference.weekdayShort}, ${reference.dateText}",
        referenceOffsetLabel = reference.offsetLabel,
        bestSlot = bestSlot,
        bestTimeText = bestTimeText,
        rows = rows,
    )
}

package com.pj95.clockwise.ui

import androidx.compose.runtime.Immutable
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.model.time.TimeDifference
import com.pj95.clockwise.model.time.WorkingHoursOverlap

/**
 * One city's line in the meeting planner: what the clock reads there at the
 * chosen slot, and whether that is a plausible hour to meet.
 */
@Immutable
data class PlannerCityRow(
    val id: String,
    /** Name with the user's own label, e.g. "Tokyo (Head Office)". */
    val displayName: String,
    /** Local time at the chosen slot, e.g. "11:30" or "11:30 AM". */
    val timeText: String,
    /** Weekday there, e.g. "Sun" — the only way to see that a city is a day behind. */
    val weekdayLabel: String,
    /** True when the local time falls inside the working day. */
    val isWorkingHours: Boolean,
    /** The city's offset from the source, at the chosen slot. */
    val difference: TimeDifference,
)

/**
 * A zone the planner's slot can be expressed in.
 *
 * @param cityId the saved city whose zone this is, or `null` for the device
 * @param name the city's name with its label, e.g. "Tokyo (Head Office)";
 *   empty for the device, whose chip is labelled from a string resource
 */
@Immutable
data class PlannerSource(
    val cityId: String?,
    val name: String,
)

/**
 * Everything the meeting planner renders.
 *
 * @param minuteOfDay the chosen slot, in [sourceCityId]'s zone — the device's
 *   own zone when no source city is selected
 * @param sourceCityId the zone the slot is read in, or `null` for the device
 * @param sourceName the selected city's name, or `null` when the source is the
 *   device
 * @param sources every zone the user can convert from; the device is first
 * @param referenceTimeText the slot as a clock reading, e.g. "09:00"
 * @param referenceDateText the slot's date, e.g. "Sun, 21 Jan"
 * @param referenceOffsetLabel the source's UTC offset at the slot, e.g. "UTC+6:30"
 * @param bestSlot the recommended quarter-hour slot, repeated here so the
 *   action can move the slider; `null` when fewer than two cities can meet
 * @param bestTimeText [bestSlot] as a clock reading in the app-wide format,
 *   e.g. "06:30" or "6:30 AM"; empty when there is no recommendation
 */
@Immutable
data class PlannerUiState(
    val minuteOfDay: MinuteOfDay = MinuteOfDay.StartOfDay,
    val sourceCityId: String? = null,
    val sourceName: String? = null,
    val sources: List<PlannerSource> = emptyList(),
    val referenceTimeText: String = "",
    val referenceDateText: String = "",
    val referenceOffsetLabel: String = "",
    val bestSlot: WorkingHoursOverlap? = null,
    val bestTimeText: String = "",
    val rows: List<PlannerCityRow> = emptyList(),
) {
    /** How many cities the recommendation puts inside the working day. */
    val bestWorkingCount: Int get() = bestSlot?.citiesWithinWorkingHours ?: 0
}

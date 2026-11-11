package com.pj95.clockwise.model.time

/**
 * Everything needed to render a city's clock at one instant.
 *
 * @param offsetLabel e.g. "UTC+9", "UTC+5:30"
 * @param timeText full time, e.g. "16:40" or "4:40 PM"
 * @param hourText hour only, used by the large detail clock
 * @param minuteText minute only, always two digits
 * @param periodText "AM"/"PM" in 12-hour mode, `null` in 24-hour mode
 * @param weekdayShort e.g. "Thu"
 * @param dateText day and month, e.g. "20 Mar"
 * @param isDaytime true between 06:00 and 17:59 local time
 */
data class CityTime(
    val offsetLabel: String,
    val timeText: String,
    val hourText: String,
    val minuteText: String,
    val periodText: String?,
    val weekdayShort: String,
    val dateText: String,
    val isDaytime: Boolean,
)

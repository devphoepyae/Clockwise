package com.pj95.clockwise.model.time

/** A recommended meeting slot and how many cities it puts inside the working day. */
data class WorkingHoursOverlap(
    val minuteOfDay: MinuteOfDay,
    val citiesWithinWorkingHours: Int,
)
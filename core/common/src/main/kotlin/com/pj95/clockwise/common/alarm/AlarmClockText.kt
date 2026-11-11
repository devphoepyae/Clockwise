package com.pj95.clockwise.common.alarm

import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.common.time.ClockText
import com.pj95.clockwise.model.alarm.Alarm
import java.time.LocalTime

/**
 * Display helpers for [Alarm].
 *
 * They live in `:core:common` rather than beside the model so that
 * `:core:model` stays a dependency-free data module.
 */

/** "07:30" or "7:30 AM", in the app-wide display format. */
fun Alarm.timeText(use24Hour: Boolean): String = clockText(use24Hour).text

/** The same time split into digits and an optional "AM"/"PM" suffix. */
fun Alarm.clockText(use24Hour: Boolean): ClockText =
    CityTimeFormatter.clockText(LocalTime.of(hour, minute), use24Hour)

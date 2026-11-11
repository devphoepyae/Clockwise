package com.pj95.clockwise.di

import android.content.Context
import com.pj95.clockwise.billing.StudioBilling
import com.pj95.clockwise.data.repository.AlarmRepository
import com.pj95.clockwise.data.repository.CityRepository
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.data.repository.SolarRepository
import com.pj95.clockwise.data.repository.TimerRepository
import com.pj95.clockwise.data.repository.WidgetConfigRepository
import com.pj95.clockwise.data.repository.WidgetLicenseRepository
import com.pj95.clockwise.common.time.SecondTicker
import java.time.Clock

/**
 * Lightweight manual dependency container.
 *
 * The app is intentionally small, so it uses plain constructor injection wired
 * by hand instead of a DI framework. Every instance is process-scoped and
 * created lazily.
 *
 * The contract lives in `:core:data` so feature modules can ask for their
 * dependencies without depending on `:app`; the runtime implementation lives in
 * the composition root, where the database, DataStore and billing client are
 * actually built.
 */
interface AppContainer {
    /** Application context, used for DataStore and other process-scoped needs. */
    val applicationContext: Context

    /** Saved cities, active city and display preference. */
    val clockRepository: ClockRepository

    /** Queryable city catalog backed by Room. */
    val cityRepository: CityRepository

    /** Sunrise/sunset engine with a Room-backed per-day cache. */
    val solarRepository: SolarRepository

    /** The user's alarms, with each write mirrored into `AlarmManager`. */
    val alarmRepository: AlarmRepository

    /** The one countdown timer, with its wake-up and notifications. */
    val timerRepository: TimerRepository

    /** Chronos Studio's local entitlements; the source of truth for licensing. */
    val widgetLicenseRepository: WidgetLicenseRepository

    /** The three-city configuration behind each pinned widget. */
    val widgetConfigRepository: WidgetConfigRepository

    /** Play Billing for the one-time Studio unlock; nothing else uses it. */
    val studioBilling: StudioBilling

    /**
     * The one clock every time-dependent component reads.
     *
     * Production uses [Clock.systemUTC]; tests and future solar/map engines can
     * substitute a fixed clock without reaching for [java.time.Instant.now].
     */
    val clock: Clock

    /** One-second, second-aligned time source. */
    val secondTicker: SecondTicker
}

/**
 * Implemented by the [android.app.Application] so a [Context] can hand out the
 * process-wide container without any module knowing the concrete app class.
 */
interface AppContainerProvider {
    val container: AppContainer
}

/**
 * Returns the process-wide [AppContainer] from any [Context] inside the app.
 */
val Context.appContainer: AppContainer
    get() = (applicationContext as AppContainerProvider).container

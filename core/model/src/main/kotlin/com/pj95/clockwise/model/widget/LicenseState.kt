package com.pj95.clockwise.model.widget

/**
 * The three-day trial rule, as one pure function.
 *
 * A trial is valid from the instant it starts until exactly 72 hours later. A
 * timestamp in the future is treated as inactive rather than as a fresh trial:
 * a user who moves the device clock backwards must not be able to extend the
 * window. This is best-effort offline protection, not a security boundary —
 * without a server there is no way to prove the wall clock was never changed.
 */
object TrialPolicy {

    /** 72 hours, in milliseconds. */
    const val DURATION_MILLIS: Long = 3L * 24L * 60L * 60L * 1_000L

    /** True while [startTimestamp] is within the 72-hour window at [now]. */
    fun isActive(startTimestamp: Long?, now: Long): Boolean {
        if (startTimestamp == null) return false
        val elapsed = now - startTimestamp
        return elapsed >= 0L && elapsed < DURATION_MILLIS
    }

    /** Millis left in the trial, or `0` when it is not active. */
    fun remainingMillis(startTimestamp: Long?, now: Long): Long {
        val start = startTimestamp ?: return 0L
        if (!isActive(start, now)) return 0L
        return DURATION_MILLIS - (now - start)
    }
}

/** What the gallery and a pinned widget should show for one style. */
sealed interface LicenseState {

    /** A free style: always usable. */
    data object Free : LicenseState

    /** A verified purchase unlocked every premium style. */
    data object Unlocked : LicenseState

    /** The 72-hour trial is running. */
    data class TrialActive(val remainingMillis: Long) : LicenseState

    /** The trial was started and has run out. */
    data object TrialExpired : LicenseState

    /** The trial has not been started and the style is not purchased. */
    data object Locked : LicenseState

    val isUsable: Boolean
        get() = this is Free || this is Unlocked || this is TrialActive
}

/**
 * One style's local entitlement.
 *
 * [trialStartTimestamp] is `null` until the user taps "Start 3-day trial"; that
 * distinction is what lets the gallery offer the trial without beginning it on
 * first open.
 */
data class WidgetLicense(
    val style: WidgetStyleId,
    val isUnlocked: Boolean,
    val trialStartTimestamp: Long?,
) {
    /** The state at [now], in wall-clock millis. */
    fun state(now: Long): LicenseState = when {
        !style.isPremium -> LicenseState.Free
        isUnlocked -> LicenseState.Unlocked
        TrialPolicy.isActive(trialStartTimestamp, now) ->
            LicenseState.TrialActive(TrialPolicy.remainingMillis(trialStartTimestamp, now))
        trialStartTimestamp != null -> LicenseState.TrialExpired
        else -> LicenseState.Locked
    }

    /** True when the "Start 3-day trial" action should be offered. */
    val canStartTrial: Boolean
        get() = style.isPremium && !isUnlocked && trialStartTimestamp == null
}

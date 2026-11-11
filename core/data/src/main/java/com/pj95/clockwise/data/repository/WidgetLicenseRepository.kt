package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.WidgetDao
import com.pj95.clockwise.data.local.entity.WidgetLicenseEntity
import com.pj95.clockwise.model.widget.LicenseState
import com.pj95.clockwise.model.widget.WidgetCategory
import com.pj95.clockwise.model.widget.WidgetLicense
import com.pj95.clockwise.model.widget.WidgetStyleId
import com.pj95.clockwise.model.widget.WidgetTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/**
 * The local source of truth for Chronos Studio entitlements.
 *
 * Billing never writes directly to the UI. It calls [unlockAllPremium] after a
 * verified purchase, and the Room flow repaints the gallery and every pinned
 * widget. When Play is unreachable the last known rows remain valid, which is
 * what keeps the app usable with no network.
 */
class WidgetLicenseRepository(
    private val dao: WidgetDao,
    private val clock: Clock,
) {

    /** Every style, with missing rows represented by their catalogue default. */
    val licenses: Flow<List<WidgetLicense>> = dao.observeLicenses().map { rows ->
        val byId = rows.associateBy(WidgetLicenseEntity::widgetId)
        WidgetStyleId.entries.map { style ->
            byId[style.id]?.toDomain() ?: WidgetLicense(
                style = style,
                isUnlocked = !style.isPremium,
                trialStartTimestamp = null,
            )
        }
    }

    /**
     * Writes the catalogue's rows if they are not there yet.
     *
     * `INSERT OR IGNORE` is the whole contract: an existing purchase or a
     * running trial must survive every launch, and a style added in a later
     * release simply gains its default row.
     */
    suspend fun ensureSeeded() {
        dao.insertLicenses(
            WidgetStyleId.entries.map { style ->
                WidgetLicenseEntity(
                    widgetId = style.id,
                    category = style.category.storageValue,
                    isUnlocked = style.tier == WidgetTier.FREE,
                    trialStartTimestamp = null,
                )
            },
        )
    }

    suspend fun get(style: WidgetStyleId): WidgetLicense {
        ensureSeeded()
        val row = dao.getLicense(style.id) ?: return WidgetLicense(
            style = style,
            isUnlocked = !style.isPremium,
            trialStartTimestamp = null,
        )
        return row.toDomain()
    }

    /**
     * Starts [style]'s trial at the current instant.
     *
     * Returns false when the style is free, already purchased, or already had
     * its trial started. The DAO's `IS NULL` guard is the final authority, so a
     * double tap cannot restart the window.
     */
    suspend fun startTrial(style: WidgetStyleId): Boolean {
        ensureSeeded()
        if (!style.isPremium) return false
        val license = get(style)
        if (license.isUnlocked || license.trialStartTimestamp != null) return false
        return dao.startTrial(style.id, clock.millis()) > 0
    }

    /** Records a verified purchase; every premium style becomes usable. */
    suspend fun unlockAllPremium() {
        ensureSeeded()
        dao.unlockAllPremium()
    }

    /** The current state of [style] at the repository's clock. */
    suspend fun state(style: WidgetStyleId): LicenseState = get(style).state(clock.millis())
}

/** The Room row as the domain value. */
fun WidgetLicenseEntity.toDomain(): WidgetLicense {
    val style = WidgetStyleId.fromId(widgetId)
        ?: WidgetStyleId.entries.first()
    return WidgetLicense(
        style = style,
        isUnlocked = isUnlocked || !style.isPremium,
        trialStartTimestamp = trialStartTimestamp,
    )
}

/** Kept for callers that need the stored category without a style lookup. */
fun WidgetLicenseEntity.categoryOrFree(): WidgetCategory =
    WidgetCategory.fromStored(category)

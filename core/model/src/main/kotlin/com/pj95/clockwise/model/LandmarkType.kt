package com.pj95.clockwise.model

/**
 * The landmark silhouette a city detail screen may draw behind its content.
 *
 * The type is deliberately coarse: it selects one hand-drawn vector path, not a
 * photograph or a per-city asset. A city that has no match stores `null` and
 * falls back to the ambient geometric pattern, which is also what a future
 * custom city will do.
 *
 * Values are persisted by [name], never by ordinal, so adding a new landmark in
 * a later release cannot silently change the meaning of an existing row.
 */
enum class LandmarkType {
    PAGODA,
    TOWER,
    SKYSCRAPER,
    CLASSIC_ARCH,
    BRIDGE,
    DOME,
    NONE;

    companion object {
        /** The stored type, or `null` when the value is absent or unknown. */
        fun fromStored(value: String?): LandmarkType? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == value }?.takeUnless { it == NONE }
        }
    }
}

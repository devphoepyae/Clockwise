package com.pj95.clockwise.model

/**
 * A place the user can track.
 *
 * [latitude] and [longitude] are only used to place the city marker on the
 * schematic world map — all timezone maths is driven by [timeZone].
 *
 * @param id stable identifier, persisted in DataStore, safe to rename [name]
 * @param name display name, e.g. "Los Angeles"
 * @param region region / state / country label shown under the name,
 *   e.g. "California, USA"
 * @param country country label used for search, e.g. "United States"
 * @param timeZone IANA timezone id, e.g. "America/Los_Angeles"
 * @param landmarkType optional vector landmark drawn behind the detail screen;
 *   `null` means the ambient fallback pattern
 */
data class City(
    val id: String,
    val name: String,
    val region: String,
    val country: String,
    val timeZone: String,
    val latitude: Double,
    val longitude: Double,
    val landmarkType: LandmarkType? = null,
)

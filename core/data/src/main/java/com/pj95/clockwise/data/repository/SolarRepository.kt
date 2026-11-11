package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.SolarDao
import com.pj95.clockwise.data.local.entity.toEntity
import com.pj95.clockwise.data.local.entity.toSolarEvents
import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.map.GeoPoint
import com.pj95.clockwise.model.solar.SolarEvents
import com.pj95.clockwise.domain.solar.SunriseSunsetCalculator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sunrise and sunset for a city, with a small Room-backed cache.
 *
 * The solar equations are cheap enough to run on demand, but the result for a
 * city and a date never changes, so it is cached and reused by the map, the
 * city detail screen and the widget. The cache is keyed by city and local date;
 * a cached row whose coordinates no longer match the city is recomputed, so a
 * coordinate correction in a future release cannot leave a stale answer.
 *
 * Cache failures are best-effort: if Room is unavailable, the freshly computed
 * result is still returned. A cache is an optimisation, never a dependency.
 */
class SolarRepository(
    private val dao: SolarDao,
    private val clock: Clock = Clock.systemUTC(),
    private val calculator: (LocalDate, GeoPoint, ZoneId) -> SolarEvents =
        SunriseSunsetCalculator::events,
) {

    /** Events for [city] on [date] in the city's own zone. */
    suspend fun events(city: City, date: LocalDate): SolarEvents =
        events(
            cityId = city.id,
            latitude = city.latitude,
            longitude = city.longitude,
            date = date,
            zone = ZoneId.of(city.timeZone),
        )

    /** Events for the local date containing [instant] in the city's zone. */
    suspend fun events(city: City, instant: Instant): SolarEvents {
        val zone = ZoneId.of(city.timeZone)
        return events(city = city, date = instant.atZone(zone).toLocalDate())
    }

    /** Events for an explicit location and zone; the cache key is [cityId]. */
    suspend fun events(
        cityId: String,
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId,
    ): SolarEvents {
        val cached = runCatching { dao.find(cityId, date.toEpochDay()) }.getOrNull()
        val cachedEvents = cached
            ?.takeIf { it.matchesCoordinates(latitude, longitude) }
            ?.let { row -> runCatching { row.toSolarEvents(zone) }.getOrNull() }
        if (cachedEvents != null) return cachedEvents

        val computed = calculator(date, GeoPoint(longitude, latitude), zone)
        runCatching {
            dao.upsert(
                computed.toEntity(
                    cityId = cityId,
                    latitude = latitude,
                    longitude = longitude,
                    calculatedAt = clock.instant(),
                ),
            )
        }
        return computed
    }

    /** Drops cached days before [date]; keeps the table bounded over time. */
    suspend fun pruneBefore(date: LocalDate): Int = dao.deleteOlderThan(date.toEpochDay())

    /** Empties the cache. */
    suspend fun clear(): Int = dao.clear()
}

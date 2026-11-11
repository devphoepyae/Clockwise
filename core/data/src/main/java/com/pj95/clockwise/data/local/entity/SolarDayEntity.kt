package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.solar.SolarEvents
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One cached solar day for one city.
 *
 * Sunrise and sunset are pure functions of date and coordinates, so the cache
 * is keyed by the city and its local date. The coordinates are stored as well:
 * if a future release corrects a city's position, the stale row is detected
 * and recomputed instead of being served forever.
 *
 * A normal day stores both instants. A polar day or night stores neither, but
 * still stores solar noon and the day length.
 */
@Entity(
    tableName = "solar_days",
    primaryKeys = ["cityId", "dateEpochDay"],
    indices = [Index("dateEpochDay")],
)
data class SolarDayEntity(
    val cityId: String,
    val dateEpochDay: Long,
    val latitude: Double,
    val longitude: Double,
    val kind: String,
    val sunriseEpochMilli: Long?,
    val sunsetEpochMilli: Long?,
    val solarNoonEpochMilli: Long,
    val dayLengthSeconds: Long,
    val calculatedAtEpochMilli: Long,
) {

    /** True when this row still describes [latitude] and [longitude]. */
    fun matchesCoordinates(latitude: Double, longitude: Double): Boolean =
        this.latitude == latitude && this.longitude == longitude
}

/** Maps a calculated result into its cache row. */
fun SolarEvents.toEntity(
    cityId: String,
    latitude: Double,
    longitude: Double,
    calculatedAt: Instant,
): SolarDayEntity = SolarDayEntity(
    cityId = cityId,
    dateEpochDay = date.toEpochDay(),
    latitude = latitude,
    longitude = longitude,
    kind = kind.name,
    sunriseEpochMilli = sunrise?.toEpochMilli(),
    sunsetEpochMilli = sunset?.toEpochMilli(),
    solarNoonEpochMilli = solarNoon.toEpochMilli(),
    dayLengthSeconds = dayLength.seconds,
    calculatedAtEpochMilli = calculatedAt.toEpochMilli(),
)

/**
 * Rebuilds the domain object. [zone] is supplied by the caller because the
 * zone is a display concern and is already part of the city definition.
 */
fun SolarDayEntity.toSolarEvents(zone: ZoneId): SolarEvents {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val sunrise = sunriseEpochMilli?.let(Instant::ofEpochMilli)
    val sunset = sunsetEpochMilli?.let(Instant::ofEpochMilli)
    return SolarEvents(
        date = date,
        zone = zone,
        kind = SolarDayKind.valueOf(kind),
        sunrise = sunrise,
        sunset = sunset,
        solarNoon = Instant.ofEpochMilli(solarNoonEpochMilli),
        dayLength = Duration.ofSeconds(dayLengthSeconds),
    )
}

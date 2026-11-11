package com.pj95.clockwise.domain.map

import com.pj95.clockwise.model.map.GeoPoint
import kotlin.math.cos
import kotlin.math.sin

/** Which of the two bundled map views is showing. */
enum class MapProjectionMode {
    /** The flat, whole-world equirectangular plate. */
    FLAT,

    /** A rotatable orthographic globe centred on the active city. */
    GLOBE,
}

/**
 * One point after orthographic projection.
 *
 * Fractions are relative to the globe's bounding square: `(0, 0)` is the
 * top-left corner of that square and `(0.5, 0.5)` is the centre of the disc.
 * [visible] is false when the point is on the far side of the globe.
 */
data class GlobePoint(
    val xFraction: Float,
    val yFraction: Float,
    val visible: Boolean,
)

/**
 * Orthographic ("as seen from space") projection of a sphere.
 *
 * The globe is centred on a chosen longitude/latitude, so the active city stays
 * at the middle of the disc while the rest of the world rotates around it.
 * Pure Kotlin and free of Compose types, which keeps the geometry testable on
 * the JVM and lets the same projection place both the drawn continents and the
 * tappable city markers.
 */
object GlobeProjection {

    /**
     * Projects [longitude]/[latitude] onto a globe centred at
     * [centerLongitude]/[centerLatitude].
     */
    fun project(
        longitude: Double,
        latitude: Double,
        centerLongitude: Double,
        centerLatitude: Double,
    ): GlobePoint {
        val relativeLongitude = Math.toRadians(SolarPosition.normalizeLongitude(longitude - centerLongitude))
        val lat = Math.toRadians(latitude.coerceIn(-90.0, 90.0))
        val centerLat = Math.toRadians(centerLatitude.coerceIn(-90.0, 90.0))

        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val cosCenter = cos(centerLat)
        val sinCenter = sin(centerLat)
        val cosLon = cos(relativeLongitude)

        val x = cosLat * sin(relativeLongitude)
        val y = cosCenter * sinLat - sinCenter * cosLat * cosLon
        val z = sinCenter * sinLat + cosCenter * cosLat * cosLon

        return GlobePoint(
            xFraction = (0.5 + x / 2.0).toFloat(),
            yFraction = (0.5 - y / 2.0).toFloat(),
            visible = z >= 0.0,
        )
    }
}

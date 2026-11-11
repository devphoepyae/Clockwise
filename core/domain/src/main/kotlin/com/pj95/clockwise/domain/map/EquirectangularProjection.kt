package com.pj95.clockwise.domain.map

import com.pj95.clockwise.model.map.GeoPoint
/**
 * Equirectangular ("plate carrée") projection.
 *
 * Every meridian comes out vertical and every parallel horizontal, which is what
 * makes the latitude/longitude grid and the solar terminator cheap to draw.
 * Results are fractions in `0..1` so callers can scale them to any canvas size.
 *
 * This is a schematic projection drawn entirely on device: no map tiles, no map
 * provider and no API key.
 */
object EquirectangularProjection {

    const val MIN_LONGITUDE: Double = -180.0
    const val MAX_LONGITUDE: Double = 180.0
    const val MIN_LATITUDE: Double = -90.0
    const val MAX_LATITUDE: Double = 90.0

    /** Fraction across the map: `0` at 180°W, `1` at 180°E. */
    fun xFraction(longitude: Double): Double = (longitude + 180.0) / 360.0

    /** Fraction down the map: `0` at 90°N, `1` at 90°S. */
    fun yFraction(latitude: Double): Double = (90.0 - latitude) / 180.0

    /** Inverse of [xFraction]. */
    fun longitudeAt(xFraction: Double): Double = xFraction * 360.0 - 180.0

    /** Inverse of [yFraction]. */
    fun latitudeAt(yFraction: Double): Double = 90.0 - yFraction * 180.0
}

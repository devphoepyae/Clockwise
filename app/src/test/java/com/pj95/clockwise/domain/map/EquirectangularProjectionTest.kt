package com.pj95.clockwise.domain.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The projection is the contract between city coordinates and map pixels, so it
 * is pinned at known reference points (spec section 4.4).
 */
class EquirectangularProjectionTest {

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) {
        assertEquals(expected, actual, tolerance)
    }

    @Test
    fun xFraction_spansTheGlobeFromWestToEast() {
        assertClose(0.0, EquirectangularProjection.xFraction(-180.0))
        assertClose(0.5, EquirectangularProjection.xFraction(0.0))
        assertClose(1.0, EquirectangularProjection.xFraction(180.0))
    }

    @Test
    fun yFraction_spansTheGlobeFromNorthToSouth() {
        assertClose(0.0, EquirectangularProjection.yFraction(90.0))
        assertClose(0.5, EquirectangularProjection.yFraction(0.0))
        assertClose(1.0, EquirectangularProjection.yFraction(-90.0))
    }

    @Test
    fun knownCities_landOnTheirExpectedSideOfTheMap() {
        // Los Angeles: 118.2°W, 34.1°N -> left quarter, upper-middle.
        val laX = EquirectangularProjection.xFraction(-118.2)
        val laY = EquirectangularProjection.yFraction(34.1)
        assertTrue("Los Angeles is west of the prime meridian", laX < 0.5)
        assertTrue("Los Angeles is north of the equator", laY < 0.5)

        // Tokyo: 139.7°E, 35.7°N -> right quarter, upper-middle.
        val tokyoX = EquirectangularProjection.xFraction(139.7)
        assertTrue("Tokyo is east of Los Angeles", tokyoX > laX)
        assertTrue("Tokyo is well east of the prime meridian", tokyoX > 0.8)

        // Sydney: 151.2°E, 33.9°S -> right quarter, lower-middle.
        val sydneyY = EquirectangularProjection.yFraction(-33.9)
        assertTrue("Sydney is south of the equator", sydneyY > 0.5)
        assertTrue("Sydney is south of Los Angeles", sydneyY > laY)
    }

    @Test
    fun fractions_areInverseOfEachOther() {
        listOf(-180.0, -97.5, 0.0, 42.25, 180.0).forEach { longitude ->
            assertClose(
                longitude,
                EquirectangularProjection.longitudeAt(EquirectangularProjection.xFraction(longitude)),
            )
        }
        listOf(-90.0, -33.9, 0.0, 34.1, 90.0).forEach { latitude ->
            assertClose(
                latitude,
                EquirectangularProjection.latitudeAt(EquirectangularProjection.yFraction(latitude)),
            )
        }
    }

    @Test
    fun xFraction_isMonotonicSoMarkersNeverCrossOver() {
        var previous = Double.NEGATIVE_INFINITY
        var longitude = -180.0
        while (longitude <= 180.0) {
            val x = EquirectangularProjection.xFraction(longitude)
            assertTrue("xFraction must increase with longitude", x > previous)
            previous = x
            longitude += 7.5
        }
    }
}

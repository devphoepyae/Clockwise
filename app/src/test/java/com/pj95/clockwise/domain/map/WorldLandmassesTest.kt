package com.pj95.clockwise.domain.map

import com.pj95.clockwise.model.map.GeoPoint
import com.pj95.clockwise.model.CityDirectory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the hand-authored landmass vectors used by the schematic map.
 *
 * The coordinates are typed by hand, so these tests catch the two failure modes
 * that would actually show on screen: a coordinate outside the globe, and an
 * outline that has drifted so far it no longer covers its own continent.
 */
class WorldLandmassesTest {

    @Test
    fun everyOutlineIsAClosedRingOfAtLeastThreePoints() {
        assertTrue("Expected a recognisable set of landmasses", WorldLandmasses.polygons.size >= 20)
        WorldLandmasses.polygons.forEachIndexed { index, outline ->
            assertTrue(
                "Outline $index needs at least three points, had ${outline.size}",
                outline.size >= 3,
            )
        }
    }

    @Test
    fun everyCoordinateIsOnTheGlobe() {
        WorldLandmasses.polygons.forEachIndexed { index, outline ->
            outline.forEach { point ->
                assertTrue(
                    "Outline $index has longitude off the globe: ${point.longitude}",
                    point.longitude >= -180.0 && point.longitude <= 180.0,
                )
                assertTrue(
                    "Outline $index has latitude off the globe: ${point.latitude}",
                    point.latitude >= -90.0 && point.latitude <= 90.0,
                )
            }
        }
    }

    @Test
    fun inlandReferencePointsFallInsideALandmass() {
        val inland = mapOf(
            "Kansas, USA" to GeoPoint(-98.0, 39.0),
            "Amazon basin, Brazil" to GeoPoint(-60.0, -10.0),
            "Kalahari, South Africa" to GeoPoint(24.0, -25.0),
            "Central Asia" to GeoPoint(75.0, 50.0),
            "Siberia" to GeoPoint(110.0, 65.0),
            "Central Australia" to GeoPoint(133.0, -25.0),
            "Greenland ice sheet" to GeoPoint(-42.0, 74.0),
            "East Africa" to GeoPoint(35.0, -3.0),
        )
        inland.forEach { (label, point) ->
            assertTrue("$label should be on land", isOnLand(point))
        }
    }

    @Test
    fun openOceanReferencePointsFallOutsideEveryLandmass() {
        val ocean = mapOf(
            "Mid-Atlantic" to GeoPoint(-30.0, 30.0),
            "North Pacific" to GeoPoint(-140.0, 0.0),
            "Indian Ocean" to GeoPoint(80.0, -40.0),
            "South Pacific" to GeoPoint(-120.0, -30.0),
            "Southern Ocean" to GeoPoint(0.0, -55.0),
        )
        ocean.forEach { (label, point) ->
            assertFalse("$label should be water", isOnLand(point))
        }
    }

    @Test
    fun everyDirectoryCityHasCoordinatesInsideTheMap() {
        CityDirectory.search("").forEach { city ->
            assertTrue(
                "${city.name} latitude ${city.latitude} is off the globe",
                city.latitude in -90.0..90.0,
            )
            assertTrue(
                "${city.name} longitude ${city.longitude} is off the globe",
                city.longitude in -180.0..180.0,
            )
            val x = EquirectangularProjection.xFraction(city.longitude)
            val y = EquirectangularProjection.yFraction(city.latitude)
            assertTrue("${city.name} projects outside the map horizontally", x in 0.0..1.0)
            assertTrue("${city.name} projects outside the map vertically", y in 0.0..1.0)
        }
    }

    /**
     * Standard even-odd ray cast. No outline crosses the antimeridian, so the
     * polygons can be tested directly in longitude/latitude space.
     */
    private fun isOnLand(point: GeoPoint): Boolean =
        WorldLandmasses.polygons.any { contains(it, point) }

    private fun contains(outline: List<GeoPoint>, point: GeoPoint): Boolean {
        var inside = false
        var previous = outline.last()
        outline.forEach { current ->
            val straddles = (current.latitude > point.latitude) != (previous.latitude > point.latitude)
            if (straddles) {
                val t = (point.latitude - current.latitude) / (previous.latitude - current.latitude)
                val crossingLongitude = current.longitude + t * (previous.longitude - current.longitude)
                if (point.longitude < crossingLongitude) inside = !inside
            }
            previous = current
        }
        return inside
    }
}

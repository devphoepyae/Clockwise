package com.pj95.clockwise.domain.model

import com.pj95.clockwise.model.CityDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Guards the bundled city directory: ids, timezones and search matching. */
class CityDirectoryTest {

    @Test
    fun containsTwentyTwoCitiesWithUniqueStableIds() {
        assertEquals(22, CityDirectory.all.size)
        assertEquals(CityDirectory.all.size, CityDirectory.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyTimezoneIsAValidIanaId() {
        CityDirectory.all.forEach { city ->
            // Throws for unknown ids, which fails the test.
            ZoneId.of(city.timeZone)
        }
    }

    @Test
    fun coordinatesAreWithinValidRanges() {
        CityDirectory.all.forEach { city ->
            assertTrue("${city.id} latitude", city.latitude in -90.0..90.0)
            assertTrue("${city.id} longitude", city.longitude in -180.0..180.0)
        }
    }

    @Test
    fun defaultSavedCitiesAllExistInOrder() {
        assertEquals(
            listOf("tokyo", "sydney", "los-angeles", "new-york", "london"),
            CityDirectory.citiesForIds(CityDirectory.defaultSavedCityIds).map { it.id },
        )
        assertEquals("los-angeles", CityDirectory.defaultActiveCityId)
    }

    @Test
    fun blankQueryReturnsEveryCity() {
        assertEquals(22, CityDirectory.search("").size)
        assertEquals(22, CityDirectory.search("   ").size)
    }

    @Test
    fun matchesByNameIgnoringCase() {
        assertEquals(listOf("tokyo"), CityDirectory.search("tokyo").map { it.id })
        assertEquals(listOf("london"), CityDirectory.search("LONDON").map { it.id })
        assertEquals(listOf("hong-kong"), CityDirectory.search("hong").map { it.id })
    }

    @Test
    fun matchesByCountryLabel() {
        val japan = CityDirectory.search("Japan").map { it.id }
        assertEquals(listOf("tokyo"), japan)
    }

    @Test
    fun matchesByIanaTimezoneName() {
        assertEquals(listOf("yangon"), CityDirectory.search("Asia/Yangon").map { it.id })
        assertEquals(listOf("mumbai"), CityDirectory.search("kolkata").map { it.id })
    }

    @Test
    fun matchesByRegion() {
        assertEquals(listOf("sydney"), CityDirectory.search("New South Wales").map { it.id })
        assertEquals(listOf("los-angeles"), CityDirectory.search("California").map { it.id })
    }

    @Test
    fun unknownQueryReturnsNothing() {
        assertTrue(CityDirectory.search("atlantis").isEmpty())
    }

    @Test
    fun citiesForIdsDropsUnknownIdsAndKeepsOrder() {
        assertEquals(
            listOf("tokyo", "london"),
            CityDirectory.citiesForIds(listOf("tokyo", "atlantis", "london")).map { it.id },
        )
    }
}

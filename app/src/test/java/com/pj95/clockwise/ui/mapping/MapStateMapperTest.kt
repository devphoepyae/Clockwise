package com.pj95.clockwise.ui.mapping

import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.solar.SolarEvents
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.MapUiState
import com.pj95.clockwise.ui.samples.PreviewSamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The map mapper: live time, time travel, and the solar summary's date rule.
 *
 * The reference instant is 20 Jan 2024, 16:40 UTC. The sample device sits in
 * `Asia/Yangon` (UTC+6:30), where that is 23:10 on Saturday 20 January; the
 * active city, Los Angeles, is at 08:40 on the same date.
 */
class MapStateMapperTest {

    private val deviceZone: ZoneId = ZoneId.of("Asia/Yangon")

    private fun map(
        travelMinute: MinuteOfDay? = null,
        use24Hour: Boolean = true,
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        activeCityId: String? = CityDirectory.defaultActiveCityId,
    ): MapUiState = buildMapUiState(
        state = PreviewSamples.clockState(
            use24Hour = use24Hour,
            savedCityIds = savedCityIds,
            activeCityId = activeCityId,
        ),
        deviceZoneId = deviceZone,
        travelMinute = travelMinute,
    )

    private fun city(map: MapUiState, id: String) = map.cities.single { it.id == id }

    private fun events(
        date: LocalDate,
        zone: ZoneId,
        sunrise: Instant? = Instant.parse("2024-01-20T14:30:00Z"),
        sunset: Instant? = Instant.parse("2024-01-21T01:05:00Z"),
        kind: SolarDayKind = SolarDayKind.NORMAL,
        dayLength: Duration = Duration.ofHours(10),
    ): SolarEvents = SolarEvents(
        date = date,
        zone = zone,
        kind = kind,
        sunrise = sunrise,
        sunset = sunset,
        solarNoon = Instant.parse("2024-01-20T20:00:00Z"),
        dayLength = dayLength,
    )

    // ------------------------------------------------------------- live time

    @Test
    fun liveTimeFollowsTheClockAndReadsTheDeviceZone() {
        val map = map()

        assertTrue(map.isLive)
        assertEquals(PreviewSamples.referenceInstant, map.instant)
        assertEquals(MinuteOfDay.of(23, 10), map.minuteOfDay)
        assertEquals("23:10", map.deviceTimeText)
        assertEquals("Sat, 20 Jan", map.deviceDateText)
        assertEquals("UTC+6:30", map.deviceOffsetLabel)
        assertEquals("los-angeles", map.activeCity?.id)
    }

    @Test
    fun everyCityIsReadAtTheSameInstantAsBefore() {
        val map = map()

        assertEquals("08:40", city(map, "los-angeles").timeText)
        assertEquals("01:40", city(map, "tokyo").timeText)
        assertEquals("Sat", city(map, "los-angeles").weekdayLabel)
    }

    @Test
    fun labelsAreCarriedThroughTheMapper() {
        val labelled = buildMapUiState(
            state = PreviewSamples.clockState(
                cityLabels = mapOf("tokyo" to "Head Office"),
            ),
            deviceZoneId = deviceZone,
        )

        assertEquals("Tokyo (Head Office)", city(labelled, "tokyo").displayName)
        assertEquals("Head Office", city(labelled, "tokyo").label)
    }

    // ------------------------------------------------------------ time travel

    @Test
    fun travellingToThreeInTheMorningMovesEveryCityWithTheSun() {
        val map = map(travelMinute = MinuteOfDay.of(3, 0))

        assertEquals(false, map.isLive)
        // 03:00 on 20 Jan in UTC+6:30 is still the previous evening in UTC.
        assertEquals(Instant.parse("2024-01-19T20:30:00Z"), map.instant)
        assertEquals(MinuteOfDay.of(3, 0), map.minuteOfDay)
        assertEquals("03:00", map.deviceTimeText)
        assertEquals("Sat, 20 Jan", map.deviceDateText)

        // Los Angeles has not reached midnight of the device's date yet.
        assertEquals("12:30", city(map, "los-angeles").timeText)
        assertEquals("Fri", city(map, "los-angeles").weekdayLabel)
        assertEquals("05:30", city(map, "tokyo").timeText)
    }

    @Test
    fun travellingToTheLastMinuteOfTheDayDoesNotRollOver() {
        val map = map(travelMinute = MinuteOfDay(MinuteOfDay.MAX_VALUE))

        assertEquals(false, map.isLive)
        assertEquals(MinuteOfDay(MinuteOfDay.MAX_VALUE), map.minuteOfDay)
        assertEquals("23:59", map.deviceTimeText)
    }

    @Test
    fun theMapCanTravelAcrossTheDateLineForTheSolarDate() {
        // 23:59 in Yangon is already 02:29 the next day in Tokyo, so the solar
        // events must be requested for 21 January, not the device's 20th.
        val map = map(
            travelMinute = MinuteOfDay(MinuteOfDay.MAX_VALUE),
            activeCityId = "tokyo",
        )

        assertEquals("tokyo", map.activeCity?.id)
        assertEquals(LocalDate.of(2024, 1, 21), map.solarDate)
        assertEquals("tokyo", map.solarCityId)
    }

    @Test
    fun theSolarDateIsTheCitysOwnDateRatherThanTheDevices() {
        // 03:00 on 20 January in Yangon is still 12:30 on the 19th in Los
        // Angeles, so the selected city's solar day is the day before the
        // device's.
        val map = map(
            travelMinute = MinuteOfDay.of(3, 0),
            activeCityId = "los-angeles",
        )

        assertEquals("Sat, 20 Jan", map.deviceDateText)
        assertEquals(LocalDate.of(2024, 1, 19), map.solarDate)
    }

    // ------------------------------------------------------- active fallback

    @Test
    fun anActiveCityThatIsNoLongerSavedFallsBackToTheFirstCity() {
        val map = map(activeCityId = "atlantis")

        assertEquals("tokyo", map.activeCity?.id)
        assertEquals("tokyo", map.solarCityId)
    }

    @Test
    fun anEmptyMapHasNoActiveCityAndNoSolarRequest() {
        val map = map(savedCityIds = emptyList(), activeCityId = null)

        assertNull(map.activeCity)
        assertNull(map.solarCityId)
        assertNull(map.solarDate)
        assertNull(map.solar)
        assertTrue(map.isLive)
    }

    // ----------------------------------------------------------- solar events

    @Test
    fun aSolarSummaryForTheSelectedCityAndDateIsAccepted() {
        val live = map()
        val summary = buildMapSolarSummary(
            events(LocalDate.of(2024, 1, 20), ZoneId.of("America/Los_Angeles")),
            cityId = "los-angeles",
            use24Hour = true,
        )

        val accepted = buildMapUiState(
            state = PreviewSamples.clockState(),
            deviceZoneId = deviceZone,
            solar = summary,
        )

        assertEquals(summary, accepted.solar)
        assertEquals(live.solarDate, accepted.solar?.date)
    }

    @Test
    fun aSolarSummaryFromAPreviousDateIsRejected() {
        val summary = buildMapSolarSummary(
            events(LocalDate.of(2024, 1, 19), ZoneId.of("America/Los_Angeles")),
            cityId = "los-angeles",
            use24Hour = true,
        )

        val map = buildMapUiState(
            state = PreviewSamples.clockState(),
            deviceZoneId = deviceZone,
            solar = summary,
        )

        assertNull(map.solar)
    }

    @Test
    fun aSolarSummaryFromAPreviousCityIsRejected() {
        val summary = buildMapSolarSummary(
            events(LocalDate.of(2024, 1, 20), ZoneId.of("Asia/Tokyo")),
            cityId = "tokyo",
            use24Hour = true,
        )

        val map = buildMapUiState(
            state = PreviewSamples.clockState(),
            deviceZoneId = deviceZone,
            solar = summary,
        )

        assertNull(map.solar)
    }

    @Test
    fun solarTimesFollowTheAppWideClockFormat() {
        val events = events(LocalDate.of(2024, 1, 20), ZoneId.of("America/Los_Angeles"))

        val twentyFour = buildMapSolarSummary(events, "los-angeles", use24Hour = true)
        assertEquals("06:30", twentyFour.sunriseText)
        assertEquals("17:05", twentyFour.sunsetText)

        val twelve = buildMapSolarSummary(events, "los-angeles", use24Hour = false)
        assertEquals("6:30 AM", twelve.sunriseText)
        assertEquals("5:05 PM", twelve.sunsetText)
    }

    @Test
    fun theMapReadsTwelveHourTimeWhenTheAppIsSetToIt() {
        val map = map(use24Hour = false)

        assertEquals("11:10 PM", map.deviceTimeText)
        assertEquals("8:40 AM", city(map, "los-angeles").timeText)
        assertEquals("1:40 AM", city(map, "tokyo").timeText)
    }

    @Test
    fun aPolarDayHasNoSunriseOrSunsetText() {
        val events = events(
            date = LocalDate.of(2024, 6, 20),
            zone = ZoneId.of("America/Los_Angeles"),
            sunrise = null,
            sunset = null,
            kind = SolarDayKind.POLAR_DAY,
            dayLength = Duration.ofHours(24),
        )

        val summary = buildMapSolarSummary(events, "los-angeles", use24Hour = true)

        assertEquals(SolarDayKind.POLAR_DAY, summary.kind)
        assertNull(summary.sunriseText)
        assertNull(summary.sunsetText)
    }

    @Test
    fun aPolarNightHasNoSunriseOrSunsetText() {
        val events = events(
            date = LocalDate.of(2024, 12, 21),
            zone = ZoneId.of("America/Los_Angeles"),
            sunrise = null,
            sunset = null,
            kind = SolarDayKind.POLAR_NIGHT,
            dayLength = Duration.ZERO,
        )

        val summary = buildMapSolarSummary(events, "los-angeles", use24Hour = false)

        assertEquals(SolarDayKind.POLAR_NIGHT, summary.kind)
        assertNull(summary.sunriseText)
        assertNull(summary.sunsetText)
    }
}

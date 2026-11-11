package com.pj95.clockwise.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.data.local.ClockwiseDatabase
import com.pj95.clockwise.data.local.dao.SolarDao
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.domain.solar.SunriseSunsetCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Guards the cache contract around the solar engine. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SolarRepositoryTest {

    private lateinit var database: ClockwiseDatabase
    private lateinit var dao: SolarDao

    private val tokyo = CityDirectory.all.first { it.id == "tokyo" }
    private val fixedClock = Clock.fixed(Instant.parse("2024-06-20T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClockwiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.solarDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun secondRequestForTheSameCityAndDayUsesTheCache() = runBlocking {
        var calculations = 0
        val repository = SolarRepository(dao, fixedClock) { date, point, zone ->
            calculations += 1
            SunriseSunsetCalculator.events(date, point, zone)
        }
        val date = LocalDate.of(2024, 6, 20)

        val first = repository.events(tokyo, date)
        val second = repository.events(tokyo, date)

        assertEquals(1, calculations)
        assertEquals(first, second)
        assertEquals(1, dao.count())
    }

    @Test
    fun aCacheHitStillReturnsTheRequestedZone() = runBlocking {
        val repository = SolarRepository(dao, fixedClock)
        val date = LocalDate.of(2024, 6, 20)
        repository.events(tokyo, date)

        val again = repository.events(tokyo, date)

        assertEquals(ZoneId.of("Asia/Tokyo"), again.zone)
        assertEquals(date, again.date)
    }

    @Test
    fun correctedCoordinatesInvalidateTheCachedRow() = runBlocking {
        var calculations = 0
        val repository = SolarRepository(dao, fixedClock) { date, point, zone ->
            calculations += 1
            SunriseSunsetCalculator.events(date, point, zone)
        }
        val date = LocalDate.of(2024, 6, 20)
        repository.events(tokyo, date)

        val moved = tokyo.copy(latitude = tokyo.latitude + 1.0)
        repository.events(moved, date)

        assertEquals(2, calculations)
        val stored = dao.find(tokyo.id, date.toEpochDay())
        assertEquals(moved.latitude, stored?.latitude)
    }

    @Test
    fun anInstantUsesTheCitysLocalDateNotTheUtcDate() = runBlocking {
        val repository = SolarRepository(dao, fixedClock)
        val instant = Instant.parse("2024-06-19T20:00:00Z")

        val events = repository.events(tokyo, instant)

        // 20:00 UTC is already 05:00 on 20 June in Tokyo.
        assertEquals(LocalDate.of(2024, 6, 20), events.date)
        assertEquals(ZoneId.of("Asia/Tokyo"), events.zone)
    }

    @Test
    fun explicitLocationUsesTheSameEngineAndCachesByCityId() = runBlocking {
        val repository = SolarRepository(dao, fixedClock)
        val date = LocalDate.of(2024, 12, 21)

        val events = repository.events(
            cityId = "tromso",
            latitude = 69.6492,
            longitude = 18.9553,
            date = date,
            zone = ZoneId.of("Europe/Oslo"),
        )

        assertEquals("tromso", dao.find("tromso", date.toEpochDay())?.cityId)
        assertEquals(events, repository.events(
            cityId = "tromso",
            latitude = 69.6492,
            longitude = 18.9553,
            date = date,
            zone = ZoneId.of("Europe/Oslo"),
        ))
    }

    @Test
    fun pruningDropsOnlyDaysBeforeTheCutoff() = runBlocking {
        val repository = SolarRepository(dao, fixedClock)
        val cutoff = LocalDate.of(2024, 6, 1)
        repository.events(tokyo, cutoff.minusDays(1))
        repository.events(tokyo, cutoff)
        repository.events(tokyo, cutoff.plusDays(1))

        val removed = repository.pruneBefore(cutoff)

        assertEquals(1, removed)
        assertNull(dao.find(tokyo.id, cutoff.minusDays(1).toEpochDay()))
        assertNotNull(dao.find(tokyo.id, cutoff.toEpochDay()))
        assertNotNull(dao.find(tokyo.id, cutoff.plusDays(1).toEpochDay()))
    }
}

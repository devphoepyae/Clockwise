package com.pj95.clockwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.data.local.dao.SolarDao
import com.pj95.clockwise.data.local.entity.SolarDayEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Guards the solar-day cache table: one row per city and day, replaceable. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SolarDaoTest {

    private lateinit var database: ClockwiseDatabase
    private lateinit var dao: SolarDao

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
    fun upsertAndFindReturnsTheStoredRow() = runBlocking {
        dao.upsert(row(dateEpochDay = 20_000, solarNoon = 1_000))

        val stored = dao.find("tokyo", 20_000)
        assertEquals("tokyo", stored?.cityId)
        assertEquals(1_000L, stored?.solarNoonEpochMilli)
        assertNull(dao.find("tokyo", 20_001))
    }

    @Test
    fun upsertingTheSameCityAndDayReplacesRatherThanDuplicates() = runBlocking {
        dao.upsert(row(dateEpochDay = 20_000, solarNoon = 1_000))
        dao.upsert(row(dateEpochDay = 20_000, solarNoon = 2_000))

        assertEquals(1, dao.count())
        assertEquals(2_000L, dao.find("tokyo", 20_000)?.solarNoonEpochMilli)
    }

    @Test
    fun differentCitiesOnTheSameDayAreSeparateRows() = runBlocking {
        dao.upsert(row(cityId = "tokyo", dateEpochDay = 20_000, solarNoon = 1_000))
        dao.upsert(row(cityId = "london", dateEpochDay = 20_000, solarNoon = 2_000))

        assertEquals(2, dao.count())
        assertEquals(1_000L, dao.find("tokyo", 20_000)?.solarNoonEpochMilli)
        assertEquals(2_000L, dao.find("london", 20_000)?.solarNoonEpochMilli)
    }

    @Test
    fun deleteOlderThanKeepsRecentRows() = runBlocking {
        dao.upsert(row(dateEpochDay = 19_999, solarNoon = 1_000))
        dao.upsert(row(dateEpochDay = 20_000, solarNoon = 2_000))

        assertEquals(1, dao.deleteOlderThan(20_000))
        assertEquals(1, dao.count())
        assertNull(dao.find("tokyo", 19_999))
        assertEquals(2_000L, dao.find("tokyo", 20_000)?.solarNoonEpochMilli)
    }

    @Test
    fun clearEmptiesTheCache() = runBlocking {
        dao.upsert(row(dateEpochDay = 20_000, solarNoon = 1_000))
        dao.upsert(row(cityId = "london", dateEpochDay = 20_000, solarNoon = 2_000))

        assertEquals(2, dao.clear())
        assertEquals(0, dao.count())
    }

    private fun row(
        cityId: String = "tokyo",
        dateEpochDay: Long,
        solarNoon: Long,
    ): SolarDayEntity = SolarDayEntity(
        cityId = cityId,
        dateEpochDay = dateEpochDay,
        latitude = 35.6762,
        longitude = 139.6503,
        kind = "NORMAL",
        sunriseEpochMilli = solarNoon - 20_000,
        sunsetEpochMilli = solarNoon + 20_000,
        solarNoonEpochMilli = solarNoon,
        dayLengthSeconds = 40_000,
        calculatedAtEpochMilli = 1_718_000_000_000,
    )
}

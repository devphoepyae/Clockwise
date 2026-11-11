package com.pj95.clockwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.data.local.dao.CityDao
import com.pj95.clockwise.data.local.seed.CitySeeder
import com.pj95.clockwise.model.CityDirectory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Guards the M0 Room foundation: seeding is idempotent, order survives, and the
 * catalog can answer the searches the city sheet needs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CityDaoTest {

    private lateinit var database: ClockwiseDatabase
    private lateinit var dao: CityDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClockwiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.cityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedingInsertsEveryBundledCityInDirectoryOrder() = runBlocking {
        CitySeeder.seed(dao)

        val rows = dao.getAll()
        assertEquals(CityDirectory.all.size, rows.size)
        assertEquals(CityDirectory.all.map { it.id }, rows.map { it.id })
        assertEquals(CityDirectory.all.first().name, rows.first().nameEn)
    }

    @Test
    fun seedingTwiceDoesNotDuplicateRows() = runBlocking {
        CitySeeder.seed(dao)
        CitySeeder.seed(dao)

        assertEquals(CityDirectory.all.size, dao.count())
    }

    @Test
    fun catalogFlowEmitsRowsInDirectoryOrder() = runBlocking {
        CitySeeder.seed(dao)

        val ids = dao.observeAll().first().map { it.id }
        assertEquals(CityDirectory.all.map { it.id }, ids)
    }

    @Test
    fun searchMatchesCountryAndTimezone() = runBlocking {
        CitySeeder.seed(dao)

        assertEquals(listOf("tokyo"), dao.search("Japan").map { it.id })
        assertEquals(listOf("yangon"), dao.search("Asia/Yangon").map { it.id })
        assertEquals(listOf("los-angeles"), dao.search("California").map { it.id })
    }

    @Test
    fun unknownIdReturnsNull() = runBlocking {
        CitySeeder.seed(dao)

        assertNull(dao.findById("atlantis"))
    }
}

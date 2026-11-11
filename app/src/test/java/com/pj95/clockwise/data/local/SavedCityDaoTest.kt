package com.pj95.clockwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.data.local.dao.SavedCityDao
import com.pj95.clockwise.data.local.entity.SavedCityEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The Room-backed display order: a reorder is an ordinary transaction, the
 * observable list follows it, and removing a city closes the gap in the
 * positions instead of leaving one behind.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SavedCityDaoTest {

    private lateinit var database: ClockwiseDatabase
    private lateinit var dao: SavedCityDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClockwiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.savedCityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun anEmptyTableReadsAsAnEmptyOrder() = runBlocking {
        assertTrue(dao.getCityIds().isEmpty())
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun replaceAllStoresADenseZeroBasedOrder() = runBlocking {
        dao.replaceAll(listOf("tokyo", "london", "sydney"))

        assertEquals(listOf("tokyo", "london", "sydney"), dao.getCityIds())
        assertEquals(
            listOf(
                SavedCityEntity("tokyo", 0),
                SavedCityEntity("london", 1),
                SavedCityEntity("sydney", 2),
            ),
            dao.getAll(),
        )
    }

    @Test
    fun reorderingReplacesTheWholeOrderRatherThanAppending() = runBlocking {
        dao.replaceAll(listOf("tokyo", "london", "sydney"))

        dao.replaceAll(listOf("sydney", "tokyo", "london"))

        assertEquals(listOf("sydney", "tokyo", "london"), dao.getCityIds())
        // The positions are rewritten, so no stale row survives at position 2.
        assertEquals(listOf(0, 1, 2), dao.getAll().map { it.position })
    }

    @Test
    fun theObservableOrderFollowsEveryWrite() = runBlocking {
        dao.replaceAll(listOf("tokyo", "london"))

        dao.replaceAll(listOf("london", "tokyo"))

        assertEquals(listOf("london", "tokyo"), dao.observeAll().first().map { it.cityId })
    }

    @Test
    fun replaceAllWithNoCitiesClearsTheTable() = runBlocking {
        dao.replaceAll(listOf("tokyo", "london"))

        dao.replaceAll(emptyList())

        assertTrue(dao.getCityIds().isEmpty())
    }

    @Test
    fun appendIfMissingKeepsTheExistingOrderAndDoesNotDuplicate() = runBlocking {
        dao.replaceAll(listOf("tokyo", "london"))

        dao.appendIfMissing("paris")
        dao.appendIfMissing("tokyo")

        assertEquals(listOf("tokyo", "london", "paris"), dao.getCityIds())
    }

    @Test
    fun removeClosesTheGapAndIgnoresUnknownIds() = runBlocking {
        dao.replaceAll(listOf("tokyo", "london", "paris"))

        dao.remove("london")
        dao.remove("atlantis")

        assertEquals(listOf("tokyo", "paris"), dao.getCityIds())
        assertEquals(listOf(0, 1), dao.getAll().map { it.position })
    }

    @Test
    fun duplicateIdsCannotCreateDuplicateRows() = runBlocking {
        // The primary key is the city id, so even a caller passing the same id
        // twice leaves one row rather than violating the order.
        dao.replaceAll(listOf("tokyo", "tokyo", "london"))

        assertEquals(listOf("tokyo", "london"), dao.getCityIds())
    }
}

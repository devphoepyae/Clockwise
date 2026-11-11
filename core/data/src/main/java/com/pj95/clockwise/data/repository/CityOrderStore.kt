package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.SavedCityDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The persisted display order of the saved cities.
 *
 * An interface rather than the Room DAO directly so `ClockRepository`'s
 * ordering rules can be tested without a database, and so a future storage
 * change does not reach the domain model. The production implementation is
 * [RoomCityOrderStore].
 */
interface CityOrderStore {

    /** The saved order, observed so a reorder repaints every collector. */
    val order: Flow<List<String>>

    /** Current order, read once when seeding or reconciling. */
    suspend fun getOrder(): List<String>

    /** Replaces the whole order atomically. */
    suspend fun replaceAll(cityIds: List<String>)

    /** Appends [cityId] when it is not already present. */
    suspend fun appendIfMissing(cityId: String)

    /** Removes [cityId] and closes the gap in the positions. */
    suspend fun remove(cityId: String)
}

/** Room-backed [CityOrderStore]; the one the app uses at runtime. */
class RoomCityOrderStore(
    private val dao: SavedCityDao,
) : CityOrderStore {

    override val order: Flow<List<String>> = dao.observeAll()
        .map { rows -> rows.map { it.cityId } }

    override suspend fun getOrder(): List<String> = dao.getCityIds()

    override suspend fun replaceAll(cityIds: List<String>) = dao.replaceAll(cityIds)

    override suspend fun appendIfMissing(cityId: String) = dao.appendIfMissing(cityId)

    override suspend fun remove(cityId: String) = dao.remove(cityId)
}

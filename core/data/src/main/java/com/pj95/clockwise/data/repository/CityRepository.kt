package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.CityDao
import com.pj95.clockwise.data.local.entity.toCity
import com.pj95.clockwise.data.local.seed.CitySeeder
import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.CityDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The queryable city catalog.
 *
 * Proto DataStore still owns which cities the user saved and in what order;
 * this repository owns the city definitions themselves. A Room failure falls
 * back to the bundled directory rather than leaving the app without a catalog.
 */
class CityRepository(
    private val dao: CityDao,
) {

    /** Every catalog city, ordered for display. */
    val cities: Flow<List<City>> = dao.observeAll()
        .map { rows -> rows.map { it.toCity() } }
        .catch { emit(CityDirectory.all) }

    /** Copies the bundled directory into Room; safe to call on every launch. */
    suspend fun ensureSeeded() {
        CitySeeder.seed(dao)
    }

    /** Searches the catalog by name, region, country or IANA timezone. */
    suspend fun search(query: String): List<City> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return CityDirectory.all
        return dao.search(trimmed).map { it.toCity() }
    }

    /** The catalog city with [id], or `null` when it is unknown. */
    suspend fun find(id: String): City? = dao.findById(id)?.toCity()
}

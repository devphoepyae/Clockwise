package com.pj95.clockwise.data.local.seed

import com.pj95.clockwise.data.local.dao.CityDao
import com.pj95.clockwise.data.local.entity.toEntity
import com.pj95.clockwise.model.CityDirectory

/**
 * Copies the bundled directory into Room.
 *
 * The write is idempotent: [CityDao.upsertAll] replaces matching ids, so a
 * launch after an app update refreshes built-in coordinates without duplicating
 * rows or deleting future custom cities.
 */
object CitySeeder {

    suspend fun seed(dao: CityDao) {
        val rows = CityDirectory.all.mapIndexed { index, city ->
            city.toEntity(sortOrder = index)
        }
        dao.upsertAll(rows)
    }
}

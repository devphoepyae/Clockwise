package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.LandmarkType

/**
 * A city in the local Room catalog.
 *
 * The bundled directory remains the source of the built-in rows; Room makes the
 * catalog queryable and gives a future custom-city feature somewhere to live.
 * [sortOrder] preserves the directory's presentation order without relying on
 * the id's lexical order.
 *
 * A city id is stable and safe to persist. [nameEn] and the other labels are
 * the canonical English values; UI-level localization is resolved separately so
 * changing the language never rewrites the catalog.
 */
@Entity(
    tableName = "cities",
    indices = [
        Index("timeZone"),
        Index("isBuiltIn"),
        Index("sortOrder"),
    ],
)
data class CityEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val regionEn: String,
    val countryEn: String,
    val timeZone: String,
    val latitude: Double,
    val longitude: Double,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
    /** The stored [LandmarkType] name, or `null` for the ambient fallback. */
    val landmarkType: String? = null,
)

/** Maps a bundled directory entry into its Room row. */
fun City.toEntity(sortOrder: Int): CityEntity = CityEntity(
    id = id,
    nameEn = name,
    regionEn = region,
    countryEn = country,
    timeZone = timeZone,
    latitude = latitude,
    longitude = longitude,
    isBuiltIn = true,
    sortOrder = sortOrder,
    landmarkType = landmarkType?.name,
)

/** Maps a Room row back into the domain model used by the screens. */
fun CityEntity.toCity(): City = City(
    id = id,
    name = nameEn,
    region = regionEn,
    country = countryEn,
    timeZone = timeZone,
    latitude = latitude,
    longitude = longitude,
    landmarkType = LandmarkType.fromStored(landmarkType),
)

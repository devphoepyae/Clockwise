package com.pj95.clockwise.model

/**
 * The offline city directory (spec section 5).
 *
 * Twenty-two cities with correct IANA timezone ids and map coordinates. This is
 * bundled local data — the app never calls a backend or geocoding service.
 */
object CityDirectory {

    /** Cities saved on first launch, in order. */
    val defaultSavedCityIds: List<String> = listOf(
        "tokyo",
        "sydney",
        "los-angeles",
        "new-york",
        "london",
    )

    /** City highlighted on first launch. */
    const val defaultActiveCityId: String = "los-angeles"

    /**
     * The vector landmark each built-in city draws on its detail screen.
     *
     * Kept beside the directory rather than in the Room seed so the mapping is
     * reviewable in one place. A city without an entry gets the ambient fallback
     * pattern, which is also what a custom offline city will get.
     */
    private val landmarks: Map<String, LandmarkType> = mapOf(
        "tokyo" to LandmarkType.TOWER,
        "sydney" to LandmarkType.CLASSIC_ARCH,
        "los-angeles" to LandmarkType.SKYSCRAPER,
        "new-york" to LandmarkType.SKYSCRAPER,
        "london" to LandmarkType.CLASSIC_ARCH,
        "yangon" to LandmarkType.PAGODA,
        "singapore" to LandmarkType.SKYSCRAPER,
        "bangkok" to LandmarkType.PAGODA,
        "mumbai" to LandmarkType.CLASSIC_ARCH,
        "dubai" to LandmarkType.SKYSCRAPER,
        "paris" to LandmarkType.TOWER,
        "berlin" to LandmarkType.CLASSIC_ARCH,
        "cairo" to LandmarkType.CLASSIC_ARCH,
        "johannesburg" to LandmarkType.SKYSCRAPER,
        "reykjavik" to LandmarkType.TOWER,
        "toronto" to LandmarkType.SKYSCRAPER,
        "vancouver" to LandmarkType.SKYSCRAPER,
        "seoul" to LandmarkType.SKYSCRAPER,
        "hong-kong" to LandmarkType.SKYSCRAPER,
        "auckland" to LandmarkType.TOWER,
        "honolulu" to LandmarkType.BRIDGE,
        "buenos-aires" to LandmarkType.CLASSIC_ARCH,
    )

    /** The landmark for [id], or `null` when the city uses the fallback. */
    fun landmarkFor(id: String): LandmarkType? = landmarks[id]

    val all: List<City> = listOf(
        City(
            id = "tokyo",
            name = "Tokyo",
            region = "Japan",
            country = "Japan",
            timeZone = "Asia/Tokyo",
            latitude = 35.6762,
            longitude = 139.6503,
        ),
        City(
            id = "sydney",
            name = "Sydney",
            region = "New South Wales, Australia",
            country = "Australia",
            timeZone = "Australia/Sydney",
            latitude = -33.8688,
            longitude = 151.2093,
        ),
        City(
            id = "los-angeles",
            name = "Los Angeles",
            region = "California, USA",
            country = "United States",
            timeZone = "America/Los_Angeles",
            latitude = 34.0522,
            longitude = -118.2437,
        ),
        City(
            id = "new-york",
            name = "New York",
            region = "New York, USA",
            country = "United States",
            timeZone = "America/New_York",
            latitude = 40.7128,
            longitude = -74.0060,
        ),
        City(
            id = "london",
            name = "London",
            region = "United Kingdom",
            country = "United Kingdom",
            timeZone = "Europe/London",
            latitude = 51.5074,
            longitude = -0.1278,
        ),
        City(
            id = "yangon",
            name = "Yangon",
            region = "Myanmar",
            country = "Myanmar",
            timeZone = "Asia/Yangon",
            latitude = 16.8409,
            longitude = 96.1735,
        ),
        City(
            id = "singapore",
            name = "Singapore",
            region = "Singapore",
            country = "Singapore",
            timeZone = "Asia/Singapore",
            latitude = 1.3521,
            longitude = 103.8198,
        ),
        City(
            id = "bangkok",
            name = "Bangkok",
            region = "Thailand",
            country = "Thailand",
            timeZone = "Asia/Bangkok",
            latitude = 13.7563,
            longitude = 100.5018,
        ),
        City(
            id = "mumbai",
            name = "Mumbai",
            region = "Maharashtra, India",
            country = "India",
            timeZone = "Asia/Kolkata",
            latitude = 19.0760,
            longitude = 72.8777,
        ),
        City(
            id = "dubai",
            name = "Dubai",
            region = "United Arab Emirates",
            country = "United Arab Emirates",
            timeZone = "Asia/Dubai",
            latitude = 25.2048,
            longitude = 55.2708,
        ),
        City(
            id = "paris",
            name = "Paris",
            region = "France",
            country = "France",
            timeZone = "Europe/Paris",
            latitude = 48.8566,
            longitude = 2.3522,
        ),
        City(
            id = "berlin",
            name = "Berlin",
            region = "Germany",
            country = "Germany",
            timeZone = "Europe/Berlin",
            latitude = 52.5200,
            longitude = 13.4050,
        ),
        City(
            id = "cairo",
            name = "Cairo",
            region = "Egypt",
            country = "Egypt",
            timeZone = "Africa/Cairo",
            latitude = 30.0444,
            longitude = 31.2357,
        ),
        City(
            id = "johannesburg",
            name = "Johannesburg",
            region = "South Africa",
            country = "South Africa",
            timeZone = "Africa/Johannesburg",
            latitude = -26.2041,
            longitude = 28.0473,
        ),
        City(
            id = "reykjavik",
            name = "Reykjavik",
            region = "Iceland",
            country = "Iceland",
            timeZone = "Atlantic/Reykjavik",
            latitude = 64.1466,
            longitude = -21.9426,
        ),
        City(
            id = "toronto",
            name = "Toronto",
            region = "Ontario, Canada",
            country = "Canada",
            timeZone = "America/Toronto",
            latitude = 43.6532,
            longitude = -79.3832,
        ),
        City(
            id = "vancouver",
            name = "Vancouver",
            region = "British Columbia, Canada",
            country = "Canada",
            timeZone = "America/Vancouver",
            latitude = 49.2827,
            longitude = -123.1207,
        ),
        City(
            id = "seoul",
            name = "Seoul",
            region = "South Korea",
            country = "South Korea",
            timeZone = "Asia/Seoul",
            latitude = 37.5665,
            longitude = 126.9780,
        ),
        City(
            id = "hong-kong",
            name = "Hong Kong",
            region = "Hong Kong",
            country = "Hong Kong",
            timeZone = "Asia/Hong_Kong",
            latitude = 22.3193,
            longitude = 114.1694,
        ),
        City(
            id = "auckland",
            name = "Auckland",
            region = "New Zealand",
            country = "New Zealand",
            timeZone = "Pacific/Auckland",
            latitude = -36.8485,
            longitude = 174.7633,
        ),
        City(
            id = "honolulu",
            name = "Honolulu",
            region = "Hawaii, USA",
            country = "United States",
            timeZone = "Pacific/Honolulu",
            latitude = 21.3069,
            longitude = -157.8583,
        ),
        City(
            id = "buenos-aires",
            name = "Buenos Aires",
            region = "Argentina",
            country = "Argentina",
            timeZone = "America/Argentina/Buenos_Aires",
            latitude = -34.6037,
            longitude = -58.3816,
        ),
    ).map { city -> city.copy(landmarkType = landmarkFor(city.id)) }

    private val byId: Map<String, City> = all.associateBy(City::id)

    /** Returns the city with [id], or `null` when the id is unknown. */
    fun find(id: String): City? = byId[id]

    /**
     * Filters the directory by a free-text query, matching city name, region,
     * country label or IANA timezone id (spec section 4.2). A blank query
     * returns the whole directory.
     */
    fun search(query: String): List<City> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        return all.filter { city ->
            city.name.contains(trimmed, ignoreCase = true) ||
                city.region.contains(trimmed, ignoreCase = true) ||
                city.country.contains(trimmed, ignoreCase = true) ||
                city.timeZone.contains(trimmed, ignoreCase = true)
        }
    }

    /**
     * Maps saved ids to cities, silently dropping ids that are no longer in the
     * directory while preserving order (spec section 7).
     */
    fun citiesForIds(ids: List<String>): List<City> = ids.mapNotNull(byId::get)

    /** The cities shown on first launch, in their default order. */
    val defaultSavedCities: List<City> get() = citiesForIds(defaultSavedCityIds)
}

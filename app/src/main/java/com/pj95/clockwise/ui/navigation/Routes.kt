package com.pj95.clockwise.ui.navigation

/** Navigation routes for the app shell. */
object Routes {

    const val CLOCKS = "clocks"
    const val ALARMS = "alarms"
    const val PLANNER = "planner"

    /** Local information screens; no WebView and no network. */
    const val PRIVACY = "privacy"
    const val HELP = "help"
    const val ABOUT = "about"
    const val STUDIO = "studio"

    const val CITY_ID_ARG = "cityId"
    const val CITY_DETAIL = "city/{$CITY_ID_ARG}"

    fun cityDetail(cityId: String): String = "city/$cityId"
}

package com.pj95.clockwise.ui.mapping

import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.ui.WidgetUiState
import com.pj95.clockwise.ui.samples.PreviewSamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * The widget's contents: fewer fields than a city card, but the same times —
 * and the same day/night and cross-date-line decisions.
 *
 * The reference instant is the previews' `2024-01-20T16:40:00Z` in
 * `Asia/Yangon` (UTC+6:30), which is 23:10 on Saturday 20 January. Tokyo and
 * Sydney are already into Sunday; Los Angeles, New York and London are not.
 */
class WidgetStateMapperTest {

    private fun widget(
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        labels: Map<String, String> = emptyMap(),
        use24Hour: Boolean = true,
        deviceZoneId: ZoneId = PreviewSamples.sampleDeviceZoneId,
    ): WidgetUiState = buildWidgetUiState(
        savedCityIds = savedCityIds,
        cityLabels = labels,
        use24Hour = use24Hour,
        now = PreviewSamples.referenceInstant,
        deviceZoneId = deviceZoneId,
    )

    @Test
    fun everyRowIsTheSameReadingAsTheClocksScreen() {
        val widget = widget()
        val cards = PreviewSamples.clockState().cities

        assertEquals(cards.map { it.id }, widget.rows.map { it.id })
        cards.zip(widget.rows).forEach { (card, row) ->
            assertEquals(card.id, row.id)
            assertEquals(card.displayName, row.displayName)
            assertEquals(card.timeText, row.timeText)
            assertEquals(card.weekdayLabel, row.weekdayLabel)
            assertEquals(card.isDaytime, row.isDaytime)
        }
    }

    @Test
    fun theHeaderCarriesTheDeviceClockEveryRowIsReadAgainst() {
        assertEquals("23:10", widget().deviceTimeText)
    }

    @Test
    fun theAppWideFormatReachesTheWidget() {
        val widget = widget(use24Hour = false)

        assertEquals("1:40 AM", widget.rows.first { it.id == "tokyo" }.timeText)
        assertEquals("4:40 PM", widget.rows.first { it.id == "london" }.timeText)
        assertEquals("11:10 PM", widget.deviceTimeText)
    }

    @Test
    fun aLabelSavedInTheAppAppearsInTheRow() {
        val tokyo = widget(labels = mapOf("tokyo" to "Head Office"))
            .rows
            .first { it.id == "tokyo" }

        assertEquals("Tokyo (Head Office)", tokyo.displayName)
    }

    @Test
    fun aBlankLabelIsIgnored() {
        val tokyo = widget(labels = mapOf("tokyo" to "   "))
            .rows
            .first { it.id == "tokyo" }

        assertEquals("Tokyo", tokyo.displayName)
    }

    @Test
    fun citiesAcrossTheDateLineSayWhichDayTheyAreOn() {
        val rows = widget().rows.associateBy { it.id }

        assertTrue("Tokyo has rolled over to Sunday", rows.getValue("tokyo").isAnotherDay)
        assertTrue("Sydney has rolled over to Sunday", rows.getValue("sydney").isAnotherDay)
        assertFalse(rows.getValue("los-angeles").isAnotherDay)
        assertFalse(rows.getValue("new-york").isAnotherDay)
        assertFalse(rows.getValue("london").isAnotherDay)
    }

    @Test
    fun theSunAndMoonFollowEachCitysOwnClock() {
        val rows = widget().rows.associateBy { it.id }

        assertFalse("01:40 is night in Tokyo", rows.getValue("tokyo").isDaytime)
        assertFalse("03:40 is night in Sydney", rows.getValue("sydney").isDaytime)
        assertTrue("08:40 is day in Los Angeles", rows.getValue("los-angeles").isDaytime)
        assertTrue("11:40 is day in New York", rows.getValue("new-york").isDaytime)
        assertTrue("16:40 is day in London", rows.getValue("london").isDaytime)
    }

    @Test
    fun theSavedOrderIsTheWidgetOrder() {
        val order = listOf("london", "tokyo", "sydney", "new-york")

        assertEquals(order, widget(savedCityIds = order).rows.map { it.id })
    }

    @Test
    fun anUnknownIdIsSkippedRatherThanDrawnAsABlankRow() {
        val widget = widget(savedCityIds = listOf("tokyo", "not-a-city", "london"))

        assertEquals(listOf("tokyo", "london"), widget.rows.map { it.id })
    }

    @Test
    fun noSavedCitiesIsAnEmptyWidgetRatherThanNoWidget() {
        val widget = widget(savedCityIds = emptyList())

        assertTrue(widget.isEmpty)
        assertEquals(emptyList<Any>(), widget.rows)
        // The device's own clock still anchors the header.
        assertEquals("23:10", widget.deviceTimeText)
    }

    @Test
    fun theDeviceZoneDecidesWhichCitiesAreOnAnotherDay() {
        // Seen from London the same instant is 16:40 on Saturday: Tokyo and
        // Sydney are still on Sunday, but so is every city east of London, and
        // the two American cities are now the ones sharing the device's day.
        val rows = widget(deviceZoneId = ZoneId.of("Europe/London"))
            .rows
            .associateBy { it.id }

        assertTrue(rows.getValue("tokyo").isAnotherDay)
        assertTrue(rows.getValue("sydney").isAnotherDay)
        assertFalse(rows.getValue("los-angeles").isAnotherDay)
    }
}

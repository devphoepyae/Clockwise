package com.pj95.clockwise.shortcuts

import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.data.prefs.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mirror rule: republish when the saved ids or their labels change, and do
 * nothing for a change that cannot affect the launcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutMirrorTest {

    private val default = UserPreferences.Default
        .copy(
            savedCityIds = listOf("tokyo", "london"),
            cityLabels = mapOf("tokyo" to "Head Office"),
        )

    /**
     * A scope that starts collecting eagerly, so the initial publication does
     * not depend on when the test scheduler is advanced. Kept a child of
     * [TestScope.backgroundScope] so `runTest` still cancels it when the test
     * ends, even if an assertion fails.
     */
    private fun TestScope.mirrorScope(): CoroutineScope =
        CoroutineScope(
            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
        )

    @Test
    fun theCurrentCitiesArePublishedAsSoonAsTheMirrorStarts() = runTest {
        val preferences = MutableStateFlow(default)
        val published = mutableListOf<Pair<List<String>, Map<String, String>>>()

        ShortcutMirror(
            scope = mirrorScope(),
            preferences = preferences,
            publish = { ids, labels -> published += ids to labels },
        ).start()
        advanceUntilIdle()

        assertEquals(
            listOf(listOf("tokyo", "london") to mapOf("tokyo" to "Head Office")),
            published,
        )
    }

    @Test
    fun aThemeOrFormatChangeDoesNotRepublish() = runTest {
        val preferences = MutableStateFlow(default)
        var publishes = 0

        ShortcutMirror(
            scope = mirrorScope(),
            preferences = preferences,
            publish = { _, _ -> publishes++ },
        ).start()
        advanceUntilIdle()
        assertEquals(1, publishes)

        preferences.value = preferences.value.copy(
            theme = AppTheme.COOL_SLATE,
            use24Hour = false,
            activeCityId = "london",
        )
        advanceUntilIdle()

        // None of those inputs is drawn in the long-press menu.
        assertEquals(1, publishes)
    }

    @Test
    fun anAddedCityRepublishesTheWholeSet() = runTest {
        val preferences = MutableStateFlow(default)
        val published = mutableListOf<List<String>>()

        ShortcutMirror(
            scope = mirrorScope(),
            preferences = preferences,
            publish = { ids, _ -> published += ids },
        ).start()
        advanceUntilIdle()

        preferences.value = preferences.value.copy(
            savedCityIds = preferences.value.savedCityIds + "sydney",
        )
        advanceUntilIdle()

        assertEquals(
            listOf(listOf("tokyo", "london"), listOf("tokyo", "london", "sydney")),
            published,
        )
    }

    @Test
    fun aLabelChangeRepublishes() = runTest {
        val preferences = MutableStateFlow(default)
        val published = mutableListOf<Map<String, String>>()

        ShortcutMirror(
            scope = mirrorScope(),
            preferences = preferences,
            publish = { _, labels -> published += labels },
        ).start()
        advanceUntilIdle()

        preferences.value = preferences.value.copy(
            cityLabels = mapOf("tokyo" to "HQ"),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(mapOf("tokyo" to "Head Office"), mapOf("tokyo" to "HQ")),
            published,
        )
    }

    @Test
    fun aRepeatedSnapshotIsPublishedOnlyOnce() = runTest {
        val preferences = MutableStateFlow(default)
        var publishes = 0

        ShortcutMirror(
            scope = mirrorScope(),
            preferences = preferences,
            publish = { _, _ -> publishes++ },
        ).start()
        advanceUntilIdle()

        preferences.value = default
        advanceUntilIdle()

        assertEquals(1, publishes)
    }

    @Test
    fun aFailedPublicationIsReportedAndTheMirrorKeepsRunning() = runTest {
        val preferences = MutableStateFlow(default)
        val errors = mutableListOf<Throwable>()
        var attempts = 0

        ShortcutMirror(
            scope = mirrorScope(),
            preferences = preferences,
            publish = { _, _ ->
                attempts++
                if (attempts == 1) error("launcher refused the update")
            },
            onError = { errors += it },
        ).start()
        advanceUntilIdle()

        assertEquals(1, errors.size)

        preferences.value = preferences.value.copy(savedCityIds = listOf("tokyo"))
        advanceUntilIdle()

        // A launcher that is unavailable once must not stop later updates.
        assertEquals(2, attempts)
        assertTrue(errors.single().message!!.contains("launcher"))
    }
}

package com.pj95.clockwise.widget

import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.testing.FakeSavedCitiesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's second refresh path: the one that reacts to the app's own data
 * rather than to the clock.
 *
 * The minute ticker would eventually catch up with a newly added city; these
 * tests pin the behaviour that makes it immediate, and that only the three
 * inputs the widget actually renders are watched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetMirrorTest {

    private class Recorder {
        val redraws = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
    }

    private fun TestScope.newRepository(): ClockRepository =
        ClockRepository(FakeSavedCitiesDataStore())

    private fun TestScope.startMirror(
        repository: ClockRepository,
        recorder: Recorder,
        refresh: (suspend () -> Unit)? = null,
    ): Job = WidgetMirror(
        scope = this,
        repository = repository,
        refresh = refresh ?: { recorder.redraws += "redraw" },
        onError = { recorder.errors += it },
    ).start()

    @Test
    fun theCurrentStateIsDrawnAsSoonAsTheMirrorStarts() = runTest {
        val repository = newRepository()
        val recorder = Recorder()

        val job = startMirror(repository, recorder)
        runCurrent()

        assertEquals(1, recorder.redraws.size)
        assertTrue(recorder.errors.isEmpty())
        job.cancel()
    }

    @Test
    fun addingACityRedrawsTheWidgetStraightAway() = runTest {
        val repository = newRepository()
        val recorder = Recorder()
        val job = startMirror(repository, recorder)
        runCurrent()
        assertEquals(1, recorder.redraws.size)

        repository.addCity("paris")
        runCurrent()

        // One minute sooner than the ticker could have managed.
        assertEquals(2, recorder.redraws.size)
        job.cancel()
    }

    @Test
    fun removingACityRedrawsTheWidgetStraightAway() = runTest {
        val repository = newRepository()
        val recorder = Recorder()
        val job = startMirror(repository, recorder)
        runCurrent()

        repository.removeCity("tokyo")
        runCurrent()

        assertEquals(2, recorder.redraws.size)
        job.cancel()
    }

    @Test
    fun labellingACityRedrawsTheWidget() = runTest {
        val repository = newRepository()
        val recorder = Recorder()
        val job = startMirror(repository, recorder)
        runCurrent()

        repository.setCityLabel("tokyo", "Head Office")
        runCurrent()

        assertEquals(2, recorder.redraws.size)
        job.cancel()
    }

    @Test
    fun switchingBetweenTwelveAndTwentyFourHourRedrawsTheWidget() = runTest {
        val repository = newRepository()
        val recorder = Recorder()
        val job = startMirror(repository, recorder)
        runCurrent()

        repository.setUse24Hour(false)
        runCurrent()

        assertEquals(2, recorder.redraws.size)
        job.cancel()
    }

    @Test
    fun aChangeTheWidgetDoesNotRenderLeavesTheHomeScreenAlone() = runTest {
        val repository = newRepository()
        val recorder = Recorder()
        val job = startMirror(repository, recorder)
        runCurrent()

        // The active city and the theme are the screens' business: a home screen
        // must not be redrawn for them. The widget follows the *device's*
        // light/dark setting, so a change of app theme cannot alter it anyway.
        repository.selectCity("sydney")
        repository.setTheme(AppTheme.COOL_SLATE)
        runCurrent()

        assertEquals(1, recorder.redraws.size)
        job.cancel()
    }

    @Test
    fun oneFailedRedrawIsReportedAndDoesNotEndTheMirror() = runTest {
        val repository = newRepository()
        val recorder = Recorder()
        var failNext = true

        val job = startMirror(
            repository = repository,
            recorder = recorder,
            refresh = {
                if (failNext) {
                    failNext = false
                    throw IllegalStateException("the widget host is gone")
                }
                recorder.redraws += "redraw"
            },
        )
        runCurrent()

        // Reported, never swallowed...
        assertEquals(1, recorder.errors.size)
        assertEquals(0, recorder.redraws.size)

        // ...and the mirror is still watching, so the next change still lands.
        repository.addCity("paris")
        runCurrent()

        assertEquals(1, recorder.redraws.size)
        assertEquals(1, recorder.errors.size)
        job.cancel()
    }
}

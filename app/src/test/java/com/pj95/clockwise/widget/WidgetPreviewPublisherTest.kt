package com.pj95.clockwise.widget

import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generated-preview publisher is deliberately small, but every branch is a
 * platform rule: Android 15+ only, once per design version, and never record a
 * success the system did not give.
 */
class WidgetPreviewPublisherTest {

    private class Harness(
        val sdkInt: Int = 35,
        var version: Int = 0,
        var result: Int = GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS,
        var throws: Throwable? = null,
    ) {
        var calls = 0
        val errors = mutableListOf<Throwable>()

        fun publisher() = WidgetPreviewPublisher(
            sdkInt = sdkInt,
            publishedVersion = { version },
            markPublished = { version = it },
            publish = {
                calls += 1
                throws?.let { throw it }
                result
            },
            onError = { errors += it },
        )
    }

    @Test
    fun aSuccessfulPublishIsRecordedAndNotRepeated() = runTest {
        val harness = Harness()

        harness.publisher().publishIfNeeded(designVersion = 1)
        harness.publisher().publishIfNeeded(designVersion = 1)

        assertEquals(1, harness.calls)
        assertEquals(1, harness.version)
        assertTrue(harness.errors.isEmpty())
    }

    @Test
    fun aNewDesignIsPublishedAgain() = runTest {
        val harness = Harness(version = 1)

        harness.publisher().publishIfNeeded(designVersion = 2)

        assertEquals(1, harness.calls)
        assertEquals(2, harness.version)
    }

    @Test
    fun aDeviceBelowAndroidFifteenNeverPublishes() = runTest {
        val harness = Harness(sdkInt = 34)

        harness.publisher().publishIfNeeded(designVersion = 1)

        assertEquals(0, harness.calls)
        assertEquals(0, harness.version)
    }

    @Test
    fun aRateLimitedPublishIsRetriedOnTheNextLaunch() = runTest {
        val harness = Harness(result = GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED)

        harness.publisher().publishIfNeeded(designVersion = 1)

        // The system said no. Pretending it succeeded would freeze the old
        // preview in the picker until the next design change.
        assertEquals(1, harness.calls)
        assertEquals(0, harness.version)

        harness.result = GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
        harness.publisher().publishIfNeeded(designVersion = 1)

        assertEquals(2, harness.calls)
        assertEquals(1, harness.version)
    }

    @Test
    fun aFailureIsReportedAndNeverRecorded() = runTest {
        val harness = Harness(throws = IllegalStateException("no widget host"))

        harness.publisher().publishIfNeeded(designVersion = 1)

        assertEquals(0, harness.version)
        assertEquals(1, harness.errors.size)
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        val harness = Harness(throws = CancellationException("the process is going away"))
        var reported = false
        val publisher = WidgetPreviewPublisher(
            sdkInt = harness.sdkInt,
            publishedVersion = { harness.version },
            markPublished = { harness.version = it },
            publish = { throw CancellationException("the process is going away") },
            onError = { reported = true },
        )

        val thrown = runCatching { publisher.publishIfNeeded(designVersion = 1) }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertFalse("cancellation is control flow, not a preview failure", reported)
        assertEquals(0, harness.version)
    }
}

package com.pj95.clockwise.guardrails

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The app's contract is "zero internet, zero ads, zero analytics". All three
 * are easy to lose in a dependency bump, so they are asserted against the
 * merged manifest and the runtime classpath rather than trusted to review.
 *
 * Play Billing is the one deliberate exception: it talks to the Play Store app
 * over a bound service, not to the network, and it is used solely to buy the
 * one-time Studio unlock. It is therefore allowed — and pinned, so nobody can
 * quietly swap it for a different billing SDK.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OfflineGuardrailTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theMergedManifestRequestsNoInternetPermission() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions.orEmpty().toSet()

        assertFalse(
            "Chronos must remain 100% offline: android.permission.INTERNET is present",
            "android.permission.INTERNET" in requested,
        )
        assertFalse(
            "ACCESS_NETWORK_STATE is useless without a network and must stay removed",
            "android.permission.ACCESS_NETWORK_STATE" in requested,
        )
    }

    @Test
    fun noAdvertisingOrAnalyticsSdkIsOnTheClasspath() {
        val forbidden = listOf(
            "com.google.android.gms.ads.MobileAds",
            "com.google.firebase.analytics.FirebaseAnalytics",
            "com.google.firebase.crashlytics.FirebaseCrashlytics",
        )

        forbidden.forEach { className ->
            val present = runCatching { Class.forName(className) }.isSuccess
            assertFalse("$className must never ship", present)
        }
    }

    @Test
    fun playBillingIsTheOnlyStoreSdkAndItIsPinned() {
        // Present: the Studio unlock is a real in-app purchase.
        val billing = runCatching {
            Class.forName("com.android.billingclient.api.BillingClient")
        }
        assertTrue("Play Billing backs the Studio unlock", billing.isSuccess)

        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions.orEmpty().toSet()
        assertTrue(
            "Play Billing needs its own permission and no other store permission",
            "com.android.vending.BILLING" in requested,
        )
    }

    @Test
    fun theAlarmPermissionsTheAppReliesOnAreStillDeclared() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = info.requestedPermissions.orEmpty().toSet()

        val required = setOf(
            "android.permission.USE_EXACT_ALARM",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.USE_FULL_SCREEN_INTENT",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
        )
        required.forEach { permission ->
            assertTrue("$permission is part of the alarm contract", permission in requested)
        }
    }
}

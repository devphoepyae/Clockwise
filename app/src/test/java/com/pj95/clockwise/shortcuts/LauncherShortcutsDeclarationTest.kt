package com.pj95.clockwise.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * The fixed quick actions are part of the app's manifest contract, so they are
 * checked from the packaged resources rather than from a copy in the test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LauncherShortcutsDeclarationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theQuickActionsAreDeclaredInOrder() {
        assertEquals(
            listOf(
                MainActivity.SHORTCUT_ADD_CITY,
                MainActivity.SHORTCUT_PLANNER,
            ),
            staticShortcutIds(),
        )
    }

    @Test
    fun everyQuickActionTargetsTheMainActivity() {
        val parser = context.resources.getXml(R.xml.shortcuts)
        val targetClasses = mutableListOf<String?>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "intent") {
                targetClasses += parser.getAttributeValue(ANDROID_NAMESPACE, "targetClass")
            }
            event = parser.next()
        }

        // Fully qualified on purpose: the application id and the namespace are
        // both com.pj95.clockwise, so a relative `.MainActivity` would resolve
        // too; the explicit name keeps the intent independent of the manifest's
        // package resolution.
        assertEquals(List(2) { MainActivity::class.java.name }, targetClasses)
    }

    @Test
    fun theActivityDeclaresTheShortcutResourceInItsMetadata() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            PackageManager.GET_META_DATA,
        )

        assertEquals(
            R.xml.shortcuts,
            info.metaData.getInt("android.app.shortcuts"),
        )
    }

    private fun staticShortcutIds(): List<String> {
        val parser = context.resources.getXml(R.xml.shortcuts)
        val ids = mutableListOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "shortcut") {
                ids += parser.getAttributeValue(ANDROID_NAMESPACE, "shortcutId")
            }
            event = parser.next()
        }
        return ids
    }

    private companion object {
        /** Every attribute in `shortcuts.xml` is in the Android namespace. */
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}

package com.pj95.clockwise.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the PJ95 "Concept 3" adaptive icon to a PNG so the mark can be
 * reviewed, and asserts the pieces the launcher depends on.
 *
 * The render is the layered icon the launcher actually composes — background
 * drawable, foreground drawable, then the 72 dp circular mask — rather than
 * the background colour on its own, so `app_icon.png` shows what a home screen
 * shows. Inflating all three layers also validates every path in the vectors,
 * which is the only thing a JVM can check about artwork.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AppIconTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun launcherIcon_renders() {
        val background = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
        assertNotNull("launcher background drawable must inflate", background)

        val foreground = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        assertNotNull("launcher foreground drawable must inflate", foreground)

        val sizePx = 432 // 108 dp at 4x
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // The background layer is drawn edge to edge; the launcher crops it.
        background!!.setBounds(0, 0, sizePx, sizePx)
        background.draw(canvas)

        // A circular launcher mask shows the central 72 dp of the 108 dp canvas.
        val maskRadius = sizePx * 36f / 108f
        val clip = Path().apply {
            addCircle(sizePx / 2f, sizePx / 2f, maskRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)
        foreground!!.setBounds(0, 0, sizePx, sizePx)
        foreground.draw(canvas)
        canvas.restore()

        val output = File("build/screenshots/app_icon.png")
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun theManifestPointsAtTheConceptThreeIcon() {
        val info = context.applicationInfo

        // `ApplicationInfo.roundIcon` is not part of the public SDK stubs, so the
        // round icon is asserted through the resource it must resolve to; the
        // manifest attribute itself is checked against the merged manifest by
        // the release-checklist command.
        assertEquals(R.mipmap.ic_launcher, info.icon)
        assertNotNull(
            "the round icon must be a real resource",
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round),
        )
    }

    @Test
    fun everyIconLayerInflates() {
        listOf(
            R.drawable.ic_launcher_background,
            R.drawable.ic_launcher_foreground,
            R.drawable.ic_launcher_monochrome,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_round,
        ).forEach { id ->
            assertNotNull(
                "icon resource 0x${id.toString(16)} must inflate",
                ContextCompat.getDrawable(context, id),
            )
        }
    }
}

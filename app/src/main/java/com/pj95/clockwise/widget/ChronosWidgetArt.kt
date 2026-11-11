package com.pj95.clockwise.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * The two hand-drawn backdrops Chronos Studio's widgets need.
 *
 * `RemoteViews` cannot run a Compose `Canvas`, and Glance has no drawing
 * primitive, so a style that needs more than rectangles and text paints itself
 * into a bitmap once per update. That is cheap — two 512 px squares per minute —
 * and it keeps the art vector-crisp at every launcher density because it is
 * rasterised at the target size rather than shipped as a fixed PNG.
 *
 * Both renderers take the Material You accent as an ARGB int, so the same code
 * produces a wallpaper-tinted dial on Android 12+ and a neutral one below.
 */
internal object ChronosWidgetArt {

    private const val SIZE = 512
    private const val CENTER = SIZE / 2f

    /**
     * The 24-hour dial: one full turn per day, a progress arc for the elapsed
     * part of it, and a hand at the current time.
     *
     * @param dayFraction the local time as a fraction of the day, `0f..1f`
     */
    fun dial(accent: Int, dayFraction: Float, isDark: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fraction = dayFraction.coerceIn(0f, 1f)

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = withAlpha(accent, if (isDark) 70 else 54)
        }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = withAlpha(accent, if (isDark) 40 else 30)
        }
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = withAlpha(accent, if (isDark) 110 else 80)
        }

        canvas.drawCircle(CENTER, CENTER, 214f, ring)
        canvas.drawCircle(CENTER, CENTER, 168f, inner)

        // 24 hour ticks; every sixth (00, 06, 12, 18) is longer and stronger.
        repeat(24) { hour ->
            val angle = Math.toRadians((hour * 15 - 90).toDouble())
            val major = hour % 6 == 0
            val outer = 206f
            val innerRadius = if (major) 184f else 194f
            tick.strokeWidth = if (major) 5f else 3f
            canvas.drawLine(
                CENTER + (innerRadius * Math.cos(angle)).toFloat(),
                CENTER + (innerRadius * Math.sin(angle)).toFloat(),
                CENTER + (outer * Math.cos(angle)).toFloat(),
                CENTER + (outer * Math.sin(angle)).toFloat(),
                tick,
            )
        }

        val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            color = accent
        }
        val arcBounds = RectF(CENTER - 214f, CENTER - 214f, CENTER + 214f, CENTER + 214f)
        canvas.drawArc(arcBounds, -90f, fraction * 360f, false, progress)

        val handAngle = Math.toRadians((fraction * 360f - 90f).toDouble())
        val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
            color = accent
        }
        canvas.drawLine(
            CENTER,
            CENTER,
            CENTER + (150f * Math.cos(handAngle)).toFloat(),
            CENTER + (150f * Math.sin(handAngle)).toFloat(),
            hand,
        )

        val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawCircle(CENTER, CENTER, 9f, hub)
        canvas.drawCircle(CENTER, CENTER, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, if (isDark) 220 else 240)
        })

        return bitmap
    }

    /**
     * The ambient card's quiet backdrop: three offset rings, one arc and a very
     * faint grid. At widget size it reads as texture, not as a diagram.
     */
    fun ambient(accent: Int, isDark: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = withAlpha(accent, if (isDark) 18 else 12)
        }
        var offset = 0f
        while (offset <= SIZE) {
            canvas.drawLine(offset, 0f, offset, SIZE.toFloat(), grid)
            canvas.drawLine(0f, offset, SIZE.toFloat(), offset, grid)
            offset += 96f
        }

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = withAlpha(accent, if (isDark) 34 else 22)
        }
        val centerX = SIZE * 0.82f
        val centerY = SIZE * 0.18f
        listOf(250f, 190f, 130f).forEachIndexed { index, radius ->
            ring.strokeWidth = (3f - index * 0.6f).coerceAtLeast(1.4f)
            canvas.drawCircle(centerX, centerY, radius, ring)
        }

        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            color = withAlpha(accent, if (isDark) 46 else 32)
        }
        val bounds = RectF(-160f, SIZE * 0.56f, 190f, SIZE + 140f)
        canvas.drawArc(bounds, 200f, 120f, false, arc)

        return bitmap
    }

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
}

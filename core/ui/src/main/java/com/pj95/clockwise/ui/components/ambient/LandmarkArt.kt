package com.pj95.clockwise.ui.components.ambient

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import com.pj95.clockwise.model.LandmarkType

/**
 * Hand-drawn vector landmarks for the city detail watermark.
 *
 * Every path is expressed as a fraction of the supplied [Size], so one drawing
 * works on a 360 dp phone and on a tablet without a second asset. Nothing here
 * is a bitmap: the whole set costs a few kilobytes of code and scales cleanly.
 *
 * The silhouettes are intentionally simplified. At 4–6% opacity the job is to
 * give the screen depth and a sense of place, not to be an illustration.
 */
object LandmarkArt {

    /** The path for [type], or `null` when the city uses the ambient fallback. */
    fun path(type: LandmarkType, size: Size): Path? = when (type) {
        LandmarkType.PAGODA -> pagoda(size)
        LandmarkType.TOWER -> tower(size)
        LandmarkType.SKYSCRAPER -> skyscraper(size)
        LandmarkType.CLASSIC_ARCH -> classicArch(size)
        LandmarkType.BRIDGE -> bridge(size)
        LandmarkType.DOME -> dome(size)
        LandmarkType.NONE -> null
    }

    private fun pagoda(size: Size): Path = Path().apply {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        // Base platform.
        moveTo(p(0.12f, 0.94f).x, p(0.12f, 0.94f).y)
        lineTo(p(0.88f, 0.94f).x, p(0.88f, 0.94f).y)
        lineTo(p(0.88f, 0.88f).x, p(0.88f, 0.88f).y)
        lineTo(p(0.12f, 0.88f).x, p(0.12f, 0.88f).y)
        close()

        // Three tiers, each a shallow trapezoid.
        tier(this, size, top = 0.76f, bottom = 0.86f, halfTop = 0.32f, halfBottom = 0.26f)
        tier(this, size, top = 0.62f, bottom = 0.72f, halfTop = 0.24f, halfBottom = 0.19f)
        tier(this, size, top = 0.48f, bottom = 0.58f, halfTop = 0.16f, halfBottom = 0.12f)

        // Spire and finial.
        moveTo(p(0.46f, 0.48f).x, p(0.46f, 0.48f).y)
        lineTo(p(0.54f, 0.48f).x, p(0.54f, 0.48f).y)
        lineTo(p(0.5f, 0.34f).x, p(0.5f, 0.34f).y)
        close()
        moveTo(p(0.485f, 0.34f).x, p(0.485f, 0.34f).y)
        lineTo(p(0.515f, 0.34f).x, p(0.515f, 0.34f).y)
        lineTo(p(0.515f, 0.28f).x, p(0.515f, 0.28f).y)
        lineTo(p(0.485f, 0.28f).x, p(0.485f, 0.28f).y)
        close()
    }

    private fun tier(
        path: Path,
        size: Size,
        top: Float,
        bottom: Float,
        halfTop: Float,
        halfBottom: Float,
    ) {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        path.moveTo(p(0.5f - halfTop, top).x, p(0.5f - halfTop, top).y)
        path.lineTo(p(0.5f + halfTop, top).x, p(0.5f + halfTop, top).y)
        path.lineTo(p(0.5f + halfBottom, bottom).x, p(0.5f + halfBottom, bottom).y)
        path.lineTo(p(0.5f - halfBottom, bottom).x, p(0.5f - halfBottom, bottom).y)
        path.close()
    }

    private fun tower(size: Size): Path = Path().apply {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        // The two legs and the arch between them.
        moveTo(p(0.26f, 0.94f).x, p(0.26f, 0.94f).y)
        lineTo(p(0.4f, 0.46f).x, p(0.4f, 0.46f).y)
        lineTo(p(0.46f, 0.22f).x, p(0.46f, 0.22f).y)
        lineTo(p(0.5f, 0.14f).x, p(0.5f, 0.14f).y)
        lineTo(p(0.54f, 0.22f).x, p(0.54f, 0.22f).y)
        lineTo(p(0.6f, 0.46f).x, p(0.6f, 0.46f).y)
        lineTo(p(0.74f, 0.94f).x, p(0.74f, 0.94f).y)
        lineTo(p(0.64f, 0.94f).x, p(0.64f, 0.94f).y)
        lineTo(p(0.54f, 0.6f).x, p(0.54f, 0.6f).y)
        lineTo(p(0.46f, 0.6f).x, p(0.46f, 0.6f).y)
        lineTo(p(0.36f, 0.94f).x, p(0.36f, 0.94f).y)
        close()

        // Two observation decks.
        deck(this, size, y = 0.58f, halfWidth = 0.13f)
        deck(this, size, y = 0.76f, halfWidth = 0.18f)
    }

    private fun deck(path: Path, size: Size, y: Float, halfWidth: Float) {
        fun p(x: Float, yy: Float) = Offset(size.width * x, size.height * yy)
        path.moveTo(p(0.5f - halfWidth, y).x, p(0.5f - halfWidth, y).y)
        path.lineTo(p(0.5f + halfWidth, y).x, p(0.5f + halfWidth, y).y)
        path.lineTo(p(0.5f + halfWidth, y + 0.025f).x, p(0.5f + halfWidth, y + 0.025f).y)
        path.lineTo(p(0.5f - halfWidth, y + 0.025f).x, p(0.5f - halfWidth, y + 0.025f).y)
        path.close()
    }

    private fun skyscraper(size: Size): Path = Path().apply {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        moveTo(p(0.28f, 0.94f).x, p(0.28f, 0.94f).y)
        lineTo(p(0.72f, 0.94f).x, p(0.72f, 0.94f).y)
        lineTo(p(0.72f, 0.38f).x, p(0.72f, 0.38f).y)
        lineTo(p(0.62f, 0.38f).x, p(0.62f, 0.38f).y)
        lineTo(p(0.62f, 0.24f).x, p(0.62f, 0.24f).y)
        lineTo(p(0.56f, 0.24f).x, p(0.56f, 0.24f).y)
        lineTo(p(0.56f, 0.13f).x, p(0.56f, 0.13f).y)
        lineTo(p(0.44f, 0.13f).x, p(0.44f, 0.13f).y)
        lineTo(p(0.44f, 0.24f).x, p(0.44f, 0.24f).y)
        lineTo(p(0.38f, 0.24f).x, p(0.38f, 0.24f).y)
        lineTo(p(0.38f, 0.38f).x, p(0.38f, 0.38f).y)
        lineTo(p(0.28f, 0.38f).x, p(0.28f, 0.38f).y)
        close()

        // Antenna.
        moveTo(p(0.49f, 0.13f).x, p(0.49f, 0.13f).y)
        lineTo(p(0.51f, 0.13f).x, p(0.51f, 0.13f).y)
        lineTo(p(0.51f, 0.02f).x, p(0.51f, 0.02f).y)
        lineTo(p(0.49f, 0.02f).x, p(0.49f, 0.02f).y)
        close()
    }

    private fun classicArch(size: Size): Path = Path().apply {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        // Pediment.
        moveTo(p(0.18f, 0.46f).x, p(0.18f, 0.46f).y)
        lineTo(p(0.82f, 0.46f).x, p(0.82f, 0.46f).y)
        lineTo(p(0.5f, 0.28f).x, p(0.5f, 0.28f).y)
        close()

        // Entablature.
        moveTo(p(0.2f, 0.46f).x, p(0.2f, 0.46f).y)
        lineTo(p(0.8f, 0.46f).x, p(0.8f, 0.46f).y)
        lineTo(p(0.8f, 0.52f).x, p(0.8f, 0.52f).y)
        lineTo(p(0.2f, 0.52f).x, p(0.2f, 0.52f).y)
        close()

        // Four columns.
        listOf(0.27f, 0.4f, 0.53f, 0.66f).forEach { x ->
            moveTo(p(x, 0.52f).x, p(x, 0.52f).y)
            lineTo(p(x + 0.07f, 0.52f).x, p(x + 0.07f, 0.52f).y)
            lineTo(p(x + 0.07f, 0.88f).x, p(x + 0.07f, 0.88f).y)
            lineTo(p(x, 0.88f).x, p(x, 0.88f).y)
            close()
        }

        // Stylobate.
        moveTo(p(0.18f, 0.88f).x, p(0.18f, 0.88f).y)
        lineTo(p(0.82f, 0.88f).x, p(0.82f, 0.88f).y)
        lineTo(p(0.82f, 0.94f).x, p(0.82f, 0.94f).y)
        lineTo(p(0.18f, 0.94f).x, p(0.18f, 0.94f).y)
        close()
    }

    private fun bridge(size: Size): Path = Path().apply {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        // Deck.
        moveTo(p(0.06f, 0.66f).x, p(0.06f, 0.66f).y)
        lineTo(p(0.94f, 0.66f).x, p(0.94f, 0.66f).y)
        lineTo(p(0.94f, 0.71f).x, p(0.94f, 0.71f).y)
        lineTo(p(0.06f, 0.71f).x, p(0.06f, 0.71f).y)
        close()

        // Two towers.
        listOf(0.28f, 0.64f).forEach { x ->
            moveTo(p(x, 0.34f).x, p(x, 0.34f).y)
            lineTo(p(x + 0.08f, 0.34f).x, p(x + 0.08f, 0.34f).y)
            lineTo(p(x + 0.08f, 0.82f).x, p(x + 0.08f, 0.82f).y)
            lineTo(p(x, 0.82f).x, p(x, 0.82f).y)
            close()
        }

        // The main cable, drawn as a thin filled band.
        moveTo(p(0.06f, 0.46f).x, p(0.06f, 0.46f).y)
        cubicTo(
            p(0.28f, 0.46f).x, p(0.28f, 0.46f).y,
            p(0.38f, 0.64f).x, p(0.38f, 0.64f).y,
            p(0.5f, 0.64f).x, p(0.5f, 0.64f).y,
        )
        cubicTo(
            p(0.62f, 0.64f).x, p(0.62f, 0.64f).y,
            p(0.72f, 0.46f).x, p(0.72f, 0.46f).y,
            p(0.94f, 0.46f).x, p(0.94f, 0.46f).y,
        )
        lineTo(p(0.94f, 0.49f).x, p(0.94f, 0.49f).y)
        cubicTo(
            p(0.72f, 0.49f).x, p(0.72f, 0.49f).y,
            p(0.62f, 0.67f).x, p(0.62f, 0.67f).y,
            p(0.5f, 0.67f).x, p(0.5f, 0.67f).y,
        )
        cubicTo(
            p(0.38f, 0.67f).x, p(0.38f, 0.67f).y,
            p(0.28f, 0.49f).x, p(0.28f, 0.49f).y,
            p(0.06f, 0.49f).x, p(0.06f, 0.49f).y,
        )
        close()
    }

    private fun dome(size: Size): Path = Path().apply {
        fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

        // Drum and dome.
        moveTo(p(0.2f, 0.62f).x, p(0.2f, 0.62f).y)
        cubicTo(
            p(0.2f, 0.26f).x, p(0.2f, 0.26f).y,
            p(0.8f, 0.26f).x, p(0.8f, 0.26f).y,
            p(0.8f, 0.62f).x, p(0.8f, 0.62f).y,
        )
        close()

        // Base.
        moveTo(p(0.16f, 0.62f).x, p(0.16f, 0.62f).y)
        lineTo(p(0.84f, 0.62f).x, p(0.84f, 0.62f).y)
        lineTo(p(0.84f, 0.94f).x, p(0.84f, 0.94f).y)
        lineTo(p(0.16f, 0.94f).x, p(0.16f, 0.94f).y)
        close()

        // Lantern and spire.
        moveTo(p(0.46f, 0.26f).x, p(0.46f, 0.26f).y)
        lineTo(p(0.54f, 0.26f).x, p(0.54f, 0.26f).y)
        lineTo(p(0.54f, 0.18f).x, p(0.54f, 0.18f).y)
        lineTo(p(0.46f, 0.18f).x, p(0.46f, 0.18f).y)
        close()
        moveTo(p(0.48f, 0.18f).x, p(0.48f, 0.18f).y)
        lineTo(p(0.52f, 0.18f).x, p(0.52f, 0.18f).y)
        lineTo(p(0.5f, 0.08f).x, p(0.5f, 0.08f).y)
        close()
    }
}

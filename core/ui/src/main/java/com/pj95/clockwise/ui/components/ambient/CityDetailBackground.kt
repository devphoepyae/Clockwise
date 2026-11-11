package com.pj95.clockwise.ui.components.ambient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.model.LandmarkType
import com.pj95.clockwise.ui.theme.LocalClockwisePalette
import kotlin.math.abs

/**
 * The subtle ambient watermark behind the city detail screen.
 *
 * It is drawn entirely with Compose Canvas paths: no bitmap, no network, no
 * per-city asset. The colour is the current Material You primary at 4% (light)
 * or 6% (dark), low enough that every text token above it keeps its contrast.
 *
 * A city with a [landmarkType] gets a simplified vector silhouette anchored to
 * the lower-right. A city without one — including a future custom offline city
 * — gets concentric rings and an arc instead. The fallback's exact offset is
 * derived from [cityName], so two custom cities do not look identical.
 *
 * The drawing is static. That is deliberate: a slowly pulsing background would
 * force a frame on every tick for no information, and this screen already has a
 * live clock. If motion is added later it belongs in `graphicsLayer` alpha, not
 * in a rebuilt path.
 */
@Composable
fun CityDetailBackground(
    cityName: String,
    landmarkType: LandmarkType?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalClockwisePalette.current
    val primary = MaterialTheme.colorScheme.primary
    val subtleAlpha = if (palette.isDark) 0.06f else 0.04f
    val seed = remember(cityName) { abs(cityName.hashCode()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
            val landmarkSize = Size(size.width * 0.62f, size.height * 0.52f)
            val landmark = landmarkType?.let { type ->
                LandmarkArt.path(type, landmarkSize)?.apply {
                    translate(Offset(size.width * 0.34f, size.height * 0.44f))
                }
            }
            val ringCenter = Offset(
                x = size.width * (0.82f + (seed % 7) * 0.01f),
                y = size.height * 0.14f,
            )
            val arcTopLeft = Offset(
                x = -size.width * 0.24f,
                y = size.height * (0.58f + (seed % 5) * 0.01f),
            )
            val arcSize = Size(size.width * 0.92f, size.width * 0.92f)

            onDrawBehind {
                drawCircle(
                    color = primary.copy(alpha = subtleAlpha),
                    radius = size.width * 0.72f,
                    center = ringCenter,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = primary.copy(alpha = subtleAlpha * 0.55f),
                    radius = size.width * 0.55f,
                    center = ringCenter,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawArc(
                    color = primary.copy(alpha = subtleAlpha * 0.8f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                if (landmark != null) {
                    drawPath(
                        path = landmark,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primary.copy(alpha = subtleAlpha),
                                primary.copy(alpha = 0f),
                            ),
                            startY = size.height,
                            endY = size.height * 0.45f,
                        ),
                    )
                } else {
                    drawFallbackRings(
                        primary = primary,
                        alpha = subtleAlpha,
                        seed = seed,
                    )
                }
            }
        },
    )
}

/**
 * The fallback: three quiet rings and a short arc, offset deterministically by
 * the city name so a directory of custom cities still feels varied.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackRings(
    primary: androidx.compose.ui.graphics.Color,
    alpha: Float,
    seed: Int,
) {
    val center = Offset(
        x = size.width * (0.78f + (seed % 11) * 0.008f),
        y = size.height * (0.34f + (seed % 7) * 0.01f),
    )
    listOf(0.62f, 0.46f, 0.3f).forEachIndexed { index, fraction ->
        drawCircle(
            color = primary.copy(alpha = alpha * (1f - index * 0.22f)),
            radius = size.width * fraction,
            center = center,
            style = Stroke(width = (1.5f - index * 0.25f).dp.toPx()),
        )
    }
    drawArc(
        color = primary.copy(alpha = alpha * 0.7f),
        startAngle = 20f + (seed % 90),
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(size.width * 0.12f, size.height * 0.58f),
        size = Size(size.width * 0.6f, size.width * 0.6f),
        style = Stroke(width = 1.dp.toPx()),
    )
}

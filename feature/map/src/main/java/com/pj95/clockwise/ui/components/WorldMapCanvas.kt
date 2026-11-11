package com.pj95.clockwise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.domain.map.EquirectangularProjection
import com.pj95.clockwise.model.map.GeoPoint
import com.pj95.clockwise.domain.map.GlobePoint
import com.pj95.clockwise.domain.map.GlobeProjection
import com.pj95.clockwise.domain.map.MapProjectionMode
import com.pj95.clockwise.domain.map.SolarPosition
import com.pj95.clockwise.domain.map.SubsolarPoint
import com.pj95.clockwise.domain.map.WorldLandmasses
import com.pj95.clockwise.ui.theme.LocalClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import java.time.Instant
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Degrees between grid lines. */
private const val GRID_STEP_DEGREES = 30.0

/** Samples used to trace the terminator; one every 2° of longitude. */
private const val TERMINATOR_STEPS = 180

/**
 * Test-only tags, kept beside the map so tests never have to match display
 * strings or screen positions.
 */
object WorldMapTestTags {
    fun marker(cityId: String): String = "mapMarker_$cityId"

    fun cityPill(cityId: String): String = "mapCityPill_$cityId"

    fun comparisonCard(cityId: String): String = "mapComparison_$cityId"

    const val SELECTED_CITY_PANEL: String = "mapSelectedCityPanel"

    const val MAP_CANVAS: String = "mapCanvas"

    const val PROJECTION_FLAT: String = "mapProjectionFlat"

    const val PROJECTION_GLOBE: String = "mapProjectionGlobe"

    const val COMPARISON_TRAY: String = "mapComparisonTray"
}

/**
 * The schematic world map (spec section 4.4).
 *
 * Everything is drawn with Compose on device from bundled vectors: landmasses,
 * a latitude/longitude graticule, the live day/night terminator, the active
 * city's meridian, and a UTC offset ruler. There are no map tiles, no map
 * provider and no API key.
 *
 * [projection] chooses between the flat plate and the orthographic globe. The
 * globe turns so the active city faces the viewer, and hides markers on its far
 * side; the flat plate always shows the whole world.
 *
 * City markers are real composables rather than canvas shapes so each one gets a
 * 44 dp touch target, a ripple, and a screen-reader label describing the city and
 * its current time.
 */
@Composable
fun WorldMapCanvas(
    cities: List<CityCardModel>,
    now: Instant,
    onCityClick: (CityCardModel) -> Unit,
    modifier: Modifier = Modifier,
    projection: MapProjectionMode = MapProjectionMode.FLAT,
) {
    val subsolar = remember(now) { SolarPosition.subsolarPoint(now) }
    val textMeasurer = rememberTextMeasurer()
    val activeCity = cities.firstOrNull { it.isActive }
    val centerLongitude = activeCity?.longitude ?: subsolar.longitude
    val centerLatitude = activeCity?.latitude ?: subsolar.latitude
    val mapDescription = stringResource(R.string.a11y_world_map)
    val rulerDescription = stringResource(R.string.a11y_utc_ruler)

    // Draw code is not composable, so the palette is read once here and passed
    // down into each drawing step.
    val palette = LocalClockwisePalette.current
    val rulerTextStyle = remember(palette) {
        ClockwiseTextStyles.MapRulerLabel.copy(color = palette.mutedForeground)
    }
    val markerTextStyle = remember(palette) {
        ClockwiseTextStyles.MapRulerLabel.copy(color = palette.foreground)
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .selectableGroup(),
    ) {
        val width = maxWidth
        val height = maxHeight
        val globeDiameter = min(width.value, height.value).dp

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "$mapDescription. $rulerDescription."
                },
        ) {
            when (projection) {
                MapProjectionMode.FLAT -> {
                    drawOcean(palette)
                    drawGraticule(palette)
                    drawLandmasses(palette)
                    drawNight(subsolar, palette)
                    drawSun(subsolar, palette)
                    drawActiveMeridian(activeCity, palette)
                    drawUtcRuler(textMeasurer, rulerTextStyle, palette)
                    drawCityLabels(cities, markerTextStyle, palette, textMeasurer) { city ->
                        Offset(
                            x = size.width * EquirectangularProjection.xFraction(city.longitude).toFloat(),
                            y = size.height * EquirectangularProjection.yFraction(city.latitude).toFloat(),
                        )
                    }
                }

                MapProjectionMode.GLOBE -> {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) / 2f - 1.dp.toPx()
                    drawGlobe(
                        subsolar = subsolar,
                        activeCity = activeCity,
                        centerLongitude = centerLongitude,
                        centerLatitude = centerLatitude,
                        center = center,
                        radius = radius,
                        palette = palette,
                    )
                    drawCityLabels(cities, markerTextStyle, palette, textMeasurer) { city ->
                        val point = GlobeProjection.project(
                            longitude = city.longitude,
                            latitude = city.latitude,
                            centerLongitude = centerLongitude,
                            centerLatitude = centerLatitude,
                        )
                        if (point.visible) point.toOffset(center, radius) else null
                    }
                }
            }
        }

        cities.forEach { city ->
            val position = when (projection) {
                MapProjectionMode.FLAT -> {
                    width * EquirectangularProjection.xFraction(city.longitude).toFloat() to
                        height * EquirectangularProjection.yFraction(city.latitude).toFloat()
                }

                MapProjectionMode.GLOBE -> {
                    val point = GlobeProjection.project(
                        longitude = city.longitude,
                        latitude = city.latitude,
                        centerLongitude = centerLongitude,
                        centerLatitude = centerLatitude,
                    )
                    if (!point.visible) {
                        null
                    } else {
                        (width / 2f + globeDiameter * (point.xFraction - 0.5f)) to
                            (height / 2f + globeDiameter * (point.yFraction - 0.5f))
                    }
                }
            }
            if (position != null) {
                CityMapMarker(
                    city = city,
                    x = position.first,
                    y = position.second,
                    onClick = { onCityClick(city) },
                )
            }
        }
    }
}

private fun DrawScope.drawOcean(palette: ClockwisePalette) {
    drawRect(color = palette.card)
}

private fun DrawScope.drawGraticule(palette: ClockwisePalette) {
    val color = palette.border
    var longitude = EquirectangularProjection.MIN_LONGITUDE
    while (longitude <= EquirectangularProjection.MAX_LONGITUDE) {
        val x = size.width * EquirectangularProjection.xFraction(longitude).toFloat()
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        longitude += GRID_STEP_DEGREES
    }
    var latitude = -60.0
    while (latitude <= 60.0) {
        val y = size.height * EquirectangularProjection.yFraction(latitude).toFloat()
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        latitude += GRID_STEP_DEGREES
    }
}

private fun DrawScope.drawLandmasses(palette: ClockwisePalette) {
    val color = palette.mapLandmass
    WorldLandmasses.polygons.forEach { outline ->
        val path = Path()
        outline.forEachIndexed { index, point ->
            val x = size.width * EquirectangularProjection.xFraction(point.longitude).toFloat()
            val y = size.height * EquirectangularProjection.yFraction(point.latitude).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = color)
    }
}

/**
 * Fills the half of the globe currently in darkness.
 *
 * The terminator is traced by solving the horizon equation for every longitude,
 * then the path is closed through whichever pole is in shadow.
 */
private fun DrawScope.drawNight(subsolar: SubsolarPoint, palette: ClockwisePalette) {
    val path = Path()
    for (step in 0..TERMINATOR_STEPS) {
        val longitude = EquirectangularProjection.MIN_LONGITUDE +
            (EquirectangularProjection.MAX_LONGITUDE - EquirectangularProjection.MIN_LONGITUDE) *
            step / TERMINATOR_STEPS
        val latitude = SolarPosition.terminatorLatitude(longitude, subsolar)
        val x = size.width * EquirectangularProjection.xFraction(longitude).toFloat()
        val y = size.height * EquirectangularProjection.yFraction(latitude).toFloat()
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (subsolar.latitude >= 0.0) {
        // The Sun is north of the equator, so the southern pole is in shadow.
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
    } else {
        path.lineTo(size.width, 0f)
        path.lineTo(0f, 0f)
    }
    path.close()
    drawPath(path, color = palette.mapNight)
}

/**
 * A small rayed sun at the subsolar point, the one place where the Sun is
 * directly overhead. It makes a time-travel scrub legible: the disc and the
 * terminator move together while the cities stay put.
 */
private fun DrawScope.drawSun(subsolar: SubsolarPoint, palette: ClockwisePalette) {
    val center = Offset(
        x = size.width * EquirectangularProjection.xFraction(subsolar.longitude).toFloat(),
        y = size.height * EquirectangularProjection.yFraction(subsolar.latitude).toFloat(),
    )
    drawSunDisc(center = center, palette = palette)
}

/** The rayed sun itself, shared by the flat plate and the globe. */
private fun DrawScope.drawSunDisc(center: Offset, palette: ClockwisePalette) {
    val radius = 2.5.dp.toPx()
    val rayStart = radius + 1.5.dp.toPx()
    val rayEnd = rayStart + 2.dp.toPx()

    drawCircle(color = palette.card, radius = radius + 1.dp.toPx(), center = center)
    drawCircle(color = palette.mapAccent, radius = radius, center = center)
    repeat(8) { index ->
        val angle = Math.toRadians(index * 45.0)
        val unitX = cos(angle).toFloat()
        val unitY = sin(angle).toFloat()
        drawLine(
            color = palette.mapAccent,
            start = Offset(center.x + unitX * rayStart, center.y + unitY * rayStart),
            end = Offset(center.x + unitX * rayEnd, center.y + unitY * rayEnd),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawActiveMeridian(city: CityCardModel?, palette: ClockwisePalette) {
    if (city == null) return
    val x = size.width * EquirectangularProjection.xFraction(city.longitude).toFloat()
    drawLine(
        color = palette.mapAccent,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.5f,
    )
}

/**
 * UTC offset ruler along the bottom edge: a tick every hour of longitude, with
 * a compact label every two hours.
 */
private fun DrawScope.drawUtcRuler(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    palette: ClockwisePalette,
) {
    val labelTop = size.height - 19.dp.toPx()
    val tickBottom = labelTop - 2.dp.toPx()
    val tickTop = tickBottom - 5.dp.toPx()

    for (offset in -12..14) {
        val longitude = offset * 15.0
        val x = size.width * EquirectangularProjection.xFraction(longitude).toFloat()
        drawLine(palette.border, Offset(x, tickTop), Offset(x, tickBottom), strokeWidth = 1f)
        if (offset % 2 != 0) continue

        val label = when {
            offset > 0 -> "+$offset"
            offset < 0 -> offset.toString()
            else -> "0"
        }
        val layout = textMeasurer.measure(label, textStyle)
        val halfWidth = layout.size.width / 2f
        // Skip labels that would spill over the map edge and collide with the
        // border; the ticks still mark every hour.
        if (x - halfWidth < 2f || x + halfWidth > size.width - 2f) continue

        drawText(
            textLayoutResult = layout,
            topLeft = Offset(x - halfWidth, labelTop),
        )
    }
}

/**
 * The orthographic globe.
 *
 * The disc is clipped first, then the same layers the flat plate draws are
 * re-projected: graticule, land, night shading, sun and the active meridian.
 * Points on the far side are either skipped (lines) or pinned to the rim
 * (landmass rings), which keeps the silhouette closed without a full
 * great-circle clipper.
 */
private fun DrawScope.drawGlobe(
    subsolar: SubsolarPoint,
    activeCity: CityCardModel?,
    centerLongitude: Double,
    centerLatitude: Double,
    center: Offset,
    radius: Float,
    palette: ClockwisePalette,
) {
    if (radius <= 0f) return
    val disc = Path().apply {
        addOval(
            Rect(
                center.x - radius,
                center.y - radius,
                center.x + radius,
                center.y + radius,
            ),
        )
    }
    clipPath(disc) {
        drawRect(palette.card)
        drawGlobeGraticule(centerLongitude, centerLatitude, center, radius, palette)
        drawGlobeLandmasses(centerLongitude, centerLatitude, center, radius, palette)
        drawGlobeNight(subsolar, centerLongitude, centerLatitude, center, radius, palette)
        drawGlobeSun(subsolar, centerLongitude, centerLatitude, center, radius, palette)
        drawGlobeMeridian(activeCity, centerLongitude, centerLatitude, center, radius, palette)
    }
    drawCircle(
        color = palette.border,
        radius = radius,
        center = center,
        style = Stroke(width = 1f),
    )
}

private fun GlobePoint.toOffset(center: Offset, radius: Float): Offset = Offset(
    x = center.x + (xFraction - 0.5f) * radius * 2f,
    y = center.y + (yFraction - 0.5f) * radius * 2f,
)

/** Pins a far-side point to the visible rim, so a continent stays closed. */
private fun GlobePoint.rimOffset(center: Offset, radius: Float): Offset {
    val dx = xFraction - 0.5f
    val dy = yFraction - 0.5f
    val length = sqrt(dx * dx + dy * dy)
    if (length < 0.0001f) return center
    val scale = 0.5f / length
    return Offset(
        x = center.x + dx * scale * radius * 2f,
        y = center.y + dy * scale * radius * 2f,
    )
}

private fun DrawScope.drawGlobeGraticule(
    centerLongitude: Double,
    centerLatitude: Double,
    center: Offset,
    radius: Float,
    palette: ClockwisePalette,
) {
    val color = palette.border

    var longitude = -180.0
    while (longitude < 180.0) {
        val path = Path()
        var started = false
        var latitude = -90.0
        while (latitude <= 90.0) {
            val point = GlobeProjection.project(longitude, latitude, centerLongitude, centerLatitude)
            if (point.visible) {
                val offset = point.toOffset(center, radius)
                if (started) path.lineTo(offset.x, offset.y) else path.moveTo(offset.x, offset.y)
                started = true
            } else {
                started = false
            }
            latitude += 3.0
        }
        drawPath(path, color = color, style = Stroke(width = 1f))
        longitude += GRID_STEP_DEGREES
    }

    var latitude = -60.0
    while (latitude <= 60.0) {
        val path = Path()
        var started = false
        var lon = -180.0
        while (lon <= 180.0) {
            val point = GlobeProjection.project(lon, latitude, centerLongitude, centerLatitude)
            if (point.visible) {
                val offset = point.toOffset(center, radius)
                if (started) path.lineTo(offset.x, offset.y) else path.moveTo(offset.x, offset.y)
                started = true
            } else {
                started = false
            }
            lon += 3.0
        }
        drawPath(path, color = color, style = Stroke(width = 1f))
        latitude += GRID_STEP_DEGREES
    }
}

private fun DrawScope.drawGlobeLandmasses(
    centerLongitude: Double,
    centerLatitude: Double,
    center: Offset,
    radius: Float,
    palette: ClockwisePalette,
) {
    WorldLandmasses.polygons.forEach { outline ->
        val path = Path()
        outline.forEachIndexed { index, point: GeoPoint ->
            val projected = GlobeProjection.project(
                longitude = point.longitude,
                latitude = point.latitude,
                centerLongitude = centerLongitude,
                centerLatitude = centerLatitude,
            )
            val offset = if (projected.visible) {
                projected.toOffset(center, radius)
            } else {
                projected.rimOffset(center, radius)
            }
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        path.close()
        drawPath(path, color = palette.mapLandmass)
    }
}

private fun DrawScope.drawGlobeNight(
    subsolar: SubsolarPoint,
    centerLongitude: Double,
    centerLatitude: Double,
    center: Offset,
    radius: Float,
    palette: ClockwisePalette,
) {
    val sun = GlobeProjection.project(
        longitude = subsolar.longitude,
        latitude = subsolar.latitude,
        centerLongitude = centerLongitude,
        centerLatitude = centerLatitude,
    )
    if (sun.visible) {
        // The night is a soft falloff away from the sun. A radial wash reads as
        // a lit sphere and avoids the flat plate's hard terminator, which would
        // look like a crease once the globe has curvature.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.mapNight.copy(alpha = 0f), palette.mapNight),
                center = sun.toOffset(center, radius),
                radius = radius * 1.9f,
            ),
            radius = radius,
            center = center,
        )
    } else {
        // The visible hemisphere is entirely in shadow.
        drawCircle(color = palette.mapNight, radius = radius, center = center)
    }
}

private fun DrawScope.drawGlobeSun(
    subsolar: SubsolarPoint,
    centerLongitude: Double,
    centerLatitude: Double,
    center: Offset,
    radius: Float,
    palette: ClockwisePalette,
) {
    val sun = GlobeProjection.project(
        longitude = subsolar.longitude,
        latitude = subsolar.latitude,
        centerLongitude = centerLongitude,
        centerLatitude = centerLatitude,
    )
    if (sun.visible) drawSunDisc(center = sun.toOffset(center, radius), palette = palette)
}

private fun DrawScope.drawGlobeMeridian(
    city: CityCardModel?,
    centerLongitude: Double,
    centerLatitude: Double,
    center: Offset,
    radius: Float,
    palette: ClockwisePalette,
) {
    if (city == null) return
    val path = Path()
    var started = false
    var latitude = -90.0
    while (latitude <= 90.0) {
        val point = GlobeProjection.project(
            longitude = city.longitude,
            latitude = latitude,
            centerLongitude = centerLongitude,
            centerLatitude = centerLatitude,
        )
        if (point.visible) {
            val offset = point.toOffset(center, radius)
            if (started) path.lineTo(offset.x, offset.y) else path.moveTo(offset.x, offset.y)
            started = true
        } else {
            started = false
        }
        latitude += 2.0
    }
    drawPath(path, color = palette.mapAccent, style = Stroke(width = 1.5f))
}

/**
 * Small name chips under every visible marker. Drawn on the canvas rather than
 * as composables so they do not intercept taps or double the accessibility tree;
 * the marker above them still owns the 44 dp target and the spoken label.
 */
private fun DrawScope.drawCityLabels(
    cities: List<CityCardModel>,
    textStyle: TextStyle,
    palette: ClockwisePalette,
    textMeasurer: TextMeasurer,
    position: (CityCardModel) -> Offset?,
) {
    val padX = 4.dp.toPx()
    val padY = 1.5.dp.toPx()
    val gap = 9.dp.toPx()

    cities.forEach { city ->
        val anchor = position(city) ?: return@forEach
        val layout = textMeasurer.measure(city.displayName.uppercase(), textStyle)
        val chipWidth = layout.size.width + padX * 2f
        val chipHeight = layout.size.height + padY * 2f
        val maxLeft = (size.width - chipWidth - 2f).coerceAtLeast(2f)
        val maxTop = (size.height - chipHeight - 2f).coerceAtLeast(2f)
        val left = (anchor.x - chipWidth / 2f).coerceIn(2f, maxLeft)
        val top = (anchor.y + gap).coerceIn(2f, maxTop)

        drawRoundRect(
            color = palette.card.copy(alpha = 0.92f),
            topLeft = Offset(left, top),
            size = Size(chipWidth, chipHeight),
            cornerRadius = CornerRadius(chipHeight / 2f),
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(left + padX, top + padY),
        )
    }
}

/**
 * One city marker: a small dot for saved cities and a red diamond for the active
 * one. The composable is 44 dp so it is comfortably tappable.
 */
@Composable
private fun CityMapMarker(
    city: CityCardModel,
    x: Dp,
    y: Dp,
    onClick: () -> Unit,
) {
    val tapSize = 44.dp
    val description = if (city.isActive) {
        stringResource(
            R.string.a11y_map_marker_selected,
            city.name,
            city.timeText,
            city.offsetLabel,
        )
    } else {
        stringResource(R.string.a11y_map_marker, city.name, city.timeText, city.offsetLabel)
    }

    Box(
        modifier = Modifier
            .offset(x = x - tapSize / 2, y = y - tapSize / 2)
            .size(tapSize)
            .testTag(WorldMapTestTags.marker(city.id))
            .clip(CircleShape)
            .selectable(
                selected = city.isActive,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (city.isActive) {
            // A faint ring around the diamond: at globe scale a 14 dp diamond
            // alone is easy to lose against the landmass fill.
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(1.dp, ClockwiseColors.MapAccent.copy(alpha = 0.35f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .rotate(45f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ClockwiseColors.MapAccent)
                    .border(1.5.dp, ClockwiseColors.Card, RoundedCornerShape(2.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(ClockwiseColors.Card)
                    .border(1.dp, ClockwiseColors.Foreground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(ClockwiseColors.Foreground),
                )
            }
        }
    }
}

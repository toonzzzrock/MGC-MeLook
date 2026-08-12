package com.example.mgc_keyboard.dashboard.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

/** Axis furniture shared by [LineChart] and [BarChart]: measured y gutter, gridlines and
 * x-label thinning. Both charts used to hardcode a 30dp gutter and draw every x label, which
 * is what smeared the weekday names together and pushed axis text across the plot. */

private const val TICK_COUNT = 3

/** Bottom, middle, top. Three is enough to read a value off; more clutters a 190dp plot. */
internal fun axisTickValues(maxValue: Float): List<Float> =
    List(TICK_COUNT) { i -> maxValue * i / (TICK_COUNT - 1) }

/** Width the widest tick label actually needs, so long labels no longer bleed into the plot. */
internal fun gutterWidthPx(
    ticks: List<Float>,
    formatter: (Float) -> String,
    textSizePx: Float,
    padPx: Float
): Float {
    val paint = axisTextPaint(Color.Black, textSizePx)
    return (ticks.maxOfOrNull { paint.measureText(formatter(it)) } ?: 0f) + padPx
}

/**
 * Evenly spaced subset of x-label slots that fit side by side without touching.
 * First and last are always kept so the range stays readable.
 */
internal fun visibleLabelIndices(count: Int, slotWidthPx: Float, labelWidthPx: Float): Set<Int> {
    if (count <= 0) return emptySet()
    if (count == 1) return setOf(0)
    val step = ((labelWidthPx / slotWidthPx.coerceAtLeast(1f)).toInt() + 1).coerceAtLeast(1)
    val last = count - 1
    val kept = (0..last step step).toMutableSet()
    // Keeping the last slot can crowd whatever step landed just before it — drop those.
    kept.removeAll(((last - step + 1) until last).toSet())
    kept += last
    return kept
}

/** Horizontal gridlines with right-aligned tick labels in the gutter left of [plotLeft]. */
internal fun DrawScope.drawGridAndTicks(
    ticks: List<Float>,
    formatter: (Float) -> String,
    plotLeft: Float,
    plotTop: Float,
    plotBottom: Float,
    labelColor: Color,
    textSizePx: Float,
    padPx: Float
) {
    if (ticks.size < 2) return
    val paint = axisTextPaint(labelColor, textSizePx)
        .apply { textAlign = android.graphics.Paint.Align.RIGHT }
    val plotHeight = plotBottom - plotTop
    ticks.forEachIndexed { i, tick ->
        val y = plotBottom - plotHeight * i / (ticks.size - 1)
        drawLine(
            color = labelColor.copy(alpha = 0.18f),
            start = Offset(plotLeft, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        val baseline = (y + textSizePx * 0.35f).coerceIn(plotTop + textSizePx, plotBottom)
        drawContext.canvas.nativeCanvas.drawText(formatter(tick), plotLeft - padPx / 2f, baseline, paint)
    }
}

package com.example.mgc_keyboard.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

data class ChartPoint(
    val value: Float,
    val label: String = "",
    /** Day number within the charted window. Set it and the point is placed on a calendar axis,
     * so a week with nothing recorded reads as a week-wide gap instead of one step. */
    val dayOffset: Int? = null
)

/** Fraction of the plot width each point sits at: calendar position when the series carries day
 * numbers, even steps otherwise. Draw and hit-test both go through this so they cannot disagree. */
internal fun xFractions(points: List<ChartPoint>): List<Float> {
    val even = points.indices.map { if (points.size < 2) 0f else it / (points.size - 1f) }
    val days = points.map { it.dayOffset ?: return even }
    val span = (days.last() - days.first()).toFloat()
    if (span <= 0f) return even
    return days.map { (it - days.first()) / span }
}

/** Keeps the first label, then every label at least [minGapPx] past the last one kept, and always
 * the last. Two labels never overlap however unevenly the points are spread. */
internal fun spacedLabelIndices(xs: List<Float>, minGapPx: Float): Set<Int> {
    if (xs.isEmpty()) return emptySet()
    val kept = mutableListOf(0)
    for (i in 1 until xs.size) {
        if (xs[i] - xs[kept.last()] >= minGapPx) kept += i
    }
    // The last point owns its label; drop whatever it would have collided with.
    while (kept.size > 1 && xs.last() - xs[kept.last()] < minGapPx) kept.removeAt(kept.lastIndex)
    kept += xs.lastIndex
    return kept.toSet()
}

/** Point nearest the touch. Slot arithmetic would miss once points sit on a calendar axis, where
 * a day after a long gap owns far more width than its neighbours. */
internal fun nearestIndex(x: Float, totalWidth: Float, leftMargin: Float, points: List<ChartPoint>): Int {
    if (points.isEmpty()) return 0
    val plotWidth = (totalWidth - leftMargin).coerceAtLeast(1f)
    val relative = ((x - leftMargin) / plotWidth).coerceIn(0f, 1f)
    val fractions = xFractions(points)
    return fractions.indices.minByOrNull { kotlin.math.abs(fractions[it] - relative) } ?: 0
}

/** True where a segment may be drawn: consecutive recorded days only. A gap in the data is left
 * as a gap rather than bridged by a line that implies days nobody recorded. */
private fun connected(points: List<ChartPoint>, i: Int): Boolean {
    val a = points[i].dayOffset ?: return true
    val b = points[i + 1].dayOffset ?: return true
    return b - a == 1
}

@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MelookColors.Accent,
    axisLabelColor: Color = MelookColors.TextGray,
    /** Top of the value axis. Sentiment and other 0..1 scores keep the default. */
    maxValue: Float = 1f,
    /** The user's usual range in value units, shaded behind the line. */
    band: ClosedFloatingPointRange<Float>? = null,
    valueFormatter: (Float) -> String = { it.formatAxisValue() },
    /** Axis ticks need to be short; the tooltip can afford prose. Defaults to the same text. */
    axisFormatter: (Float) -> String = valueFormatter
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current
    val axisLabelSizePx = with(density) { 10.sp.toPx() }
    val xLabelHeightPx = with(density) { 18.dp.toPx() }
    val tooltipAreaPx = with(density) { 26.dp.toPx() }
    val gutterPadPx = with(density) { 8.dp.toPx() }
    val ticks = remember(maxValue) { axisTickValues(maxValue) }
    // Measured, not a 30dp guess: the hit-testing below has to use the same left margin the
    // draw pass does, so it is computed once here rather than inside the Canvas.
    val yAxisWidthPx = remember(axisFormatter, axisLabelSizePx) {
        gutterWidthPx(ticks, axisFormatter, axisLabelSizePx, gutterPadPx)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(points) {
                if (points.size < 2) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pointerId = down.id
                    selectedIndex = nearestIndex(down.position.x, size.width.toFloat(), yAxisWidthPx, points)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        selectedIndex = nearestIndex(change.position.x, size.width.toFloat(), yAxisWidthPx, points)
                        change.consume()
                    }
                    // Lift the finger and the reading goes with it, so no chart is left
                    // wearing a tooltip from a tap the user has forgotten about.
                    selectedIndex = null
                }
            }
    ) {
        if (points.size < 2) return@Canvas

        val plotLeft = yAxisWidthPx
        val plotRight = this.size.width
        val plotWidth = plotRight - plotLeft
        val plotTop = tooltipAreaPx
        val plotBottom = this.size.height - xLabelHeightPx
        val plotHeight = plotBottom - plotTop

        val stepX = plotWidth / (points.size - 1)
        val fractions = xFractions(points)
        val scale = if (maxValue > 0f) maxValue else 1f
        fun yFor(value: Float) = plotBottom - plotHeight * (value / scale).coerceIn(0f, 1f)
        val offsets = points.mapIndexed { i, p ->
            Offset(plotLeft + plotWidth * fractions[i], yFor(p.value))
        }

        band?.let {
            val top = yFor(it.endInclusive)
            drawRect(
                color = MelookColors.SeriesNeutral.copy(alpha = 0.16f),
                topLeft = Offset(plotLeft, top),
                size = androidx.compose.ui.geometry.Size(plotWidth, yFor(it.start) - top)
            )
        }

        drawGridAndTicks(
            ticks = ticks,
            formatter = axisFormatter,
            plotLeft = plotLeft,
            plotTop = plotTop,
            plotBottom = plotBottom,
            labelColor = axisLabelColor,
            textSizePx = axisLabelSizePx,
            padPx = gutterPadPx
        )

        // Fill and stroke run per unbroken stretch of days, so a gap stays empty in both.
        var runStart = 0
        val runs = mutableListOf<IntRange>()
        for (i in 0 until points.size - 1) {
            if (!connected(points, i)) {
                runs += runStart..i
                runStart = i + 1
            }
        }
        runs += runStart..points.lastIndex

        runs.filter { it.count() > 1 }.forEach { run ->
            val runOffsets = offsets.slice(run)
            val fill = Path().apply {
                moveTo(runOffsets.first().x, plotBottom)
                runOffsets.forEach { lineTo(it.x, it.y) }
                lineTo(runOffsets.last().x, plotBottom)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                    startY = plotTop,
                    endY = plotBottom
                )
            )
            for (i in 0 until runOffsets.size - 1) {
                drawLine(
                    color = lineColor,
                    start = runOffsets[i],
                    end = runOffsets[i + 1],
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }
        // A line of identical dots hides the days worth looking at. Days outside the user's usual
        // range are marked, and the most recent day is ringed so "where am I now" needs no counting.
        offsets.forEachIndexed { i, p ->
            val isSelected = selectedIndex == i
            val isLatest = i == offsets.lastIndex
            val outside = band != null && points[i].value !in band
            val dotColor = if (outside) MelookColors.SeriesFlagged else lineColor
            if (isLatest) {
                drawCircle(color = MelookColors.Surface, radius = 11f, center = p)
                drawCircle(color = dotColor, radius = 11f, center = p, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            }
            drawCircle(color = dotColor, radius = if (isSelected) 9f else if (outside || isLatest) 7f else 5f, center = p)
        }

        val labelPaint = axisTextPaint(axisLabelColor, axisLabelSizePx)
        val widestLabel = points.maxOf { labelPaint.measureText(it.label) }
        // Thinned by where the labels actually land, not by slot count: on a calendar axis a
        // fortnight of daily points and a lone day after a gap sit at very different spacings.
        val visible = spacedLabelIndices(offsets.map { it.x }, widestLabel * 1.2f)
        points.forEachIndexed { i, p ->
            if (p.label.isNotEmpty() && i in visible) {
                drawContext.canvas.nativeCanvas.drawText(
                    p.label,
                    offsets[i].x.coerceIn(plotLeft, this.size.width - widestLabel / 2f),
                    this.size.height - 4f,
                    labelPaint
                )
            }
        }

        selectedIndex?.let { i ->
            val point = points.getOrNull(i) ?: return@let
            val anchor = offsets.getOrNull(i) ?: return@let
            val tooltipText = if (point.label.isNotEmpty()) "${point.label}: ${valueFormatter(point.value)}" else valueFormatter(point.value)
            drawLineChartTooltip(
                text = tooltipText,
                anchorX = anchor.x,
                anchorY = anchor.y,
                canvasWidth = this.size.width
            )
        }
    }
}

/** Sentiment scores are a unitless 0..1 scale — a bare "0.62" tells the user nothing about
 * whether that's good or bad, so axis and tooltip both go through this instead. */
fun sentimentLabel(value: Float): String = when {
    value <= 0.05f -> "negative"
    value >= 0.95f -> "positive"
    value < 0.4f -> "leaning negative (%.2f)".format(value)
    value > 0.6f -> "leaning positive (%.2f)".format(value)
    else -> "neutral (%.2f)".format(value)
}

/** Same scale as [sentimentLabel], but short enough to live in an axis gutter. */
fun sentimentAxisLabel(value: Float): String = when {
    value <= 0.05f -> "neg"
    value >= 0.95f -> "pos"
    else -> "%.1f".format(value)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineChartTooltip(
    text: String,
    anchorX: Float,
    anchorY: Float,
    canvasWidth: Float
) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 11.sp.toPx()
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    val textWidth = paint.measureText(text)
    val paddingH = 10.dp.toPx()
    val paddingV = 6.dp.toPx()
    val boxWidth = textWidth + paddingH * 2
    val boxHeight = paint.textSize + paddingV * 2
    val boxLeft = (anchorX - boxWidth / 2f).coerceIn(0f, (canvasWidth - boxWidth).coerceAtLeast(0f))
    val boxTop = (anchorY - boxHeight - 10.dp.toPx()).coerceAtLeast(0f)

    drawRoundRect(
        color = Color(0xFF2A2E3B),
        topLeft = Offset(boxLeft, boxTop),
        size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawContext.canvas.nativeCanvas.drawText(
        text,
        boxLeft + boxWidth / 2f,
        boxTop + boxHeight / 2f + paint.textSize * 0.35f,
        paint
    )
}

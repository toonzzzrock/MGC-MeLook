package com.example.mgc_keyboard.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class HeatmapDay(
    val dayEpoch: Long,
    val value: Float,
    val label: String = ""
)

/** Weeks the grid spans, so callers can caption it with the range that exists rather than
 * the range that was requested. */
fun heatmapWeekSpan(days: List<HeatmapDay>): Int {
    if (days.isEmpty()) return 0
    val min = days.minOf { it.dayEpoch }
    val max = days.maxOf { it.dayEpoch }
    return ((max - min + LocalDate.ofEpochDay(min).dayOfWeek.value % 7 + 7) / 7).toInt()
}

/**
 * GitHub-contribution-style calendar grid: columns are weeks (oldest to newest, left to
 * right), rows are Sun..Sat. [days] need not cover every date — missing dates render as
 * empty (zero-intensity) cells so the grid stays a clean rectangle.
 *
 * Cells are sized from the available width and the grid height follows, so the grid always
 * fills the card instead of being capped by a fixed canvas height.
 */
@Composable
fun GithubHeatmap(
    days: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    baseColor: Color = MelookColors.Accent,
    emptyColor: Color = MelookColors.Divider,
    axisLabelColor: Color = MelookColors.TextGray
) {
    var selectedEpoch by remember { mutableStateOf<Long?>(null) }

    val density = LocalDensity.current
    val axisLabelSizePx = with(density) { 10.sp.toPx() }
    val monthLabelHeightPx = with(density) { 16.dp.toPx() }
    val legendHeightPx = with(density) { 22.dp.toPx() }
    val cellGapPx = with(density) { 3.dp.toPx() }
    val maxCellPx = with(density) { 26.dp.toPx() }

    if (days.isEmpty()) return

    val byEpoch = days.associateBy { it.dayEpoch }
    val minEpoch = days.minOf { it.dayEpoch }
    val maxEpoch = days.maxOf { it.dayEpoch }
    val firstDate = LocalDate.ofEpochDay(minEpoch)
    // Align the grid so column 0's row 0 is the Sunday on/before the first day, matching
    // GitHub's own week alignment regardless of what weekday the data happens to start on.
    val startOffset = firstDate.dayOfWeek.value % 7 // Mon=1..Sun=7 -> Sun=0
    val gridStartEpoch = minEpoch - startOffset
    val totalDays = (maxEpoch - gridStartEpoch + 1).toInt()
    val weekCount = ((totalDays + 6) / 7).coerceAtLeast(1)

    val maxValue = days.maxOf { it.value }.let { if (it <= 0f) 1f else it }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        // Row labels only need every other weekday; "Wed" is the widest of the three.
        val rowLabelPaint = axisTextPaint(axisLabelColor, axisLabelSizePx)
            .apply { textAlign = android.graphics.Paint.Align.RIGHT }
        val gutterPx = rowLabelPaint.measureText("Wed") + with(density) { 6.dp.toPx() }
        // Square cells sized to fill the card width; the cap keeps a short history from
        // rendering as a handful of oversized blocks.
        val cellPx = ((widthPx - gutterPx - cellGapPx * (weekCount - 1)) / weekCount)
            .coerceIn(1f, maxCellPx)
        val stridePx = cellPx + cellGapPx
        val canvasHeight = with(density) {
            (monthLabelHeightPx + 7 * stridePx + legendHeightPx).toDp()
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(days, cellPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pointerId = down.id
                        fun pick(position: Offset) {
                            selectedEpoch = epochForPosition(
                                position, weekCount, gridStartEpoch, monthLabelHeightPx, gutterPx, stridePx
                            ).takeIf { it <= maxEpoch }
                        }
                        pick(down.position)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            pick(change.position)
                            change.consume()
                        }
                    }
                }
        ) {
            val plotTop = monthLabelHeightPx

            var lastMonth = -1
            for (week in 0 until weekCount) {
                for (row in 0 until 7) {
                    val epoch = gridStartEpoch + week * 7 + row
                    val x = gutterPx + week * stridePx
                    val y = plotTop + row * stridePx

                    if (epoch > maxEpoch) {
                        // Future day within the current (incomplete) week — dashed outline so the
                        // grid reads as a complete rectangle instead of trailing off mid-week.
                        drawRoundRect(
                            color = emptyColor,
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                            cornerRadius = CornerRadius(3f, 3f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        )
                        continue
                    }

                    val day = byEpoch[epoch]
                    val fraction = if (day != null) (day.value / maxValue).coerceIn(0f, 1f) else 0f
                    val color = if (day == null || day.value <= 0f) emptyColor
                                else baseColor.copy(alpha = 0.25f + fraction * 0.75f)
                    val isSelected = selectedEpoch == epoch
                    drawRoundRect(
                        color = if (isSelected) baseColor else color,
                        topLeft = Offset(x, y),
                        size = Size(cellPx, cellPx),
                        cornerRadius = CornerRadius(3f, 3f)
                    )

                    val month = LocalDate.ofEpochDay(epoch).monthValue
                    if (row == 0 && month != lastMonth) {
                        lastMonth = month
                        drawContext.canvas.nativeCanvas.drawText(
                            LocalDate.ofEpochDay(epoch).month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                            x,
                            monthLabelHeightPx - 4f,
                            axisTextPaint(axisLabelColor, axisLabelSizePx)
                                .apply { textAlign = android.graphics.Paint.Align.LEFT }
                        )
                    }
                }
            }

            // Row labels: Mon/Wed/Fri only, same convention as GitHub.
            listOf(1 to "Mon", 3 to "Wed", 5 to "Fri").forEach { (row, text) ->
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    gutterPx - 6f,
                    plotTop + row * stridePx + cellPx * 0.75f,
                    rowLabelPaint
                )
            }

            // Legend: less -> more, so the colour ramp is readable without a tap.
            val legendTop = plotTop + 7 * stridePx + cellGapPx * 2
            val swatch = (cellPx * 0.7f).coerceAtMost(with(density) { 12.dp.toPx() })
            val legendPaint = axisTextPaint(axisLabelColor, axisLabelSizePx)
                .apply { textAlign = android.graphics.Paint.Align.LEFT }
            val lessWidth = legendPaint.measureText("less")
            drawContext.canvas.nativeCanvas.drawText("less", gutterPx, legendTop + swatch * 0.85f, legendPaint)
            listOf(0f, 0.33f, 0.66f, 1f).forEachIndexed { i, fraction ->
                drawRoundRect(
                    color = if (fraction == 0f) emptyColor else baseColor.copy(alpha = 0.25f + fraction * 0.75f),
                    topLeft = Offset(gutterPx + lessWidth + 6f + i * (swatch + 3f), legendTop),
                    size = Size(swatch, swatch),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            drawContext.canvas.nativeCanvas.drawText(
                "more",
                gutterPx + lessWidth + 12f + 4 * (swatch + 3f),
                legendTop + swatch * 0.85f,
                legendPaint
            )

            selectedEpoch?.let { epoch ->
                val day = byEpoch[epoch]
                val date = LocalDate.ofEpochDay(epoch)
                val dateLabel = "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.dayOfMonth}"
                val text = if (day != null) "$dateLabel: ${day.value.formatAxisValue()}" else "$dateLabel: 0"
                val week = ((epoch - gridStartEpoch) / 7).toInt()
                val row = ((epoch - gridStartEpoch) % 7).toInt()
                drawHeatmapTooltip(
                    text = text,
                    anchorX = gutterPx + week * stridePx + cellPx / 2f,
                    anchorY = plotTop + row * stridePx,
                    canvasWidth = this.size.width
                )
            }
        }
    }
}

/** Mirrors the draw pass exactly — it used to recompute cell width differently, which put the
 * touch targets a column off from what was drawn. */
private fun epochForPosition(
    position: Offset,
    weekCount: Int,
    gridStartEpoch: Long,
    plotTop: Float,
    gutterPx: Float,
    stridePx: Float
): Long {
    val week = ((position.x - gutterPx) / stridePx).toInt().coerceIn(0, weekCount - 1)
    val row = ((position.y - plotTop) / stridePx).toInt().coerceIn(0, 6)
    return gridStartEpoch + week * 7 + row
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeatmapTooltip(
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
    val boxTop = (anchorY - boxHeight - 8.dp.toPx()).coerceAtLeast(0f)

    drawRoundRect(
        color = Color(0xFF2A2E3B),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawContext.canvas.nativeCanvas.drawText(
        text,
        boxLeft + boxWidth / 2f,
        boxTop + boxHeight / 2f + paint.textSize * 0.35f,
        paint
    )
}

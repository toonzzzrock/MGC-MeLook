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
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class HeatmapDay(
    val dayEpoch: Long,
    val value: Float,
    val label: String = ""
)

/**
 * GitHub-contribution-style calendar grid: columns are weeks (oldest to newest, left to
 * right), rows are Sun..Sat. [days] need not cover every date — missing dates render as
 * empty (zero-intensity) cells so the grid stays a clean rectangle.
 */
@Composable
fun GithubHeatmap(
    days: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF3B6FF2),
    emptyColor: Color = Color(0xFFE4E7F0),
    axisLabelColor: Color = Color(0xFF8A8FA3)
) {
    var selectedEpoch by remember { mutableStateOf<Long?>(null) }

    val density = LocalDensity.current
    val axisLabelSizePx = with(density) { 10.sp.toPx() }
    val monthLabelHeightPx = with(density) { 16.dp.toPx() }
    val tooltipAreaPx = with(density) { 26.dp.toPx() }
    val cellGapPx = with(density) { 3.dp.toPx() }

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
    val weekCount = (totalDays + 6) / 7

    val maxValue = days.maxOf { it.value }.let { if (it <= 0f) 1f else it }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(days) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pointerId = down.id
                    selectedEpoch = epochForPosition(down.position, size, weekCount, gridStartEpoch, monthLabelHeightPx, cellGapPx)
                        .takeIf { it <= maxEpoch }
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        selectedEpoch = epochForPosition(change.position, size, weekCount, gridStartEpoch, monthLabelHeightPx, cellGapPx)
                            .takeIf { it <= maxEpoch }
                        change.consume()
                    }
                }
            }
    ) {
        val plotTop = monthLabelHeightPx
        val plotBottom = this.size.height
        val plotHeight = plotBottom - plotTop
        val cellSize = ((plotHeight - cellGapPx * 6) / 7).coerceAtLeast(1f)
        val cellStride = cellSize + cellGapPx
        val gridWidth = weekCount * cellStride - cellGapPx
        val cellWidth = ((this.size.width - cellGapPx * (weekCount - 1)) / weekCount).coerceAtLeast(1f).coerceAtMost(cellSize)

        var lastMonth = -1
        for (week in 0 until weekCount) {
            for (row in 0 until 7) {
                val epoch = gridStartEpoch + week * 7 + row
                val x = week * (cellWidth + cellGapPx)
                val y = plotTop + row * cellStride

                if (epoch > maxEpoch) {
                    // Future day within the current (incomplete) week — draw a dashed outline
                    // placeholder so the grid reads as a complete rectangle instead of trailing
                    // off mid-week.
                    drawRoundRect(
                        color = emptyColor,
                        topLeft = Offset(x, y),
                        size = Size(cellWidth, cellSize),
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
                val color = if (day == null || day.value <= 0f) emptyColor else baseColor.copy(alpha = 0.25f + fraction * 0.75f)
                val isSelected = selectedEpoch == epoch
                drawRoundRect(
                    color = if (isSelected) baseColor else color,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth, cellSize),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                val month = LocalDate.ofEpochDay(epoch).monthValue
                if (row == 0 && month != lastMonth) {
                    lastMonth = month
                    drawContext.canvas.nativeCanvas.drawText(
                        LocalDate.ofEpochDay(epoch).month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        x,
                        monthLabelHeightPx - 4f,
                        axisTextPaint(axisLabelColor, axisLabelSizePx).apply { textAlign = android.graphics.Paint.Align.LEFT }
                    )
                }
            }
        }

        selectedEpoch?.let { epoch ->
            val day = byEpoch[epoch]
            val date = LocalDate.ofEpochDay(epoch)
            val dateLabel = "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.dayOfMonth}"
            val text = if (day != null) "$dateLabel: ${day.value.formatAxisValue()}" else "$dateLabel: 0"
            val week = ((epoch - gridStartEpoch) / 7).toInt()
            val row = ((epoch - gridStartEpoch) % 7).toInt()
            val anchorX = week * (cellWidth + cellGapPx) + cellWidth / 2f
            val anchorY = plotTop + row * cellStride
            drawHeatmapTooltip(text = text, anchorX = anchorX, anchorY = anchorY, canvasWidth = this.size.width)
        }
    }
}

private fun epochForPosition(
    position: Offset,
    canvasSize: androidx.compose.ui.unit.IntSize,
    weekCount: Int,
    gridStartEpoch: Long,
    plotTop: Float,
    cellGapPx: Float
): Long {
    val plotHeight = canvasSize.height - plotTop
    val cellSize = ((plotHeight - cellGapPx * 6) / 7).coerceAtLeast(1f)
    val cellStride = cellSize + cellGapPx
    val cellWidth = ((canvasSize.width - cellGapPx * (weekCount - 1)) / weekCount).coerceAtLeast(1f)
    val week = (position.x / (cellWidth + cellGapPx)).toInt().coerceIn(0, weekCount - 1)
    val row = ((position.y - plotTop) / cellStride).toInt().coerceIn(0, 6)
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

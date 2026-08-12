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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

data class Bar(
    val heightFraction: Float,
    val color: Color,
    val label: String = "",
    val value: Float = heightFraction
)

@Composable
fun BarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    axisLabelColor: Color = MelookColors.TextGray,
    valueFormatter: (Float) -> String = { it.formatAxisValue() },
    /** Axis ticks need to be short; the tooltip can afford prose. Defaults to the same text. */
    axisFormatter: (Float) -> String = valueFormatter
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val axisLabelSizePx = with(density) { 10.sp.toPx() }
    val xLabelHeightPx = with(density) { 18.dp.toPx() }
    val tooltipAreaPx = with(density) { 26.dp.toPx() }
    val gutterPadPx = with(density) { 8.dp.toPx() }
    val maxValue = bars.maxOfOrNull { it.value }.let { if (it == null || it <= 0f) 1f else it }
    val ticks = axisTickValues(maxValue)
    // Measured so the widest tick label fits; hit-testing below shares this left margin.
    val yAxisWidthPx = gutterWidthPx(ticks, axisFormatter, axisLabelSizePx, gutterPadPx)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(bars) {
                if (bars.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pointerId = down.id
                    selectedIndex = indexForX(down.position.x, size.width.toFloat(), yAxisWidthPx, bars.size)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        selectedIndex = indexForX(change.position.x, size.width.toFloat(), yAxisWidthPx, bars.size)
                        change.consume()
                    }
                }
            }
    ) {
        if (bars.isEmpty()) return@Canvas

        val plotLeft = yAxisWidthPx
        val plotRight = this.size.width
        val plotWidth = plotRight - plotLeft
        val plotTop = tooltipAreaPx
        val plotBottom = this.size.height - xLabelHeightPx
        val plotHeight = plotBottom - plotTop

        val barCount = bars.size
        val gap = plotWidth * 0.03f
        val barWidth = (plotWidth - gap * (barCount - 1)) / barCount

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

        val labelPaint = axisTextPaint(axisLabelColor, axisLabelSizePx)
        val widestLabel = bars.maxOf { labelPaint.measureText(it.label) }
        val visible = visibleLabelIndices(barCount, barWidth + gap, widestLabel)

        bars.forEachIndexed { i, bar ->
            val barHeight = plotHeight * bar.heightFraction
            val x = plotLeft + i * (barWidth + gap)
            val isSelected = selectedIndex == i
            drawRoundRect(
                color = if (isSelected) bar.color else bar.color.copy(alpha = 0.85f),
                topLeft = Offset(x, plotBottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            if (bar.label.isNotEmpty() && i in visible) {
                drawContext.canvas.nativeCanvas.drawText(
                    bar.label,
                    (x + barWidth / 2f).coerceAtMost(this.size.width - widestLabel / 2f),
                    this.size.height - 4f,
                    labelPaint
                )
            }
        }

        selectedIndex?.let { i ->
            val bar = bars.getOrNull(i) ?: return@let
            val barHeight = plotHeight * bar.heightFraction
            val x = plotLeft + i * (barWidth + gap) + barWidth / 2f
            val tooltipText = if (bar.label.isNotEmpty()) "${bar.label}: ${valueFormatter(bar.value)}" else valueFormatter(bar.value)
            drawTooltip(
                text = tooltipText,
                anchorX = x,
                anchorY = (plotBottom - barHeight).coerceAtLeast(plotTop),
                canvasWidth = this.size.width
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTooltip(
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
    val boxTop = (anchorY - boxHeight - 6.dp.toPx()).coerceAtLeast(0f)

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

internal fun axisTextPaint(color: Color, sizePx: Float): android.graphics.Paint =
    android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = sizePx
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

internal fun indexForX(x: Float, totalWidth: Float, leftMargin: Float, count: Int): Int {
    if (count <= 0) return 0
    val plotWidth = (totalWidth - leftMargin).coerceAtLeast(1f)
    val relativeX = (x - leftMargin).coerceIn(0f, plotWidth)
    val slot = (relativeX / plotWidth * count).toInt()
    return slot.coerceIn(0, count - 1)
}

internal fun Float.formatAxisValue(): String =
    if (this == this.toLong().toFloat()) this.toLong().toString() else "%.2f".format(this)

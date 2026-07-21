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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartPoint(
    val value: Float,
    val label: String = ""
)

@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF3B6FF2),
    dashedReferenceColor: Color = Color(0xFFE0A63C),
    axisLabelColor: Color = Color(0xFF8A8FA3),
    valueFormatter: (Float) -> String = { it.formatAxisValue() }
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current
    val axisLabelSizePx = with(density) { 10.sp.toPx() }
    val xLabelHeightPx = with(density) { 18.dp.toPx() }
    val yAxisWidthPx = with(density) { 30.dp.toPx() }
    val tooltipAreaPx = with(density) { 26.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(points) {
                if (points.size < 2) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val pointerId = down.id
                    selectedIndex = indexForX(down.position.x, size.width.toFloat(), yAxisWidthPx, points.size)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        selectedIndex = indexForX(change.position.x, size.width.toFloat(), yAxisWidthPx, points.size)
                        change.consume()
                    }
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
        val offsets = points.mapIndexed { i, p ->
            Offset(plotLeft + i * stepX, plotBottom - plotHeight * p.value.coerceIn(0f, 1f))
        }

        for (i in 0 until offsets.size - 1) {
            drawLine(
                color = lineColor,
                start = offsets[i],
                end = offsets[i + 1],
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
        offsets.forEachIndexed { i, p ->
            val isSelected = selectedIndex == i
            drawCircle(color = lineColor, radius = if (isSelected) 9f else 6f, center = p)
        }

        // dashed reference/baseline line — spans the full plot width so it reads as a
        // reference for every point, not just the last few.
        val refY = plotTop + plotHeight * 0.72f
        drawLine(
            color = dashedReferenceColor,
            start = Offset(plotLeft, refY),
            end = Offset(plotRight, refY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        points.forEachIndexed { i, p ->
            if (p.label.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    p.label,
                    plotLeft + i * stepX,
                    this.size.height - 4f,
                    axisTextPaint(axisLabelColor, axisLabelSizePx)
                )
            }
        }

        drawContext.canvas.nativeCanvas.apply {
            val paint = axisTextPaint(axisLabelColor, axisLabelSizePx).apply {
                textAlign = android.graphics.Paint.Align.LEFT
            }
            drawText(valueFormatter(0f), 2f, plotBottom, paint)
            drawText(valueFormatter(1f), 2f, plotTop + axisLabelSizePx, paint)
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

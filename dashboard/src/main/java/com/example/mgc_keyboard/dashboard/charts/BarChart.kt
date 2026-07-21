package com.example.mgc_keyboard.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class Bar(val heightFraction: Float, val color: Color)

@Composable
fun BarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    baselineColor: Color = Color(0xFFB9C6E8)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val barCount = bars.size
        val gap = size.width * 0.03f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val baselineY = size.height * 0.15f

        drawLine(
            color = baselineColor,
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        bars.forEachIndexed { i, bar ->
            val barHeight = size.height * bar.heightFraction
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = bar.color,
                topLeft = Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
    }
}

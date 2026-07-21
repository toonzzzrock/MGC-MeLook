package com.example.mgc_keyboard.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF3B6FF2),
    dashedReferenceColor: Color = Color(0xFFE0A63C)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1)
        val offsets = points.mapIndexed { i, v ->
            Offset(i * stepX, size.height * (1f - v))
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
        offsets.forEach { p ->
            drawCircle(color = lineColor, radius = 6f, center = p)
        }

        // dashed reference line ("less active than usual") near the tail
        val refY = size.height * 0.72f
        drawLine(
            color = dashedReferenceColor,
            start = Offset(size.width * 0.55f, refY),
            end = Offset(size.width, refY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    }
}

package com.example.mgc_keyboard.dashboard.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.UsualRange

/**
 * A metric row's whole answer in one line: a rail spanning what the user has done in this window,
 * a shaded band for their usual range, and a dot for today. Reading it needs no numbers — the dot
 * is either on the band or off it.
 */
@Composable
fun RangeStrip(
    range: UsualRange,
    concerning: Boolean,
    outsideUsualRange: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(18.dp)) {
        val dotRadius = 5.dp.toPx()
        // Inset by the dot so a today at either extreme is drawn whole, not clipped in half.
        val left = dotRadius
        val right = size.width - dotRadius
        val width = right - left
        val midY = size.height / 2f
        val railHeight = 3.dp.toPx()
        val bandHeight = 9.dp.toPx()

        drawRoundRect(
            color = MelookColors.Border,
            topLeft = Offset(left, midY - railHeight / 2f),
            size = Size(width, railHeight),
            cornerRadius = CornerRadius(railHeight / 2f, railHeight / 2f)
        )

        val bandLeft = left + width * range.bandStart
        val bandWidth = (width * (range.bandEnd - range.bandStart)).coerceAtLeast(railHeight)
        drawRoundRect(
            color = MelookColors.SeriesNeutral.copy(alpha = 0.35f),
            topLeft = Offset(bandLeft, midY - bandHeight / 2f),
            size = Size(bandWidth, bandHeight),
            cornerRadius = CornerRadius(bandHeight / 2f, bandHeight / 2f)
        )

        val todayX = left + width * range.today
        val dotColor = when {
            concerning -> MelookColors.SeriesFlagged
            outsideUsualRange -> MelookColors.TextGray
            else -> MelookColors.SeriesNeutral
        }
        // White collar so the dot stays legible where it overlaps the band.
        drawCircle(color = MelookColors.Surface, radius = dotRadius + 2.dp.toPx(), center = Offset(todayX, midY))
        drawCircle(color = dotColor, radius = dotRadius, center = Offset(todayX, midY))
    }
}

package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.TrendSeries
import com.example.mgc_keyboard.dashboard.charts.ChartInfoDot
import com.example.mgc_keyboard.dashboard.charts.LineChart
import com.example.mgc_keyboard.dashboard.charts.sentimentAxisLabel

/**
 * The long view: every signal over every recorded day, each against the shaded band of the user's
 * own usual range. Days with nothing recorded are gaps, not points — the axis is a calendar, so
 * the shape of a line is the shape of time. Nothing here is flagged as urgent; Home covers today.
 */
@Composable
fun TrendsScreen(
    hasEnoughWeeksForTrend: Boolean = true,
    trends: List<TrendSeries> = emptyList(),
    daysOfDataCollected: Int = 14,
    onOpenMetric: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MelookColors.BackgroundLight)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenHeader(
                title = "Trends",
                caption = "Every signal across $daysOfDataCollected recorded days · shaded band is your usual range"
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (!hasEnoughWeeksForTrend || trends.isEmpty()) {
                    SectionCard {
                        Text("Not enough data yet", color = MelookColors.TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "A trend line needs at least 14 days before it says more than noise — $daysOfDataCollected days recorded so far.",
                            color = MelookColors.TextGray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    trends.forEach { trend ->
                        TrendCard(trend, onOpenMetric)
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(
                        "Today is left out of these lines: it is a part-day, and four of these signals " +
                            "add up through the day. Home is where today is compared.",
                        color = MelookColors.TextFaint,
                        fontSize = 11.sp
                    )
                }

                DisclaimerLine()
            }
        }
        bottomBar()
    }
}

@Composable
private fun TrendCard(trend: TrendSeries, onOpenMetric: (String) -> Unit) {
    SectionCard(modifier = Modifier.clickable { onOpenMetric(trend.key) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trend.name, color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            ChartInfoDot(trend.info)
            Spacer(Modifier.fillMaxWidth().weight(1f))
            RowChevron()
        }
        Text(trend.caption, color = MelookColors.TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LineChart(
            points = trend.points,
            lineColor = MelookColors.SeriesNeutral,
            maxValue = trend.maxValue,
            band = trend.band,
            valueFormatter = trend.format,
            // Sentiment's axis reads as words; every other signal is already in its own unit.
            axisFormatter = if (trend.maxValue == 1f) { v -> sentimentAxisLabel(v) } else trend.format
        )
        Spacer(Modifier.height(10.dp))
        Text(trend.summary, color = MelookColors.TextGray, fontSize = 12.sp)
        Text(
            "Ringed dot is the latest day · amber dots sat outside your usual range",
            color = MelookColors.TextFaint,
            fontSize = 11.sp
        )
    }
}

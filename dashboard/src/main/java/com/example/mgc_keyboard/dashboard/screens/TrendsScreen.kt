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
import com.example.mgc_keyboard.dashboard.MetricKeys
import com.example.mgc_keyboard.dashboard.charts.ChartCitations
import com.example.mgc_keyboard.dashboard.charts.ChartInfoDot
import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import com.example.mgc_keyboard.dashboard.charts.LineChart
import com.example.mgc_keyboard.dashboard.charts.sentimentAxisLabel
import com.example.mgc_keyboard.dashboard.charts.sentimentLabel

private val DEFAULT_TREND_POINTS = listOf(0.85f, 0.80f, 0.78f, 0.72f, 0.65f, 0.55f, 0.45f, 0.35f).map { ChartPoint(it) }

/**
 * The long view: one line over every recorded day, against the direction label the ViewModel
 * derives from the same window. Day-to-day noise is what Home is for, so nothing here is
 * flagged as urgent — this screen exists to make a slow drift visible.
 */
@Composable
fun TrendsScreen(
    hasEnoughWeeksForTrend: Boolean = true,
    trendPoints: List<ChartPoint> = DEFAULT_TREND_POINTS,
    trendDirectionLabel: String = "about the same as usual",
    quietStretchHours: Float = 3.1f,
    quietStretchIncreased: Boolean = true,
    daysOfDataCollected: Int = 14,
    onOpenMetric: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MelookColors.BackgroundLight)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenHeader(
                title = "Trends",
                caption = "Typing sentiment across $daysOfDataCollected recorded days"
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (!hasEnoughWeeksForTrend) {
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
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Typing sentiment", color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            ChartInfoDot(ChartCitations.TRENDS_SENTIMENT)
                        }
                        Text("0 = negative, 1 = positive · one point per day", color = MelookColors.TextGray, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        LineChart(
                            points = trendPoints,
                            lineColor = MelookColors.SeriesNeutral,
                            valueFormatter = { sentimentLabel(it) },
                            axisFormatter = { sentimentAxisLabel(it) }
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Last 7 days read $trendDirectionLabel, compared with your own earlier weeks.",
                            color = MelookColors.TextGray,
                            fontSize = 12.sp
                        )
                        Text("Source: Eichstaedt et al., 2018", color = MelookColors.TextFaint, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                SectionLabel("ALSO OVER THIS WINDOW")
                SectionCard(modifier = Modifier.clickable { onOpenMetric(MetricKeys.QUIET) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Longest quiet stretch", color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                ChartInfoDot(ChartCitations.PHONE_SCHEDULE)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${quietStretchHours.toInt()} hours with no typing and no screen time — " +
                                    if (quietStretchIncreased) "longer than your usual" else "in line with your usual",
                                color = MelookColors.TextGray,
                                fontSize = 12.sp
                            )
                        }
                        RowChevron()
                    }
                }

                DisclaimerLine()
            }
        }
        bottomBar()
    }
}

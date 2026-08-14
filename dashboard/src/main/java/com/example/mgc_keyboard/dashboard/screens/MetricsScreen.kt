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
import androidx.compose.foundation.layout.width
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
import com.example.mgc_keyboard.dashboard.MetricSnapshot
import com.example.mgc_keyboard.dashboard.charts.ChartCitations
import com.example.mgc_keyboard.dashboard.charts.ChartInfoDot
import com.example.mgc_keyboard.dashboard.charts.HeatmapDay
import com.example.mgc_keyboard.dashboard.charts.heatmapWeekSpan

/**
 * Every tracked signal in one list, most-deviating first. This is the drill-down index: a row
 * opens that signal's chart, method and source, and the ⓘ on the row opens the source without
 * leaving the list.
 */
@Composable
fun MetricsScreen(
    metrics: List<MetricSnapshot>,
    heatmapDays: List<HeatmapDay>,
    daysOfDataCollected: Int,
    onOpenMetric: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MelookColors.BackgroundLight)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenHeader(
                title = "Metrics",
                caption = "Tap a row for the chart and method · ⓘ for the source"
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (metrics.isEmpty()) {
                    SectionCard {
                        Text("Nothing to compare yet", color = MelookColors.TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Each signal needs a few days of your own history before a comparison means anything. $daysOfDataCollected days recorded so far.",
                            color = MelookColors.TextGray,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    SectionCard {
                        metrics.forEachIndexed { index, metric ->
                            if (index > 0) {
                                Spacer(Modifier.fillMaxWidth().height(1.dp).background(MelookColors.Border))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenMetric(metric.key) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(metric.name, color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        ChartInfoDot(metric.info)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(metric.unitCaption, color = MelookColors.TextGray, fontSize = 12.sp)
                                    Text("Source: ${metric.sourceLabel}", color = MelookColors.TextFaint, fontSize = 11.sp)
                                }
                                Column(
                                    modifier = Modifier.width(78.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(metric.todayLabel, color = MelookColors.TextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    DeltaText(metric.deltaLabel, metric.concerning, metric.outsideUsualRange, fontSize = 12)
                                }
                                Spacer(Modifier.width(6.dp))
                                RowChevron()
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                SectionLabel("LONG RANGE")
                SectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMetric(MetricKeys.HEATMAP) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Activity heatmap", color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                ChartInfoDot(ChartCitations.ACTIVITY_HEATMAP)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Key presses per day, last ${heatmapWeekSpan(heatmapDays)} weeks",
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

package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import com.example.mgc_keyboard.dashboard.charts.RangeStrip
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
                caption = "Shaded band is your usual range · the dot is today · marked rows sat outside it, amber where the move runs the way the research warns about"
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
                            // Name and today's figure on one line, the strip under it: where the
                            // dot sits against the band is the reading, so the row needs no prose
                            // and the source can stay on the detail page where it is explained.
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenMetric(metric.key) }
                                    // Tint marks the rows worth reading. Padding stays the same on
                                    // every row: inset only the tinted ones and their strips would
                                    // be narrower than the rest, so the dots would stop lining up.
                                    .background(if (metric.concerning) MelookColors.WarnSurface else MelookColors.Surface)
                                    .padding(vertical = 12.dp, horizontal = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Fixed-width slot so the marker never shifts the name.
                                    Box(Modifier.width(12.dp)) {
                                        if (metric.outsideUsualRange) {
                                            Text(
                                                "●",
                                                color = if (metric.concerning) MelookColors.SeriesFlagged else MelookColors.TextGray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Text(metric.name, color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    ChartInfoDot(metric.info)
                                    Spacer(Modifier.weight(1f))
                                    // A figure on its own says nothing, so today never appears
                                    // without the average it is being read against.
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(metric.todayLabel, color = MelookColors.TextDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("usually ${metric.baselineLabel}", color = MelookColors.TextFaint, fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    RowChevron()
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RangeStrip(
                                        range = metric.usualRange,
                                        concerning = metric.concerning,
                                        outsideUsualRange = metric.outsideUsualRange,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Box(Modifier.width(70.dp)) {
                                        DeltaText(metric.deltaLabel, metric.concerning, metric.outsideUsualRange, fontSize = 12)
                                    }
                                }
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

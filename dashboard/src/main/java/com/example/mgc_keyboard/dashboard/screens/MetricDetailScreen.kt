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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.MetricKeys
import com.example.mgc_keyboard.dashboard.MetricSnapshot
import com.example.mgc_keyboard.dashboard.charts.Bar
import com.example.mgc_keyboard.dashboard.charts.BarChart
import com.example.mgc_keyboard.dashboard.charts.ChartCitations
import com.example.mgc_keyboard.dashboard.charts.ChartInfo
import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import com.example.mgc_keyboard.dashboard.charts.GithubHeatmap
import com.example.mgc_keyboard.dashboard.charts.HeatmapDay
import com.example.mgc_keyboard.dashboard.charts.LineChart
import com.example.mgc_keyboard.dashboard.charts.citationSearchUrl
import com.example.mgc_keyboard.dashboard.charts.heatmapWeekSpan
import com.example.mgc_keyboard.dashboard.charts.sentimentAxisLabel
import com.example.mgc_keyboard.dashboard.charts.sentimentLabel

/**
 * The per-signal page every chevron on Home and Metrics leads to: the chart, the comparison
 * against the user's own average, how the number is computed, and the paper behind tracking it
 * at all — with a tap-through to that paper.
 */
@Composable
fun MetricDetailScreen(
    metricKey: String,
    metric: MetricSnapshot?,
    heatmapDays: List<HeatmapDay>,
    hourlyActivityPattern: List<Bar>,
    dailyActivityPatternMonth: List<Bar>,
    onBack: () -> Unit
) {
    val isHeatmap = metricKey == MetricKeys.HEATMAP
    val title = if (isHeatmap) "Activity heatmap" else metric?.name ?: "Metric"
    val info: ChartInfo = if (isHeatmap) ChartCitations.ACTIVITY_HEATMAP else metric?.info ?: ChartCitations.ACTIVITY_HEATMAP

    Column(
        Modifier
            .fillMaxSize()
            .background(MelookColors.BackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MelookColors.TextDark)
            }
            // No breadcrumb: this page opens from Home, Trends and Metrics alike, and a fixed
            // "Metrics" label would name a screen the back arrow is not going to.
            Column(Modifier.weight(1f)) {
                Text(title, color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))

        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionCard {
                Text(
                    if (isHeatmap) "Key presses per day, last ${heatmapWeekSpan(heatmapDays)} weeks — darker means busier"
                    else metric?.unitCaption.orEmpty(),
                    color = MelookColors.TextGray,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                when {
                    isHeatmap -> GithubHeatmap(days = heatmapDays)

                    metric == null -> Text(
                        "Not enough days recorded yet to draw this.",
                        color = MelookColors.TextGray,
                        fontSize = 13.sp
                    )

                    metricKey == MetricKeys.SENTIMENT -> LineChart(
                        points = metric.points.ifEmpty { List(7) { ChartPoint(0.5f) } },
                        lineColor = MelookColors.SeriesNeutral,
                        valueFormatter = { sentimentLabel(it) },
                        axisFormatter = { sentimentAxisLabel(it) }
                    )

                    else -> BarChart(bars = metric.bars)
                }

                // Screen-on time is the one signal worth reading two ways: the shape of a
                // typical day, and the run of daily totals. Both come from the same counters.
                if (metricKey == MetricKeys.SCREEN_TIME) {
                    var hourly by remember { mutableStateOf(true) }
                    Spacer(Modifier.height(14.dp))
                    ModeToggle(
                        leftLabel = "By hour · 7 days",
                        rightLabel = "By day · month",
                        leftSelected = hourly,
                        onSelect = { hourly = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    if (hourly) {
                        BarChart(bars = hourlyActivityPattern.ifEmpty { List(24) { Bar(0.05f, MelookColors.SeriesNeutral) } })
                    } else {
                        BarChart(bars = dailyActivityPatternMonth.ifEmpty { List(30) { Bar(0.05f, MelookColors.SeriesNeutral) } })
                    }
                }
            }

            if (metric != null) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("TODAY VS YOUR AVERAGE")
                SectionCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("Today", color = MelookColors.TextFaint, fontSize = 11.sp)
                            Text(metric.todayLabel, color = MelookColors.TextDark, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(metric.baselineCaption, color = MelookColors.TextFaint, fontSize = 11.sp)
                            Text(metric.baselineLabel, color = MelookColors.TextGray, fontSize = 22.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Change", color = MelookColors.TextFaint, fontSize = 11.sp)
                            DeltaText(metric.deltaLabel, metric.concerning, metric.outsideUsualRange, fontSize = 18)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (metric.outsideUsualRange)
                            "Outside your usual range — further from your own average than a typical day's variation."
                        else
                            "Within your usual range — this size of change is normal day-to-day variation for you.",
                        color = if (metric.concerning) MelookColors.Negative else MelookColors.TextGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel("HOW IT IS MEASURED")
                SectionCard {
                    Text(metric.howMeasured, color = MelookColors.TextGray, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("WHY WE TRACK IT")
            SectionCard {
                Text(info.whatItShows, color = MelookColors.TextDark, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text("Research reference", color = MelookColors.TextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(info.citation, color = MelookColors.TextGray, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                val context = LocalContext.current
                Text(
                    "Open in Google Scholar ›",
                    color = MelookColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(citationSearchUrl(info.citation))))
                    }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Correlational finding. This app does not diagnose anything and does not replace a clinician.",
                    color = MelookColors.TextFaint,
                    fontSize = 11.sp
                )
            }

            DisclaimerLine()
        }
    }
}

/** Two-state segmented control. Moved here from the old all-stats screen — the phone-schedule
 * toggle is the only place in the app that needs one. */
@Composable
private fun ModeToggle(leftLabel: String, rightLabel: String, leftSelected: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MelookColors.BackgroundLight)
            .padding(2.dp)
    ) {
        ModeToggleChip(leftLabel, leftSelected, { onSelect(true) }, Modifier.weight(1f))
        ModeToggleChip(rightLabel, !leftSelected, { onSelect(false) }, Modifier.weight(1f))
    }
}

@Composable
private fun ModeToggleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MelookColors.Surface else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Border) else null,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) MelookColors.TextDark else MelookColors.TextGray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)
        )
    }
}

package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.charts.Bar
import com.example.mgc_keyboard.dashboard.charts.BarChart
import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import com.example.mgc_keyboard.dashboard.charts.GithubHeatmap
import com.example.mgc_keyboard.dashboard.charts.HeatmapDay
import com.example.mgc_keyboard.dashboard.charts.LineChart

/** Everything currently collected on-device (README §4.1/§4.3), in one place — not just the
 * narrative insight cards on the summary/trends screens. All figures are local-only. */
@Composable
fun AllStatsScreen(
    hourlyActivityPattern: List<Bar>,
    dailyActivityPatternMonth: List<Bar>,
    backspaceRateBars: List<Bar>,
    sentimentTrendRecent: List<ChartPoint>,
    appSwitchBars: List<Bar>,
    appVarietyBars: List<Bar>,
    heatmapDays: List<HeatmapDay>,
    totalKeyPressesToday: Int,
    totalBackspacesToday: Int,
    totalWordsScoredToday: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MelookColors.TextDark)
            }
            Text("Everything we track", color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(4.dp))
            Text(
                "All signals below come from the keyboard and screen-usage tracker on this device — nothing leaves your phone.",
                color = MelookColors.TextGray,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(20.dp))

            TodayTotalsRow(totalKeyPressesToday, totalBackspacesToday, totalWordsScoredToday)
            Spacer(Modifier.height(20.dp))

            var phoneScheduleModeIsHourly by remember { mutableStateOf(true) }
            StatCard(
                title = "Phone on/off schedule",
                subtitle = if (phoneScheduleModeIsHourly)
                    "Average screen-on time by hour of day (last 7 days), tallest = busiest hour"
                else
                    "Total screen-on time per day (last month)"
            ) {
                ModeToggle(
                    leftLabel = "Hourly · 7 days",
                    rightLabel = "Daily · month",
                    leftSelected = phoneScheduleModeIsHourly,
                    onSelect = { phoneScheduleModeIsHourly = it }
                )
                Spacer(Modifier.height(8.dp))
                if (phoneScheduleModeIsHourly) {
                    BarChart(bars = hourlyActivityPattern.ifEmpty { List(24) { Bar(0.05f, MelookColors.Accent) } })
                } else {
                    BarChart(bars = dailyActivityPatternMonth.ifEmpty { List(30) { Bar(0.05f, MelookColors.Accent) } })
                }
            }
            Spacer(Modifier.height(16.dp))

            StatCard(
                title = "Backspace rate",
                subtitle = "Share of key presses that were backspace, per day (last 7 days)"
            ) {
                BarChart(bars = backspaceRateBars.ifEmpty { List(7) { Bar(0.05f, MelookColors.Accent) } })
            }
            Spacer(Modifier.height(16.dp))

            StatCard(
                title = "Typing sentiment",
                subtitle = "On-device sentiment score of what you type, per day (last 7 days)"
            ) {
                LineChart(points = sentimentTrendRecent.ifEmpty { List(7) { ChartPoint(0.5f) } })
            }
            Spacer(Modifier.height(16.dp))

            StatCard(
                title = "App switching",
                subtitle = "How often you jump between apps, per day (last 7 days)"
            ) {
                BarChart(bars = appSwitchBars.ifEmpty { List(7) { Bar(0.05f, MelookColors.Amber) } })
            }
            Spacer(Modifier.height(16.dp))

            StatCard(
                title = "App variety",
                subtitle = "Number of distinct apps used, per day (last 7 days)"
            ) {
                BarChart(bars = appVarietyBars.ifEmpty { List(7) { Bar(0.05f, MelookColors.Green) } })
            }
            Spacer(Modifier.height(16.dp))

            StatCard(
                title = "Activity heatmap",
                subtitle = "Key presses per day (last ~14 weeks) — darker means busier"
            ) {
                GithubHeatmap(days = heatmapDays)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "🔒  All of this stays on your phone",
                color = MelookColors.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clickable(enabled = false) {}
            )
        }
    }
}

@Composable
private fun TodayTotalsRow(keyPresses: Int, backspaces: Int, wordsScored: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TotalPill(label = "Key presses today", value = keyPresses.toString(), modifier = Modifier.weight(1f))
        TotalPill(label = "Backspaces today", value = backspaces.toString(), modifier = Modifier.weight(1f))
        TotalPill(label = "Words scored today", value = wordsScored.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TotalPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MelookColors.BackgroundLight,
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = MelookColors.TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MelookColors.TextGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ModeToggle(leftLabel: String, rightLabel: String, leftSelected: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MelookColors.Surface)
            .padding(2.dp)
    ) {
        ModeToggleChip(leftLabel, selected = leftSelected, onClick = { onSelect(true) }, modifier = Modifier.weight(1f))
        ModeToggleChip(rightLabel, selected = !leftSelected, onClick = { onSelect(false) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ModeToggleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MelookColors.Accent else MelookColors.Surface,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) androidx.compose.ui.graphics.Color.White else MelookColors.TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun StatCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MelookColors.BackgroundLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Divider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MelookColors.TextDark)
            Text(subtitle, color = MelookColors.TextGray, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

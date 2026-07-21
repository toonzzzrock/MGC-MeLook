package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.CollectedToday
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.charts.Bar
import com.example.mgc_keyboard.dashboard.charts.BarChart

private val DEFAULT_WEEK_BARS = listOf(
    Bar(0.55f, MelookColors.Accent),
    Bar(0.70f, MelookColors.Accent),
    Bar(0.62f, MelookColors.Accent),
    Bar(0.85f, MelookColors.Accent),
    Bar(0.45f, MelookColors.Amber),
    Bar(0.38f, MelookColors.Amber),
    Bar(0.30f, MelookColors.Amber)
)

private val WIND_DOWN_STEPS = listOf(
    "Put your phone face-down for the next 5 minutes.",
    "Breathe in for 4 seconds, hold for 4, out for 6 — repeat 6 times.",
    "Unclench your jaw and drop your shoulders.",
    "Write down one thing that's on your mind, even just a few words.",
    "When the 5 minutes are up, decide if you still want to pick your phone back up."
)

@Composable
fun WeeklySummaryScreen(
    hasBaseline: Boolean = true,
    paceChangePercent: Int = 15,
    weekBars: List<Bar> = DEFAULT_WEEK_BARS,
    lateNightChangePercent: Int = 22,
    appVarietyLower: Boolean = true,
    showSuggestion: Boolean = true,
    collectedToday: CollectedToday = CollectedToday(0, "0m", 0, 0),
    displayName: String = "there",
    onNext: () -> Unit
) {
    var showWindDown by remember { mutableStateOf(false) }

    if (showWindDown) {
        AlertDialog(
            onDismissRequest = { showWindDown = false },
            title = { Text("5-minute wind-down") },
            text = {
                Column {
                    WIND_DOWN_STEPS.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", fontSize = 13.sp, color = MelookColors.TextDark, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWindDown = false }) { Text("Done") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("YOUR WEEKLY SUMMARY", color = MelookColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Hi $displayName! Here's your week", color = MelookColors.TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        if (!hasBaseline) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MelookColors.AccentSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Still learning your patterns", color = MelookColors.TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Once a few more days of data are collected, we'll compare this week against your usual baseline. Until then, here's what's been captured live.",
                        color = MelookColors.TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("TODAY SO FAR", color = MelookColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            InsightRow(
                title = "Typing sessions",
                detail = "${collectedToday.typingSessions} today",
                icon = Icons.Default.ArrowUpward,
                iconTint = MelookColors.Accent
            )
            Spacer(Modifier.height(10.dp))
            InsightRow(
                title = "Screen time",
                detail = collectedToday.screenTimeLabel,
                icon = Icons.Default.ArrowUpward,
                iconTint = MelookColors.Accent
            )
            Spacer(Modifier.height(10.dp))
            InsightRow(
                title = "Apps used",
                detail = "${collectedToday.appsUsed} apps",
                icon = Icons.Default.ArrowUpward,
                iconTint = MelookColors.Accent
            )
            Spacer(Modifier.height(10.dp))
            InsightRow(
                title = "Quiet stretches",
                detail = "${collectedToday.quietStretches} today",
                icon = Icons.Default.ArrowDownward,
                iconTint = MelookColors.Accent
            )
            Spacer(Modifier.weight(1f))
            Text(
                "This updates live as you type — no waiting required.",
                color = MelookColors.TextGray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            Text(
                "See trends →",
                color = MelookColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clickable { onNext() }
            )
            return@Column
        }

        val paceWord = if (paceChangePercent >= 0) "slowed" else "picked up"
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MelookColors.AccentSoft,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Your typing pace $paceWord ${kotlin.math.abs(paceChangePercent)}%", color = MelookColors.TextDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("compared with your usual pace", color = MelookColors.TextGray, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Text("baseline", color = MelookColors.TextGray, fontSize = 10.sp)
                BarChart(bars = weekBars)
            }
        }

        Spacer(Modifier.height(14.dp))
        InsightRow(
            title = "Late-night phone use",
            detail = "$lateNightChangePercent% more than usual",
            icon = Icons.Default.ArrowUpward,
            iconTint = Color(0xFFE0733C)
        )
        Spacer(Modifier.height(10.dp))
        InsightRow(
            title = "App variety",
            detail = if (appVarietyLower) "lower this week" else "higher this week",
            icon = if (appVarietyLower) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            iconTint = MelookColors.Accent
        )

        if (showSuggestion) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MelookColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Divider),
                modifier = Modifier.fillMaxWidth().clickable { showWindDown = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MelookColors.Accent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Suggested for you", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MelookColors.TextDark)
                        Text("A 5-minute wind-down routine for late nights", fontSize = 12.sp, color = MelookColors.TextGray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MelookColors.TextGray)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "See what you can do →",
            color = MelookColors.Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clickable { onNext() }
        )
    }
}

@Composable
private fun InsightRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MelookColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Divider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 13.sp, color = MelookColors.TextDark)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(detail, fontSize = 12.sp, color = iconTint)
            }
        }
    }
}

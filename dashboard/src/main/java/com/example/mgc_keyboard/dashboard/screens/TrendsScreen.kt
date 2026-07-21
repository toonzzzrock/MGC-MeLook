package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.charts.LineChart

private val DEFAULT_TREND_POINTS = listOf(0.85f, 0.80f, 0.78f, 0.72f, 0.65f, 0.55f, 0.45f, 0.35f)

@Composable
fun TrendsScreen(
    hasEnoughWeeksForTrend: Boolean = true,
    trendPoints: List<Float> = DEFAULT_TREND_POINTS,
    trendDirectionLabel: String = "less active than usual →",
    quietStretchHours: Float = 3.1f,
    quietStretchIncreased: Boolean = true,
    daysOfDataCollected: Int = 14,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("YOUR TRENDS · LAST 8 WEEKS", color = MelookColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("How your habits have changed", color = MelookColors.TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Small changes add up too slowly to notice day by day. Here are yours at a glance.",
            color = MelookColors.TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        if (!hasEnoughWeeksForTrend) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MelookColors.BackgroundLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Not enough data yet", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MelookColors.TextDark)
                    Text(
                        "Trends need at least two weeks of data to be meaningful ($daysOfDataCollected of 14 days collected so far).",
                        color = MelookColors.TextGray,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("LIVE SO FAR", color = MelookColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            TrendRow(
                title = "Longest quiet stretch",
                detail = "$quietStretchHours hours of inactivity so far",
                icon = Icons.Default.ArrowDownward,
                iconTint = MelookColors.Accent
            )
            Spacer(Modifier.weight(1f))
            Text(
                "← Back to summary",
                color = MelookColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onNext() }
            )
            Text(
                "🔒  All of this stays on your phone",
                color = MelookColors.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
            return@Column
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MelookColors.BackgroundLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                LineChart(points = trendPoints)
                Text(trendDirectionLabel, color = MelookColors.Amber, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MelookColors.AccentSoft,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Spotted early", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MelookColors.TextDark)
                Text(
                    "This change became visible weeks before you might have noticed it yourself.",
                    color = MelookColors.TextGray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        TrendRow(
            title = "Longest quiet stretch",
            detail = "${quietStretchHours} hours of inactivity — ${if (quietStretchIncreased) "more" else "less"} than usual",
            icon = if (quietStretchIncreased) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            iconTint = if (quietStretchIncreased) Color(0xFFE0733C) else MelookColors.Accent
        )

        Spacer(Modifier.weight(1f))
        Text(
            "← Back to summary",
            color = MelookColors.Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onNext() }
        )
        Text(
            "🔒  All of this stays on your phone",
            color = MelookColors.Green,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun TrendRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color) {
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

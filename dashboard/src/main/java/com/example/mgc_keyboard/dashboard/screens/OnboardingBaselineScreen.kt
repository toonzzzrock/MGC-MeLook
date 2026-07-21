package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.CollectedToday
import com.example.mgc_keyboard.dashboard.MelookColors

private data class LiveRow(val time: String, val description: String)

private val LIVE_FEED = listOf(
    LiveRow("21:32", "typing session captured"),
    LiveRow("21:05", "screen unlocked"),
    LiveRow("19:48", "app switch · social → music"),
    LiveRow("18:12", "call metadata counted (no content)")
)

private const val BASELINE_TARGET_DAYS = 7

@Composable
fun OnboardingBaselineScreen(
    daysCollected: Int = 3,
    collectedToday: CollectedToday = CollectedToday(14, "5h 12m", 9, 2),
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
            .clickable { onNext() }
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "LEARNING · DAY ${daysCollected.coerceIn(1, BASELINE_TARGET_DAYS)} OF $BASELINE_TARGET_DAYS",
            color = MelookColors.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Getting to know your routine",
            color = MelookColors.TextDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Keep using your phone as usual. Your personal baseline is built quietly, right on your phone.",
            color = MelookColors.TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { daysCollected.coerceIn(0, BASELINE_TARGET_DAYS) / BASELINE_TARGET_DAYS.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = MelookColors.Accent,
            trackColor = MelookColors.AccentSoft
        )
        Spacer(Modifier.height(6.dp))
        Text("${daysCollected.coerceIn(0, BASELINE_TARGET_DAYS)} of $BASELINE_TARGET_DAYS days collected", color = MelookColors.TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MelookColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MelookColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Collected today", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MelookColors.TextDark)
                Spacer(Modifier.height(10.dp))
                listOf(
                    Triple("⌨", "Typing sessions", "${collectedToday.typingSessions} captured"),
                    Triple("⏱", "Screen-time", collectedToday.screenTimeLabel),
                    Triple("▦", "Apps used", "${collectedToday.appsUsed} different apps"),
                    Triple("☾", "Quiet stretches", "${collectedToday.quietStretches} detected")
                ).forEach { (icon, label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(icon, modifier = Modifier.width(28.dp))
                        Text(label, color = MelookColors.TextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(value, color = MelookColors.TextGray, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MelookColors.AccentSoft,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "LIVE · PROCESSED ON YOUR PHONE",
                    color = MelookColors.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                LIVE_FEED.forEach { row ->
                    Text(
                        "•  ${row.time} — ${row.description}",
                        color = MelookColors.TextDark,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "🔒  Nothing has left your phone.",
            color = MelookColors.Green,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }
}

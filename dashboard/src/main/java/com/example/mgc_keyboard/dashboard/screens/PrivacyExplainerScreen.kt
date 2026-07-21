package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

/**
 * US1-1: plain-language summary of collected/non-collected data, shown before any permission
 * is requested. US1-3: same "processing happens on-device" explainer, reachable again from
 * Settings — [standalone] switches the CTA from "Accept & continue" to a plain "Back".
 */
@Composable
fun PrivacyExplainerScreen(standalone: Boolean = false, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("BEFORE YOU START", color = MelookColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Your data stays on your phone", color = MelookColors.TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        InfoCard(
            title = "What is collected",
            lines = listOf(
                "Typing rhythm & backspace rate (numbers only)",
                "A sentiment score per word, computed on-device",
                "Screen-time and how many different apps you use",
                "How often you switch between apps"
            ),
            positive = true
        )
        Spacer(Modifier.height(12.dp))
        InfoCard(
            title = "What is never collected",
            lines = listOf(
                "The words or messages you type",
                "Call or message content",
                "Anything sent off your device — this app has no internet permission"
            ),
            positive = false
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MelookColors.NavyCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("On-device processing", color = MelookColors.Surface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "All scoring and aggregation runs locally on your phone using an on-device model. Raw text is discarded immediately after scoring and never written to storage.",
                    color = MelookColors.TextGray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(if (standalone) "Back" else "I understand, continue")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>, positive: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (positive) MelookColors.AccentSoft else MelookColors.BackgroundLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MelookColors.TextDark)
            Spacer(Modifier.height(8.dp))
            lines.forEach { line ->
                Text("${if (positive) "•" else "✕"}  $line", fontSize = 12.sp, color = MelookColors.TextGray)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

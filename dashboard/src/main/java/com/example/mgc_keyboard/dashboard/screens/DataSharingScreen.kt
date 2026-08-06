package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.bridge.ClinicalBridgeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** US7-1: transparent data-sharing screen. Reflects the real Clinical Bridge toggle
 * (Settings) instead of a fixed claim — the app does declare INTERNET and does make network
 * calls when a clinician connection is enabled, so the copy here must track [bridgeState]. */
@Composable
fun DataSharingScreen(bridgeState: ClinicalBridgeState = ClinicalBridgeState(), onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MelookColors.TextDark)
            }
            Text("Data sharing", color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        if (!bridgeState.enabled) {
            Surface(shape = RoundedCornerShape(16.dp), color = MelookColors.AccentSoft, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🔒  Nothing is being shared", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MelookColors.TextDark)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Clinical Bridge is off. All behavioral data is generated, scored, and stored only on this device, and no network call is made.",
                        fontSize = 13.sp,
                        color = MelookColors.TextGray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("If you turn Clinical Bridge on (Settings)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MelookColors.TextDark)
            Spacer(Modifier.height(8.dp))
            Text(
                "The device sends only the metrics listed below to the server you configure — never raw text, calls, or messages.",
                fontSize = 12.sp,
                color = MelookColors.TextGray
            )
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = MelookColors.Amber.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🌐  Clinical Bridge is on", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MelookColors.TextDark)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "This device sends the metrics below to ${bridgeState.serverUrl.ifBlank { "the configured server" }} on each sync.",
                        fontSize = 13.sp,
                        color = MelookColors.TextGray
                    )
                    bridgeState.lastSyncAtMillis?.let { lastSync ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Last sync: ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(lastSync))}",
                            fontSize = 11.sp,
                            color = MelookColors.TextGray
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Exactly what is sent", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MelookColors.TextDark)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Backspace rate (share of key presses that were backspace)",
                "On-device sentiment score of what you type",
                "Screen-on minutes",
                "Number of distinct apps used",
                "Total key presses and words scored"
            ).forEach { line ->
                Text("• $line", fontSize = 12.sp, color = MelookColors.TextGray)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Never sent: raw keystrokes, message content, call logs, contacts, or app names.",
                fontSize = 12.sp,
                color = MelookColors.TextGray
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

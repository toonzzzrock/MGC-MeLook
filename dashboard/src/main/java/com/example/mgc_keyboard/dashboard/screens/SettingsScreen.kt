package com.example.mgc_keyboard.dashboard.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.alerts.AlertThresholds
import com.example.mgc_keyboard.alerts.AlertThresholdsStore
import com.example.mgc_keyboard.dashboard.AppPreferencesStore
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.R
import com.example.mgc_keyboard.statscore.AuditEventType
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.launch

/** README §4.5: the only UI surface that calls AlertThresholdsStore.update(). */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDataSharing: () -> Unit,
    onOpenAllStats: () -> Unit
) {
    val context = LocalContext.current
    val thresholdsStore = remember { AlertThresholdsStore(context) }
    val repository = remember { StatsRepository.from(StatsDatabase.getInstance(context)) }
    val prefsStore = remember { AppPreferencesStore(context) }
    val scope = rememberCoroutineScope()

    val thresholds by thresholdsStore.thresholds.collectAsState(initial = AlertThresholds())
    val prefs by prefsStore.state.collectAsState(initial = null)
    var name by remember(prefs?.displayName) { mutableStateOf(prefs?.displayName ?: "") }
    var isSeedingDemoData by remember { mutableStateOf(false) }
    var isClearingData by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MelookColors.Surface
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MelookColors.TextDark)
            }
            Text("Settings", color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.logo_mark),
                contentDescription = "Mental Melook",
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = MelookColors.BackgroundLight, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Wellness alerts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MelookColors.TextDark)
                        Text("Gentle nudges when patterns shift", fontSize = 12.sp, color = MelookColors.TextGray)
                    }
                    Switch(
                        checked = thresholds.alertsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                val updated = thresholds.copy(alertsEnabled = enabled)
                                thresholdsStore.update(updated)
                                repository.recordAudit(AuditEventType.THRESHOLDS_CHANGED, "alertsEnabled=$enabled")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                ThresholdSlider(
                    label = "Backspace-rate threshold",
                    value = thresholds.backspaceRateThreshold,
                    valueLabel = "${(thresholds.backspaceRateThreshold * 100).toInt()}%",
                    onChange = { v ->
                        scope.launch {
                            val updated = thresholds.copy(backspaceRateThreshold = v)
                            thresholdsStore.update(updated)
                            repository.recordAudit(AuditEventType.THRESHOLDS_CHANGED, "backspaceRateThreshold=$v")
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                ThresholdSlider(
                    label = "Sentiment floor",
                    value = thresholds.sentimentFloor,
                    valueLabel = "${(thresholds.sentimentFloor * 100).toInt()}%",
                    onChange = { v ->
                        scope.launch {
                            val updated = thresholds.copy(sentimentFloor = v)
                            thresholdsStore.update(updated)
                            repository.recordAudit(AuditEventType.THRESHOLDS_CHANGED, "sentimentFloor=$v")
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = MelookColors.BackgroundLight, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Your name", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MelookColors.TextDark)
                Text("Used to greet you in your summaries", fontSize = 12.sp, color = MelookColors.TextGray)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(30) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            scope.launch {
                                prefsStore.setDisplayName(name.trim())
                                snackbarHostState.showSnackbar("Name saved")
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SettingsLinkRow(title = "Everything we track", onClick = onOpenAllStats)
        Spacer(Modifier.height(8.dp))
        SettingsLinkRow(title = "Privacy & on-device processing", onClick = onOpenPrivacy)
        Spacer(Modifier.height(8.dp))
        SettingsLinkRow(title = "Data sharing", onClick = onOpenDataSharing)

        Spacer(Modifier.height(24.dp))
        Text("TESTING", color = MelookColors.TextGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        SettingsLinkRow(
            title = if (isSeedingDemoData) "Loading demo data…" else "Load demo data (preview full dashboard)",
            enabled = !isSeedingDemoData
        ) {
            isSeedingDemoData = true
            scope.launch {
                try {
                    repository.seedDemoData()
                    snackbarHostState.showSnackbar("Demo data loaded — check the dashboard")
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Failed to load demo data", e)
                    snackbarHostState.showSnackbar("Couldn't load demo data, try again")
                } finally {
                    isSeedingDemoData = false
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsLinkRow(
            title = if (isClearingData) "Clearing data…" else "Clear all data",
            enabled = !isClearingData
        ) {
            showClearDataConfirm = true
        }
        Spacer(Modifier.height(24.dp))
    }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently deletes all locally stored stats, baselines, and audit logs on this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataConfirm = false
                    isClearingData = true
                    scope.launch {
                        try {
                            repository.clearAllData()
                            snackbarHostState.showSnackbar("All data cleared")
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to clear data", e)
                            snackbarHostState.showSnackbar("Couldn't clear data, try again")
                        } finally {
                            isClearingData = false
                        }
                    }
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThresholdSlider(label: String, value: Float, valueLabel: String, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MelookColors.TextDark)
            Text(valueLabel, fontSize = 13.sp, color = MelookColors.Accent, fontWeight = FontWeight.Medium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 0.1f..0.9f)
    }
}

@Composable
private fun SettingsLinkRow(title: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MelookColors.BackgroundLight,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontSize = 14.sp,
                color = if (enabled) MelookColors.TextDark else MelookColors.TextGray
            )
            if (enabled) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MelookColors.TextGray)
            } else {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MelookColors.Accent)
            }
        }
    }
}

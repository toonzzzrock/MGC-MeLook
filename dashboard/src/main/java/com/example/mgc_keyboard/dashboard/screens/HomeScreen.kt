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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.CollectedToday
import com.example.mgc_keyboard.dashboard.CurrentHourSnapshot
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.MetricSnapshot
import com.example.mgc_keyboard.dashboard.charts.ChartInfoDialog
import com.example.mgc_keyboard.dashboard.charts.ChartInfoDot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WIND_DOWN_STEPS = listOf(
    "Put your phone face-down for the next 5 minutes.",
    "Breathe in for 4 seconds, hold for 4, out for 6 — repeat 6 times.",
    "Unclench your jaw and drop your shoulders.",
    "Write down one thing that's on your mind, even just a few words.",
    "When the 5 minutes are up, decide if you still want to pick your phone back up."
)

/**
 * The one screen that answers "how am I doing right now", ordered by urgency rather than by
 * what happens to look good: anything outside the user's own range first, then the full
 * today-vs-average table, then the suggested action, then how much data those numbers rest on.
 *
 * [showRiskDialog] is driven by a stored epoch day, so the risk pop-up opens once on the first
 * view of a day and not every time the Home tab is re-selected.
 */
@Composable
fun HomeScreen(
    metrics: List<MetricSnapshot>,
    daysOfDataCollected: Int,
    collectedToday: CollectedToday,
    currentHour: CurrentHourSnapshot,
    showRiskDialog: Boolean,
    onRiskDialogDismissed: () -> Unit,
    onOpenMetric: (String) -> Unit,
    onOpenMetrics: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val risks = metrics.filter { it.concerning }
    var showWindDown by remember { mutableStateOf(false) }

    if (showRiskDialog && risks.isNotEmpty()) {
        RiskDialog(risks = risks, onOpenMetric = onOpenMetric, onDismiss = onRiskDialogDismissed)
    }
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
            confirmButton = { TextButton(onClick = { showWindDown = false }) { Text("Done") } }
        )
    }

    Column(Modifier.fillMaxSize().background(MelookColors.BackgroundLight)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenHeader(
                title = "Home",
                caption = if (metrics.isEmpty()) "$daysOfDataCollected days collected so far"
                // Smallest window across the signals, not the first row's: every metric drops its
                // own no-data days, so quoting one row's count overstates the rest.
                else "Today against your own average of ${metrics.minOf { it.daysCompared }}+ earlier days"
            )
            Column(Modifier.padding(horizontal = 20.dp)) {

                if (metrics.isEmpty()) {
                    SectionCard {
                        Text("Still building your baseline", color = MelookColors.TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Comparisons need a few days of your own history before they mean anything. " +
                                "$daysOfDataCollected days recorded so far — until then this screen shows what is being captured, not what it means.",
                            color = MelookColors.TextGray,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    if (risks.isNotEmpty()) {
                        RiskCard(risks = risks, onOpenMetric = onOpenMetric)
                        Spacer(Modifier.height(16.dp))
                    }

                    SectionLabel("TODAY VS YOUR AVERAGE")
                    SectionCard {
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("Signal", color = MelookColors.TextFaint, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("Today", color = MelookColors.TextFaint, fontSize = 11.sp, modifier = Modifier.width(74.dp))
                            Text("Change", color = MelookColors.TextFaint, fontSize = 11.sp, modifier = Modifier.width(64.dp))
                            Spacer(Modifier.width(14.dp))
                        }
                        metrics.forEach { metric ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenMetric(metric.key) }
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(metric.name, color = MelookColors.TextDark, fontSize = 13.sp)
                                    ChartInfoDot(metric.info)
                                }
                                Column(Modifier.width(74.dp)) {
                                    Text(metric.todayLabel, color = MelookColors.TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("was ${metric.baselineLabel}", color = MelookColors.TextFaint, fontSize = 11.sp)
                                }
                                Column(Modifier.width(64.dp)) {
                                    DeltaText(metric.deltaLabel, metric.concerning, metric.outsideUsualRange)
                                }
                                RowChevron()
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    SectionLabel("SUGGESTED NOW")
                    SectionCard(modifier = Modifier.clickable { showWindDown = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("5-minute wind-down", color = MelookColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (risks.isEmpty()) "Nothing is outside your usual range today. This is here whenever you want it."
                                    else "Suggested because ${risks.first().name.lowercase()} is outside your usual range.",
                                    color = MelookColors.TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            // The recommendation has no reference of its own, so this points at the
                            // source for the signal that triggered it rather than inventing one.
                            ChartInfoDot((risks.firstOrNull() ?: metrics.first()).info)
                            Spacer(Modifier.width(4.dp))
                            RowChevron()
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                SectionLabel("DATA COLLECTED")
                SectionCard {
                    KeyValueRow("Days recorded", "$daysOfDataCollected")
                    KeyValueRow("Typing sessions today", "${collectedToday.typingSessions}")
                    KeyValueRow("Screen-on today", collectedToday.screenTimeLabel)
                    KeyValueRow(
                        "This hour",
                        "${currentHour.keyPresses} keys · ${currentHour.backspaces} backspaces"
                    )
                    KeyValueRow(
                        "Last write",
                        if (currentHour.asOfMillis > 0)
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentHour.asOfMillis))
                        else "—"
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "See every signal ›",
                        color = MelookColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onOpenMetrics() }
                    )
                }

                DisclaimerLine()
            }
        }
        bottomBar()
    }
}

/** Top card whenever a signal sits outside the user's own range. Amber, not red: this is a
 * "worth a look" marker for a correlational signal, and the copy says so. */
@Composable
private fun RiskCard(risks: List<MetricSnapshot>, onOpenMetric: (String) -> Unit) {
    SectionCard(background = MelookColors.WarnSurface, border = MelookColors.WarnBorder) {
        Text(
            if (risks.size == 1) "1 signal needs attention" else "${risks.size} signals need attention",
            color = MelookColors.WarnText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Outside your own usual day-to-day range — not a clinical threshold.",
            color = MelookColors.WarnText,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        risks.forEach { risk ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenMetric(risk.key) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(risk.name, color = MelookColors.TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        ChartInfoDot(risk.info, tint = MelookColors.WarnText)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(risk.plainReading, color = MelookColors.TextGray, fontSize = 12.sp)
                    Text("Source: ${risk.sourceLabel}", color = MelookColors.TextFaint, fontSize = 11.sp)
                }
                RowChevron()
            }
        }
    }
}

/** First view of the day: the same risks as a modal, so a signal outside range is not missed
 * by a user who never scrolls. Dismissing it leaves the card in place on the screen behind. */
@Composable
private fun RiskDialog(risks: List<MetricSnapshot>, onOpenMetric: (String) -> Unit, onDismiss: () -> Unit) {
    var reference by remember { mutableStateOf<MetricSnapshot?>(null) }
    reference?.let { metric ->
        ChartInfoDialog(metric.info) { reference = null }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (risks.size == 1) "1 signal needs attention" else "${risks.size} signals need attention") },
        text = {
            Column {
                risks.forEach { risk ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenMetric(risk.key)
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(risk.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MelookColors.TextDark)
                            Spacer(Modifier.width(6.dp))
                            Text("read the source", fontSize = 11.sp, color = MelookColors.Accent, modifier = Modifier.clickable { reference = risk })
                        }
                        Text(risk.plainReading, fontSize = 12.sp, color = MelookColors.TextGray)
                        Text("Source: ${risk.sourceLabel}", fontSize = 11.sp, color = MelookColors.TextFaint)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Behaviour signals only. Not a diagnosis.",
                    fontSize = 11.sp,
                    color = MelookColors.TextFaint
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Review") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } }
    )
}

package com.example.mgc_keyboard.dashboard.charts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import java.net.URLEncoder

/** Citation strings cite paper titles but carry no URL/DOI we can verify here, so the tap
 * target opens a Google Scholar search for the first quoted title rather than risking a
 * hardcoded DOI that goes stale or was never verified. */
fun citationSearchUrl(citation: String): String {
    val title = Regex("\"([^\"]+)\"").find(citation)?.groupValues?.get(1) ?: citation
    return "https://scholar.google.com/scholar?q=" + URLEncoder.encode(title, "UTF-8")
}

/** What a chart shows, in plain language, plus the peer-reviewed source for why that signal
 * is tracked at all — so "what does this number mean" has a real answer instead of a bare
 * plot. [citation] is a paper we can point to for the association; it validates that the
 * *signal* is worth watching, not that this app diagnoses anything. */
data class ChartInfo(
    val whatItShows: String,
    val citation: String
)

/** Citations backing each dashboard metric — gathered via research-mode literature search.
 * Kept as plain strings (not links) since correlational findings, not diagnostic thresholds. */
object ChartCitations {
    val PHONE_SCHEDULE = ChartInfo(
        whatItShows = "How much of each hour/day your screen is on. Sustained changes in screen-on time and rest-activity rhythm are studied as passive markers of mood and anxiety state.",
        citation = "Place et al., \"Behavioral Indicators on a Mobile Sensing Platform Predict Clinically Validated Psychiatric Symptoms of Mood, Anxiety, and Psychosis\" (J Med Internet Res, 2017); Chikersal et al., \"Differential temporal utility of passively sensed smartphone features for depression and anxiety symptom prediction: A longitudinal cohort study\" (npj Mental Health Research, 2024) — screen/usage features linked to both PHQ and GAD symptom trajectories over time."
    )
    val BACKSPACE_RATE = ChartInfo(
        whatItShows = "Share of key presses that were backspace/delete. Higher edit/backspace rates while typing have been associated with higher depressive symptom scores in passive smartphone-sensing studies.",
        citation = "Liu, Vesel, Rashidisabet, Zulueta, et al., \"Digital phenotypes of mobile keyboard backspace rates and their associations with symptoms of mood disorder\" (JMIR, vol. 26, e51269, 2024); Cao et al., \"DeepMood: Modeling Mobile Phone Typing Dynamics for Mood Detection\" (KDD, 2017) — keystroke/backspace features correlated with PHQ-8/PHQ-9 severity; Ross et al., \"Associations Between Smartphone Keystroke Metadata and Mental Health Symptoms in Adolescents: Findings From the Future Proofing Study\" (JMIR Mental Health, 2023) — keystroke-timing/backspace patterns linked to depression, anxiety, and distress symptoms."
    )
    val TYPING_SENTIMENT = ChartInfo(
        whatItShows = "On-device sentiment score of the words you type (0=negative, 1=positive), scored locally — the text itself is never stored or sent anywhere.",
        citation = "Linguistic/sentiment shifts in typed or spoken language are a long-studied passive marker of depressive affect; see Eichstaedt et al., \"Facebook language predicts depression in medical records\" (PNAS, 2018) for the underlying language-affect association this on-device score is inspired by."
    )
    val APP_SWITCHING = ChartInfo(
        whatItShows = "How often you jump between different apps per day. App-switching frequency and fragmented phone use are studied as passive correlates of anxiety severity.",
        citation = "Rozgonjuk et al., \"Associations between symptoms of anxiety and depression, and smartphone app-switching behavior\" (2021); Jacobson, Summers, and Wilhelm, \"Automated screening for social anxiety, generalized anxiety, and depression from objective smartphone collected data: Cross sectional study\" (JMIR, vol. 23, no. 8, e28918, 2021) — smartphone usage-pattern features linked to GAD-7 scores."
    )
    val APP_VARIETY = ChartInfo(
        whatItShows = "Number of distinct apps used per day. A shrinking set of apps used (reduced variety) is one of the behavioral-withdrawal signals studied alongside depression severity.",
        citation = "Saeb et al., \"Mobile Phone Sensor Correlates of Depressive Symptom Severity in Daily-Life Behavior\" (JMIR, 2015) — app-usage diversity and phone-usage duration correlated with PHQ-9 scores."
    )
    val ACTIVITY_HEATMAP = ChartInfo(
        whatItShows = "Key presses per day over roughly the last 14 weeks — a long-range view of typing activity, darker means busier.",
        citation = "Long-window activity heatmaps make gradual trend shifts visible that a single day's numbers can hide; see Saeb et al. (JMIR, 2015) and Place et al. (JMIR, 2017) above for why sustained activity-level shifts are tracked at all."
    )
    val TYPING_PACE = ChartInfo(
        whatItShows = "Key presses per day this week, compared with your usual pace. A sustained slowdown or speedup in typing activity is one of the passive-sensing signals studied alongside mood changes.",
        citation = "Saeb et al., \"Mobile Phone Sensor Correlates of Depressive Symptom Severity in Daily-Life Behavior\" (JMIR, 2015)."
    )
    val TRENDS_SENTIMENT = ChartInfo(
        whatItShows = "Weekly typing-sentiment trend compared to your own baseline — 'more/less positive than usual' is relative to your last several weeks, not a population norm.",
        citation = "Eichstaedt et al., \"Facebook language predicts depression in medical records\" (PNAS, 2018)."
    )
}

/** Small "ⓘ" next to a chart title. Tap opens [info] as a dialog. Every plot on the "what we
 * track" screens should carry one of these — a chart with no legend, no reference, and no
 * citation is not something a user can trust their own mental-health data to. */
@Composable
fun ChartInfoButton(info: ChartInfo) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Default.Info, contentDescription = "What this chart means", tint = MelookColors.TextGray)
    }
    if (open) ChartInfoDialog(info) { open = false }
}

/** Same dialog, but a bare 16dp glyph instead of a 48dp [IconButton] — for list rows, where the
 * full touch target would push the row height past what a scannable table can afford. The row
 * itself stays tappable for its own action, so this only has to catch a deliberate tap on the
 * glyph. */
@Composable
fun ChartInfoDot(info: ChartInfo, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = MelookColors.TextFaint) {
    var open by remember { mutableStateOf(false) }
    Icon(
        Icons.Default.Info,
        contentDescription = "What this measures",
        tint = tint,
        modifier = modifier
            .padding(start = 6.dp)
            .size(15.dp)
            .clickable { open = true }
    )
    if (open) ChartInfoDialog(info) { open = false }
}

/** The reference pop-up itself: what the signal is, the peer-reviewed source behind tracking it,
 * a tap-through to that source, and the reminder that an association is not a diagnosis. */
@Composable
fun ChartInfoDialog(info: ChartInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What this chart means") },
        text = {
            Column {
                Text(info.whatItShows, fontSize = 13.sp, color = MelookColors.TextDark, modifier = Modifier.padding(bottom = 10.dp))
                Text("Research reference", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 12.sp, color = MelookColors.TextDark)
                Text(
                    info.citation,
                    fontSize = 11.sp,
                    color = MelookColors.Accent,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(citationSearchUrl(info.citation))))
                        }
                )
                Text(
                    "This links the signal to published research — it does not mean this app diagnoses or replaces a clinician.",
                    fontSize = 10.sp,
                    color = MelookColors.TextGray,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

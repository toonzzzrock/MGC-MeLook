package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.dashboard.MelookRoutes

/**
 * Shared shell for the three reading screens. Deliberately plain: a bordered white card, one
 * type scale, and colour spent only on values that sit outside the user's own range. Anything
 * decorative here reads as a claim the data cannot back.
 */

/** Screen title block. [caption] carries the window the numbers cover, so no figure on the
 * screen below is left without the period it was computed over. */
@Composable
fun ScreenHeader(title: String, caption: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MelookColors.TextDark, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(caption, color = MelookColors.TextFaint, fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    background: Color = MelookColors.Surface,
    border: Color = MelookColors.Border,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** Small all-caps section label above a card. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        color = MelookColors.TextFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * A signed change, as text rather than a filled pill. Grey unless the value is outside the
 * user's own range: a change inside normal day-to-day spread is not news and should not be
 * coloured like it is.
 */
@Composable
fun DeltaText(label: String, concerning: Boolean, outsideRange: Boolean, fontSize: Int = 13) {
    Text(
        label,
        color = when {
            concerning -> MelookColors.Negative
            outsideRange -> MelookColors.Positive
            else -> MelookColors.TextGray
        },
        fontSize = fontSize.sp,
        fontWeight = if (outsideRange) FontWeight.Medium else FontWeight.Normal
    )
}

/** Chevron marking a row that opens something. Tinted explicitly — a bare Icon here would
 * default to black and disappear against the dark-theme surface. */
@Composable
fun RowChevron() {
    Text("›", color = MelookColors.TextFaint, fontSize = 18.sp)
}

/** One-line footer stating what the whole app is and is not. Appears on every reading screen,
 * because a screen of clinical-sounding numbers without it invites the wrong reading. */
@Composable
fun DisclaimerLine(modifier: Modifier = Modifier) {
    Text(
        "Behaviour signals only. Not a diagnosis. All figures stay on this device.",
        color = MelookColors.TextFaint,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
    )
}

enum class MelookTab(val label: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.Default.Home, MelookRoutes.SUMMARY),
    TRENDS("Trends", Icons.Default.ShowChart, MelookRoutes.TRENDS),
    METRICS("Metrics", Icons.Default.BarChart, MelookRoutes.METRICS),
    SETTINGS("Settings", Icons.Default.Settings, MelookRoutes.SETTINGS)
}

/** Flat bottom nav: four destinations, always visible, so "what are these pages for" is
 * answered by the app itself instead of by hunting for icons in a corner. */
@Composable
fun MelookBottomBar(current: MelookTab, onSelect: (MelookTab) -> Unit) {
    Column {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(MelookColors.Border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MelookColors.Surface)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MelookTab.values().forEach { tab ->
                val selected = tab == current
                val tint = if (selected) MelookColors.Accent else MelookColors.TextFaint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(tab) }.padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/** Label/value line inside a card, e.g. the "Data collected" block. */
@Composable
fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MelookColors.TextGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, color = MelookColors.TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

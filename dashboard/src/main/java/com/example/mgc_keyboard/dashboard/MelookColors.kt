package com.example.mgc_keyboard.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object MelookColors {

    /**
     * Light/dark switch for the whole app. Set from [AppPrefsState.darkTheme] in [MelookNavHost];
     * flipped by the Customize screen. Snapshot state, so every screen that reads a themed token
     * below recomposes on change without any of them having to observe anything.
     */
    var dark by mutableStateOf(false)

    // Brand colours — identical in both themes on purpose. DashboardViewModel bakes these into
    // Bar/ChartPoint data outside composition, so a themed value here would go stale until the
    // next ViewModel refresh. Don't turn them into getters.
    val Accent = Color(0xFF3B6FF2)
    val Amber = Color(0xFFE0A63C)
    val Green = Color(0xFF1FAA59)
    val Navy = Color(0xFF0A1428)
    val NavyCard = Color(0xFF13213D)
    val BubbleIncoming = Color(0xFF223252)

    // Themed surface/text tokens.
    val Surface get() = if (dark) Color(0xFF0E141F) else Color(0xFFFFFFFF)
    val BackgroundLight get() = if (dark) Color(0xFF18202E) else Color(0xFFF7F8FA)
    val AccentSoft get() = if (dark) Color(0xFF1B2A47) else Color(0xFFE9EFFE)
    val TextDark get() = if (dark) Color(0xFFEDF1F7) else Color(0xFF14151A)
    val TextGray get() = if (dark) Color(0xFF9AA4B4) else Color(0xFF8A93A3)
    val Divider get() = if (dark) Color(0xFF243044) else Color(0xFFECEEF3)
}

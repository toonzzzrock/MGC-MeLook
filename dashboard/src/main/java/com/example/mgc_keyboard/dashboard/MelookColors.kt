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

    // Chart-series colours. Same rule as the brand colours above: baked into Bar/ChartPoint
    // by DashboardViewModel outside composition, so these stay plain vals in both themes.
    // Neutral is the default series colour — a bar only takes [SeriesFlagged] when the value
    // it carries sits outside the user's own range, so colour means something.
    val SeriesNeutral = Color(0xFF6E7A8C)
    val SeriesFlagged = Color(0xFFA33B22)

    // Themed surface/text tokens.
    val Surface get() = if (dark) Color(0xFF0E141F) else Color(0xFFFFFFFF)
    val BackgroundLight get() = if (dark) Color(0xFF18202E) else Color(0xFFF7F8FA)
    val AccentSoft get() = if (dark) Color(0xFF1B2A47) else Color(0xFFE9EFFE)
    val TextDark get() = if (dark) Color(0xFFEDF1F7) else Color(0xFF14151A)
    val TextGray get() = if (dark) Color(0xFF9AA4B4) else Color(0xFF8A93A3)
    val Divider get() = if (dark) Color(0xFF243044) else Color(0xFFECEEF3)

    // Reference/measurement palette used by Home, Metrics and Metric detail. Muted on purpose:
    // the reading screens carry numbers and sources, so colour is spent only on direction of
    // change and on the risk card, never on decoration.
    val TextFaint get() = if (dark) Color(0xFF6D7787) else Color(0xFF8B94A1)
    val Border get() = if (dark) Color(0xFF29354A) else Color(0xFFE4E7EC)
    val Positive get() = if (dark) Color(0xFF4FBF8B) else Color(0xFF1B6B4A)
    val Negative get() = if (dark) Color(0xFFE08163) else Color(0xFFA33B22)
    val WarnText get() = if (dark) Color(0xFFE8C070) else Color(0xFF9A6B12)
    val WarnSurface get() = if (dark) Color(0xFF2B2415) else Color(0xFFFFF6E3)
    val WarnBorder get() = if (dark) Color(0xFF4A3D1E) else Color(0xFFEBD9AE)
}

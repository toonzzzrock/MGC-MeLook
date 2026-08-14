package com.example.mgc_keyboard.dashboard

import com.example.mgc_keyboard.dashboard.charts.Bar
import com.example.mgc_keyboard.dashboard.charts.ChartInfo
import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import kotlin.math.abs
import kotlin.math.sqrt

/** Metric keys, also the argument of the metric-detail route. */
object MetricKeys {
    const val SENTIMENT = "typing_sentiment"
    const val SCREEN_TIME = "screen_time"
    const val APP_SWITCHING = "app_switching"
    const val BACKSPACE = "backspace_rate"
    const val APP_VARIETY = "app_variety"
    const val QUIET = "quiet_stretches"
    const val HEATMAP = "activity_heatmap"
}

/**
 * One tracked signal, today's value next to the user's own recent average, with everything the
 * reading screens need: the number, the comparison, the source line, and the chart series.
 *
 * [outsideUsualRange] is deliberately defined against the user's own day-to-day spread (more
 * than one standard deviation from their own mean, computed over the days before today) rather
 * than any published cut-off — there is no diagnostic threshold for these signals, and the
 * screens say so.
 */
data class MetricSnapshot(
    val key: String,
    val name: String,
    val todayLabel: String,
    val baselineLabel: String,
    val deltaLabel: String,
    val higher: Boolean,
    val outsideUsualRange: Boolean,
    /** Outside the range *and* moved in the direction the literature associates with symptoms. */
    val concerning: Boolean,
    /** Distance from the user's mean in standard deviations — what Home orders rows by. */
    val deviations: Float,
    val daysCompared: Int,
    val unitCaption: String,
    val sourceLabel: String,
    val howMeasured: String,
    val plainReading: String,
    val info: ChartInfo,
    val bars: List<Bar> = emptyList(),
    val points: List<ChartPoint> = emptyList()
)

/**
 * Builds a snapshot from an ascending per-day series. Today is the last entry; the mean and
 * spread come from every earlier day in the window, so today is compared against a baseline it
 * is not itself part of. Returns null when there are too few earlier days to say anything.
 */
internal fun metricFrom(
    key: String,
    name: String,
    series: List<Pair<String, Float>>,
    format: (Float) -> String,
    deltaFormat: (Float) -> String,
    higherIsConcerning: Boolean,
    unitCaption: String,
    sourceLabel: String,
    howMeasured: String,
    info: ChartInfo,
    asLine: Boolean = false
): MetricSnapshot? {
    if (series.size < 4) return null
    val today = series.last().second
    val prior = series.dropLast(1).map { it.second }
    val mean = prior.average().toFloat()
    val sd = sqrt(prior.map { (it - mean) * (it - mean) }.average()).toFloat()
    val delta = today - mean
    // A flat series (sd == 0) would flag any change at all as unusual, so it stays unflagged.
    val deviations = if (sd > 0f) abs(delta) / sd else 0f
    val outside = prior.size >= 5 && sd > 0f && deviations > 1f
    val higher = delta >= 0f
    val concerning = outside && (higher == higherIsConcerning)

    val recent = series.takeLast(7)
    val peak = recent.maxOf { it.second }.coerceAtLeast(0.0001f)
    val bars = if (asLine) emptyList() else recent.mapIndexed { index, (label, value) ->
        val isToday = index == recent.lastIndex
        Bar(
            heightFraction = (value / peak).coerceIn(0.02f, 1f),
            color = if (isToday && concerning) MelookColors.SeriesFlagged else MelookColors.SeriesNeutral,
            label = label,
            value = value
        )
    }
    val points = if (asLine) recent.map { (label, value) -> ChartPoint(value = value, label = label) } else emptyList()

    return MetricSnapshot(
        key = key,
        name = name,
        todayLabel = format(today),
        baselineLabel = format(mean),
        deltaLabel = (if (higher) "+" else "−") + deltaFormat(abs(delta)),
        higher = higher,
        outsideUsualRange = outside,
        concerning = concerning,
        deviations = deviations,
        daysCompared = prior.size,
        unitCaption = unitCaption,
        sourceLabel = sourceLabel,
        howMeasured = howMeasured,
        plainReading = "${format(today)} today vs ${format(mean)} on your last ${prior.size} days",
        info = info,
        bars = bars,
        points = points
    )
}

fun formatMinutes(minutes: Float): String {
    val total = minutes.toInt().coerceAtLeast(0)
    return if (total >= 60) "${total / 60}h ${total % 60}m" else "${total}m"
}

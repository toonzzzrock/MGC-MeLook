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
 * Where today sits on the user's own scale, as fractions of a 0..1 strip: the band is their usual
 * range (one standard deviation either side of their mean), the marker is today. Lets a row answer
 * "is this normal for me" without opening anything.
 */
data class UsualRange(
    val bandStart: Float,
    val bandEnd: Float,
    val today: Float
)

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
    /** Too few hours of the day recorded to judge a running total either way. Distinct from
     * "within range": the screens must not call it normal variation when nothing was tested. */
    val tooEarlyToJudge: Boolean,
    /** Distance from the user's mean in standard deviations — what Home orders rows by. */
    val deviations: Float,
    val daysCompared: Int,
    /** What the average column is an average *of* — spells out the same-hours window when the
     * day is still running, so a morning figure is never labelled as a whole day. */
    val baselineCaption: String,
    val unitCaption: String,
    val sourceLabel: String,
    val howMeasured: String,
    val plainReading: String,
    val info: ChartInfo,
    val usualRange: UsualRange,
    val bars: List<Bar> = emptyList(),
    val points: List<ChartPoint> = emptyList()
)

/**
 * Truncates every earlier day to the hour of day the last day has reached, so a total that
 * accumulates through the day is compared against the same slice of earlier days. Without it a
 * part-day "today" reads far below every full day all morning, and app variety — where a drop is
 * the worry — would raise a risk every day before noon purely because the day is not over.
 * Ratio metrics (backspace share, mean sentiment) do not accumulate and are left alone.
 */
/** Hours of a day that must be recorded before an accumulating total may be called unusual. */
internal const val MIN_HOURS_TO_FLAG = 6

internal fun <T> alignToPartialDay(daysAsc: List<List<T>>, hourOfDay: (T) -> Int): List<List<T>> {
    val cutoff = daysAsc.lastOrNull()?.maxOfOrNull(hourOfDay) ?: return daysAsc
    return daysAsc.map { day -> day.filter { hourOfDay(it) <= cutoff } }
}

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
    asLine: Boolean = false,
    /** Hours of the day behind the figures, when this is an accumulating total aligned by
     * [alignToPartialDay]; null for ratio metrics and for a day that has run its full 24 hours. */
    partialDayHours: Int? = null
): MetricSnapshot? {
    if (series.size < 4) return null
    val today = series.last().second
    val prior = series.dropLast(1).map { it.second }
    val mean = prior.average().toFloat()
    val sd = sqrt(prior.map { (it - mean) * (it - mean) }.average()).toFloat()
    val delta = today - mean
    // A flat series (sd == 0) would flag any change at all as unusual, so it stays unflagged.
    val enoughOfTheDay = partialDayHours == null || partialDayHours >= MIN_HOURS_TO_FLAG
    // Zero while the day is too young: this is the key Home orders rows by, and a thin window's
    // inflated distance would still rank a non-finding at the top of an urgency-ordered page.
    val deviations = if (sd > 0f && enoughOfTheDay) abs(delta) / sd else 0f
    // A few hours of a running total carry almost no spread, so an ordinary first coffee can sit
    // several deviations above a near-zero mean. Aligned totals only get to claim "unusual" once
    // enough of the day is behind them; the number itself still shows from the first hour.
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
        tooEarlyToJudge = !enoughOfTheDay,
        deviations = deviations,
        daysCompared = prior.size,
        baselineCaption = if (partialDayHours == null) "${prior.size}-day average"
        else "${prior.size}-day average, same hours",
        unitCaption = unitCaption,
        sourceLabel = sourceLabel,
        howMeasured = howMeasured,
        plainReading = if (partialDayHours == null)
            "${format(today)} today vs ${format(mean)} on your last ${prior.size} days"
        else
            "${format(today)} so far today vs ${format(mean)} by this time on your last ${prior.size} days",
        info = info,
        usualRange = usualRange(prior, today, mean, sd),
        bars = bars,
        points = points
    )
}

/**
 * Positions the usual-range band and today's marker on a shared 0..1 strip. The strip spans
 * everything the user has actually done in the window plus the band itself, so today always lands
 * inside the drawing whether it sits in the band, at an edge, or well past one.
 */
private fun usualRange(prior: List<Float>, today: Float, mean: Float, sd: Float): UsualRange {
    val low = minOf(prior.minOrNull() ?: today, today, mean - sd)
    val high = maxOf(prior.maxOrNull() ?: today, today, mean + sd)
    val span = high - low
    // One value repeated, or a single day: centre the band and put today on it rather than
    // dividing by zero and drawing a strip that means nothing.
    if (span <= 0f) return UsualRange(bandStart = 0.4f, bandEnd = 0.6f, today = 0.5f)
    fun at(value: Float) = ((value - low) / span).coerceIn(0f, 1f)
    return UsualRange(bandStart = at(mean - sd), bandEnd = at(mean + sd), today = at(today))
}

fun formatMinutes(minutes: Float): String {
    val total = minutes.toInt().coerceAtLeast(0)
    return if (total >= 60) "${total / 60}h ${total % 60}m" else "${total}m"
}

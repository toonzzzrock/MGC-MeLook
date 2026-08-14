package com.example.mgc_keyboard.dashboard

import com.example.mgc_keyboard.dashboard.charts.ChartCitations
import com.example.mgc_keyboard.dashboard.charts.ChartInfo
import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import com.example.mgc_keyboard.statscore.HourlyStat
import com.example.mgc_keyboard.statscore.averageSentiment
import com.example.mgc_keyboard.statscore.backspaceRate
import com.example.mgc_keyboard.statscore.dayBucket
import com.example.mgc_keyboard.statscore.isInactive
import java.time.LocalDate
import java.util.Locale
import kotlin.math.sqrt

/** One signal drawn over every recorded day, with the user's own usual range behind it. */
data class TrendSeries(
    val key: String,
    val name: String,
    val info: ChartInfo,
    val points: List<ChartPoint>,
    val maxValue: Float,
    val band: ClosedFloatingPointRange<Float>?,
    val caption: String,
    val summary: String,
    val format: (Float) -> String
)

/** A day with nothing recorded for a signal is absent from that signal's series, never zero and
 * never a filled-in midpoint. The chart spaces points by date, so absence shows as a gap. */
private fun dayLabel(dayBucket: Long): String = LocalDate.ofEpochDay(dayBucket).let {
    "${it.dayOfMonth}/${it.monthValue}"
}

private const val RECENT_WINDOW = 7

/**
 * Long-range series for every tracked signal. Today is dropped: it is a part-day everywhere and
 * would dip the last point of four of the six signals for reasons that have nothing to do with
 * the user. Trends is the slow view; Home covers today.
 */
fun buildTrendSeries(byDay: Map<Long, List<HourlyStat>>): List<TrendSeries> {
    val completeDays = byDay.entries.sortedBy { it.key }.dropLast(1)
    if (completeDays.size < 2) return emptyList()

    fun series(
        key: String,
        name: String,
        info: ChartInfo,
        caption: String,
        format: (Float) -> String,
        scoreScale: Boolean = false,
        value: (List<HourlyStat>) -> Float?
    ): TrendSeries? {
        val recorded = completeDays.mapNotNull { (day, stats) -> value(stats)?.let { day to it } }
        if (recorded.size < 2) return null

        val values = recorded.map { it.second }
        val mean = values.average().toFloat()
        val sd = sqrt(values.map { (it - mean) * (it - mean) }.average()).toFloat()
        val firstDay = recorded.first().first

        return TrendSeries(
            key = key,
            name = name,
            info = info,
            points = recorded.map { (day, v) ->
                ChartPoint(value = v, label = dayLabel(day), dayOffset = (day - firstDay).toInt())
            },
            // Headroom so the top point is not welded to the frame; scores keep their own 0..1.
            maxValue = if (scoreScale) 1f else maxOf(values.max(), mean + sd) * 1.1f,
            band = if (sd > 0f) (mean - sd).coerceAtLeast(0f)..(mean + sd) else null,
            caption = caption,
            summary = summarise(values, format),
            format = format
        )
    }

    return listOfNotNull(
        series(
            key = MetricKeys.SENTIMENT,
            name = "Typing sentiment",
            info = ChartCitations.TRENDS_SENTIMENT,
            caption = "0 = negative, 1 = positive",
            format = { String.format(Locale.US, "%.2f", it) },
            scoreScale = true
        ) { day -> day.mapNotNull { it.averageSentiment() }.takeIf { it.isNotEmpty() }?.average()?.toFloat() },
        series(
            key = MetricKeys.SCREEN_TIME,
            name = "Screen-on time",
            info = ChartCitations.PHONE_SCHEDULE,
            caption = "Minutes with the screen on",
            format = { formatMinutes(it) }
        ) { day -> (day.sumOf { it.screenTimeMillis } / 60_000f).takeIf { it > 0f } },
        series(
            key = MetricKeys.APP_SWITCHING,
            name = "App switching",
            info = ChartCitations.APP_SWITCHING,
            caption = "Moves between apps",
            format = { "${it.toInt()}" }
        ) { day -> day.sumOf { it.appSwitchCount }.toFloat().takeIf { it > 0f } },
        series(
            key = MetricKeys.BACKSPACE,
            name = "Backspace rate",
            info = ChartCitations.BACKSPACE_RATE,
            caption = "Share of key presses that were backspace",
            format = { String.format(Locale.US, "%.1f%%", it) }
        ) { day ->
            day.filter { it.totalKeyPresses > 0 }.map { it.backspaceRate() }
                .takeIf { it.isNotEmpty() }?.average()?.times(100)?.toFloat()
        },
        series(
            key = MetricKeys.APP_VARIETY,
            name = "App variety",
            info = ChartCitations.APP_VARIETY,
            caption = "Distinct apps opened",
            format = { "${it.toInt()}" }
        ) { day -> day.sumOf { it.distinctAppCount }.toFloat().takeIf { it > 0f } },
        series(
            key = MetricKeys.QUIET,
            name = "Quiet stretches",
            info = ChartCitations.PHONE_SCHEDULE,
            caption = "Hours with no typing and no screen time",
            format = { "${it.toInt()}h" }
        ) { day -> day.count { it.isInactive() }.toFloat().takeIf { day.isNotEmpty() } }
    )
}

/**
 * The last week against everything before it, in the user's own spread. Half a standard deviation
 * is the smallest move worth a sentence — below that the two weeks are the same week with noise.
 */
internal fun summarise(values: List<Float>, format: (Float) -> String): String {
    val recent = values.takeLast(RECENT_WINDOW)
    val earlier = values.dropLast(recent.size)
    if (earlier.size < RECENT_WINDOW) return "Not enough earlier days yet to compare this week against."

    val earlierMean = earlier.average().toFloat()
    val earlierSd = sqrt(earlier.map { (it - earlierMean) * (it - earlierMean) }.average()).toFloat()
    val recentMean = recent.average().toFloat()
    val direction = when {
        earlierSd <= 0f || kotlin.math.abs(recentMean - earlierMean) < earlierSd / 2f -> "about the same as"
        recentMean > earlierMean -> "higher than"
        else -> "lower than"
    }
    return "Last ${recent.size} days average ${format(recentMean)}, $direction your earlier days (${format(earlierMean)})."
}

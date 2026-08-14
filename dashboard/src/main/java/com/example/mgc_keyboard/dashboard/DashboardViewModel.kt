package com.example.mgc_keyboard.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mgc_keyboard.dashboard.charts.Bar
import com.example.mgc_keyboard.dashboard.charts.ChartCitations
import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import com.example.mgc_keyboard.dashboard.charts.HeatmapDay
import com.example.mgc_keyboard.statscore.BehavioralBaseline
import com.example.mgc_keyboard.statscore.HourlyStat
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsRepository
import com.example.mgc_keyboard.statscore.averageSentiment
import com.example.mgc_keyboard.statscore.backspaceRate
import com.example.mgc_keyboard.statscore.dayBucket
import com.example.mgc_keyboard.statscore.isInactive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** "Mon", "Tue", ... for the day the given HourlyStat batch falls on (all rows in a batch
 * share the same day bucket, so the first row's date is enough). */
private fun List<HourlyStat>.weekdayLabel(): String =
    firstOrNull()?.let { LocalDate.ofEpochDay(it.dayBucket()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) } ?: ""

/** Blank for most hours — labelling all 24 bars crowds the axis unreadable, so only every
 * 3rd hour gets a tick, same spacing GitHub/most chart libraries use for dense category axes. */
/** "13/8" for the day the batch falls on. The trend chart spans two weeks, so weekday names
 * would repeat and tell the user nothing about which Tuesday they are looking at. */
private fun List<HourlyStat>.dayMonthLabel(): String =
    firstOrNull()?.let {
        val date = LocalDate.ofEpochDay(it.dayBucket())
        "${date.dayOfMonth}/${date.monthValue}"
    } ?: ""

private fun hourOfDayLabel(hour: Int): String {
    if (hour % 3 != 0) return ""
    return when (hour) {
        0 -> "12a"
        12 -> "12p"
        in 1..11 -> "${hour}a"
        else -> "${hour - 12}p"
    }
}

/** Compares the last 7 days of the trend window against baseline sentiment, so the label on
 * TrendsScreen describes what the plotted line actually shows instead of a fixed string. */
private fun trendDirectionLabel(byDay: Map<Long, List<HourlyStat>>, baseline: BehavioralBaseline?): String {
    if (baseline == null || byDay.size < 14) return ""
    val recentSentiment = byDay.entries.sortedByDescending { it.key }.take(7)
        .flatMap { it.value }.mapNotNull { it.averageSentiment() }.average()
        .let { if (it.isNaN()) return "" else it }
    val delta = recentSentiment - baseline.avgSentiment
    return when {
        delta > 0.05 -> "more positive than usual"
        delta < -0.05 -> "less positive than usual"
        else -> "about the same as usual"
    }
}

data class CollectedToday(
    val typingSessions: Int,
    val screenTimeLabel: String,
    val appsUsed: Int,
    val quietStretches: Int
)

/** The current (still-filling) hour bucket, straight from the same Room Flow every other
 * chart reads — so this ticks up live as you type, letting you verify a keystroke actually
 * got recorded instead of just trusting the app. */
data class CurrentHourSnapshot(
    val keyPresses: Int = 0,
    val backspaces: Int = 0,
    val wordsScored: Int = 0,
    val appSwitches: Int = 0,
    val asOfMillis: Long = 0L
)

data class DashboardUiState(
    val daysOfDataCollected: Int = 0,
    val collectedToday: CollectedToday = CollectedToday(0, "0m", 0, 0),
    val hasEnoughWeeksForTrend: Boolean = false,
    val trendPoints: List<ChartPoint> = emptyList(),
    val trendDirectionLabel: String = "",
    val quietStretchHours: Float = 0f,
    val quietStretchIncreased: Boolean = false,
    // Screen-on time detail has 2 view modes sharing this data: hourly shape over the last 7
    // days, and a per-day total across the last month.
    val hourlyActivityPattern: List<Bar> = emptyList(),
    val dailyActivityPatternMonth: List<Bar> = emptyList(),
    val heatmapDays: List<HeatmapDay> = emptyList(),
    val currentHour: CurrentHourSnapshot = CurrentHourSnapshot(),
    /** Every tracked signal as today-vs-own-average, ordered most-deviating first. Home, the
     * metrics list and the metric detail all read this one list. */
    val metrics: List<MetricSnapshot> = emptyList()
)

/**
 * Today vs the user's own recent average for every tracked signal. One ascending per-day series
 * per metric; days with nothing recorded for that metric are dropped rather than counted as
 * zero, which would drag the average down and manufacture a deviation.
 */
private fun buildMetrics(byDay: Map<Long, List<HourlyStat>>): List<MetricSnapshot> {
    val daysAsc = byDay.entries.sortedBy { it.key }.map { it.value }

    fun series(value: (List<HourlyStat>) -> Float?): List<Pair<String, Float>> =
        daysAsc.mapNotNull { day -> value(day)?.let { day.weekdayLabel() to it } }

    val metrics = listOfNotNull(
        metricFrom(
            key = MetricKeys.SENTIMENT,
            name = "Typing sentiment",
            series = series { day ->
                day.mapNotNull { it.averageSentiment() }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            },
            format = { String.format(Locale.US, "%.2f", it) },
            deltaFormat = { String.format(Locale.US, "%.2f", it) },
            higherIsConcerning = false,
            unitCaption = "0 = negative, 1 = positive",
            sourceLabel = "Eichstaedt et al., 2018",
            howMeasured = "Every word you type is scored on-device against a sentiment lexicon and averaged per day. The text itself is never stored or sent anywhere — only the score.",
            info = ChartCitations.TYPING_SENTIMENT,
            asLine = true
        ),
        metricFrom(
            key = MetricKeys.SCREEN_TIME,
            name = "Screen-on time",
            series = series { day -> (day.sumOf { it.screenTimeMillis } / 60_000f).takeIf { it > 0f } },
            format = { formatMinutes(it) },
            deltaFormat = { formatMinutes(it) },
            higherIsConcerning = true,
            unitCaption = "Total time the screen was on, per day",
            sourceLabel = "Place et al., 2017",
            howMeasured = "Screen-on time is summed per hour from the system usage tracker, then totalled per calendar day.",
            info = ChartCitations.PHONE_SCHEDULE
        ),
        metricFrom(
            key = MetricKeys.APP_SWITCHING,
            name = "App switching",
            series = series { day -> day.sumOf { it.appSwitchCount }.toFloat() },
            format = { "${it.toInt()}" },
            deltaFormat = { "${it.toInt()}" },
            higherIsConcerning = true,
            unitCaption = "Times you moved between apps, per day",
            sourceLabel = "Rozgonjuk et al., 2021",
            howMeasured = "Each time the foreground app changes it is counted once, and the counts are totalled per day.",
            info = ChartCitations.APP_SWITCHING
        ),
        metricFrom(
            key = MetricKeys.BACKSPACE,
            name = "Backspace rate",
            series = series { day ->
                day.filter { it.totalKeyPresses > 0 }.map { it.backspaceRate() }
                    .takeIf { it.isNotEmpty() }?.average()?.times(100)?.toFloat()
            },
            format = { String.format(Locale.US, "%.1f%%", it) },
            deltaFormat = { String.format(Locale.US, "%.1f pp", it) },
            higherIsConcerning = true,
            unitCaption = "Share of key presses that were backspace",
            sourceLabel = "Liu et al., 2024",
            howMeasured = "Backspace presses divided by all key presses, averaged over the hours you typed in that day. Hours with no typing are excluded.",
            info = ChartCitations.BACKSPACE_RATE
        ),
        metricFrom(
            key = MetricKeys.APP_VARIETY,
            name = "App variety",
            series = series { day -> day.sumOf { it.distinctAppCount }.toFloat() },
            format = { "${it.toInt()}" },
            deltaFormat = { "${it.toInt()}" },
            higherIsConcerning = false,
            unitCaption = "Distinct apps opened, per day",
            sourceLabel = "Saeb et al., 2015",
            howMeasured = "Distinct apps seen in each hour, totalled per day. A narrowing set of apps is the withdrawal signal this tracks.",
            info = ChartCitations.APP_VARIETY
        ),
        metricFrom(
            key = MetricKeys.QUIET,
            name = "Quiet stretches",
            series = series { day -> day.count { it.isInactive() }.toFloat() },
            format = { "${it.toInt()}h" },
            deltaFormat = { "${it.toInt()}h" },
            higherIsConcerning = true,
            unitCaption = "Hours with no typing and no screen time",
            sourceLabel = "Place et al., 2017",
            howMeasured = "An hour counts as quiet when it recorded no key presses and no screen-on time. This counts those hours per day.",
            info = ChartCitations.PHONE_SCHEDULE
        )
    )
    return metrics.sortedByDescending { it.deviations }
}

/** US3-1/2/5: reads StatsRepository + BehavioralBaseline and derives the numbers each screen
 * shows, instead of the screens holding hardcoded demo data. */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatsRepository.from(StatsDatabase.getInstance(application))

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        combine(
            repository.observeRecentHours(24 * 14),
            repository.observeBaseline(),
            // Wider, separate window purely for the heatmap so it doesn't distort the
            // 14-day-windowed metrics (trendPoints etc.) computed from the first flow.
            repository.observeRecentHours(24 * 98)
        ) { recentHours, baseline, heatmapHours -> Triple(recentHours, baseline, heatmapHours) }
            .onEach { (recentHours, baseline, heatmapHours) -> update(recentHours, baseline, heatmapHours) }
            .launchIn(viewModelScope)
    }

    private fun update(recentHours: List<HourlyStat>, baseline: BehavioralBaseline?, heatmapHours: List<HourlyStat>) {
        val byDay = recentHours.groupBy { it.dayBucket() }
        val today = byDay.values.maxByOrNull { day -> day.maxOf { it.hourBucket } }.orEmpty()

        val screenTimeMs = today.sumOf { it.screenTimeMillis }
        val hours = screenTimeMs / 3_600_000
        val minutes = (screenTimeMs % 3_600_000) / 60_000

        val collectedToday = CollectedToday(
            typingSessions = today.count { it.totalKeyPresses > 0 },
            screenTimeLabel = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
            appsUsed = today.sumOf { it.distinctAppCount },
            quietStretches = today.count { it.totalKeyPresses == 0 && it.screenTimeMillis == 0L }
        )

        val longestInactiveStretchHours = recentHours.sortedBy { it.hourBucket }
            .fold(0 to 0) { (current, longest), stat ->
                if (stat.isInactive()) (current + 1) to maxOf(longest, current + 1) else 0 to longest
            }.second

        // Phone on/off schedule, mode 1: average screen time per hour-of-day (0-23) across
        // the last 7 days only, normalized to the busiest hour so the shape of the day is
        // visible regardless of absolute usage.
        val last7DayBuckets = byDay.entries.sortedByDescending { it.key }.take(7).map { it.key }.toSet()
        val last7DaysHours = recentHours.filter { it.dayBucket() in last7DayBuckets }
        val byHourOfDay = last7DaysHours.groupBy { (it.hourBucket % 24).toInt() }
        val maxAvgScreenTime = (0..23).maxOfOrNull { hour ->
            byHourOfDay[hour]?.map { it.screenTimeMillis }?.average() ?: 0.0
        }?.coerceAtLeast(1.0) ?: 1.0
        val hourlyActivityPattern = (0..23).map { hour ->
            val avg = byHourOfDay[hour]?.map { it.screenTimeMillis }?.average() ?: 0.0
            val fraction = (avg / maxAvgScreenTime).toFloat().coerceIn(0f, 1f)
            Bar(
                heightFraction = fraction.coerceAtLeast(0.05f),
                color = MelookColors.SeriesNeutral,
                label = hourOfDayLabel(hour),
                value = (avg / 60_000.0).toFloat() // minutes of screen time
            )
        }

        // Phone on/off schedule, mode 2: total screen time per calendar day across the last
        // month, drawn from the wider heatmap window so it isn't capped at 14 days.
        val byDayMonth = heatmapHours.groupBy { it.dayBucket() }
        val lastMonthDaysAsc = byDayMonth.entries.sortedByDescending { it.key }.take(30).map { it }.reversed()
        val maxDailyScreenTime = lastMonthDaysAsc.maxOfOrNull { (_, hours) -> hours.sumOf { it.screenTimeMillis } }
            ?.coerceAtLeast(1) ?: 1
        val dailyActivityPatternMonth = lastMonthDaysAsc.map { (dayEpoch, hours) ->
            val total = hours.sumOf { it.screenTimeMillis }
            val fraction = (total.toFloat() / maxDailyScreenTime).coerceIn(0.05f, 1f)
            Bar(
                heightFraction = fraction,
                color = MelookColors.SeriesNeutral,
                label = LocalDate.ofEpochDay(dayEpoch).dayOfMonth.toString(),
                value = (total / 60_000.0).toFloat() // minutes of screen time
            )
        }

        val currentHourStat = today.maxByOrNull { it.hourBucket }
        val currentHour = CurrentHourSnapshot(
            keyPresses = currentHourStat?.totalKeyPresses ?: 0,
            backspaces = currentHourStat?.backspacePresses ?: 0,
            wordsScored = currentHourStat?.wordsScored ?: 0,
            appSwitches = currentHourStat?.appSwitchCount ?: 0,
            asOfMillis = System.currentTimeMillis()
        )

        val heatmapDays = heatmapHours.groupBy { it.dayBucket() }.map { (dayEpoch, hours) ->
            HeatmapDay(dayEpoch = dayEpoch, value = hours.sumOf { it.totalKeyPresses }.toFloat())
        }

        _state.value = DashboardUiState(
            daysOfDataCollected = byDay.size,
            collectedToday = collectedToday,
            hasEnoughWeeksForTrend = byDay.size >= 14,
            trendPoints = if (byDay.size >= 14) {
                byDay.entries.sortedBy { it.key }.map { (_, day) ->
                    val score = day.mapNotNull { it.averageSentiment() }.average()
                        .let { if (it.isNaN()) 0.5f else it.toFloat() }
                        .coerceIn(0f, 1f)
                    ChartPoint(value = score, label = day.dayMonthLabel())
                }
            } else emptyList(),
            trendDirectionLabel = trendDirectionLabel(byDay, baseline),
            quietStretchHours = longestInactiveStretchHours.toFloat(),
            quietStretchIncreased = baseline != null && longestInactiveStretchHours > baseline.avgLongestInactiveStretchHours,
            hourlyActivityPattern = hourlyActivityPattern,
            dailyActivityPatternMonth = dailyActivityPatternMonth,
            heatmapDays = heatmapDays,
            currentHour = currentHour,
            metrics = buildMetrics(byDay)
        )
    }
}

package com.example.mgc_keyboard.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mgc_keyboard.dashboard.charts.Bar
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

data class CollectedToday(
    val typingSessions: Int,
    val screenTimeLabel: String,
    val appsUsed: Int,
    val quietStretches: Int
)

data class DashboardUiState(
    val daysOfDataCollected: Int = 0,
    val collectedToday: CollectedToday = CollectedToday(0, "0m", 0, 0),
    val hasBaseline: Boolean = false,
    val paceChangePercent: Int = 0,
    val weekBars: List<Bar> = emptyList(),
    val lateNightChangePercent: Int = 0,
    val appVarietyLower: Boolean = true,
    val showSuggestion: Boolean = false,
    val hasEnoughWeeksForTrend: Boolean = false,
    val trendPoints: List<Float> = emptyList(),
    val quietStretchHours: Float = 0f,
    val quietStretchIncreased: Boolean = false,
    // All-stats screen: every collected signal, independent of the 14-day trend gate above.
    val hourlyActivityPattern: List<Float> = emptyList(),
    val backspaceRateBars: List<Bar> = emptyList(),
    val sentimentTrendRecent: List<Float> = emptyList(),
    val appSwitchBars: List<Bar> = emptyList(),
    val appVarietyBars: List<Bar> = emptyList(),
    val totalKeyPressesToday: Int = 0,
    val totalBackspacesToday: Int = 0,
    val totalWordsScoredToday: Int = 0
)

/** US3-1/2/5: reads StatsRepository + BehavioralBaseline and derives the numbers each screen
 * shows, instead of the screens holding hardcoded demo data. */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatsRepository.from(StatsDatabase.getInstance(application))

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        combine(
            repository.observeRecentHours(24 * 14),
            repository.observeBaseline()
        ) { recentHours, baseline -> recentHours to baseline }
            .onEach { (recentHours, baseline) -> update(recentHours, baseline) }
            .launchIn(viewModelScope)
    }

    private fun update(recentHours: List<HourlyStat>, baseline: BehavioralBaseline?) {
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

        var paceChangePercent = 0
        var weekBars = listOf<Bar>()
        var lateNightChangePercent = 0
        var showSuggestion = false

        if (baseline != null) {
            val recentBackspace = recentHours.filter { it.totalKeyPresses > 0 }
                .map { it.backspaceRate() }
                .average()
                .let { if (it.isNaN()) 0.0 else it }
            paceChangePercent = (((recentBackspace - baseline.avgBackspaceRate) / baseline.avgBackspaceRate.coerceAtLeast(0.01f)) * 100)
                .toInt()
                .coerceIn(-90, 90)

            val lastSevenDays = byDay.entries.sortedByDescending { it.key }.take(7).map { it.value }
            // Compare each day's total against the busiest day in the window, not a single
            // hour's max — otherwise every daily sum dwarfs the hourly max and every bar
            // clamps to 1.0 (flat), except a partial "today" which looks artificially low.
            val maxDailyKeyPresses = lastSevenDays.maxOfOrNull { day -> day.sumOf { it.totalKeyPresses } }
                ?.coerceAtLeast(1) ?: 1
            weekBars = lastSevenDays.reversed().mapIndexed { index, day ->
                val busy = day.sumOf { it.totalKeyPresses }.coerceAtLeast(1)
                val fraction = (busy.toFloat() / maxDailyKeyPresses).coerceIn(0.1f, 1f)
                val isRecent = index >= lastSevenDays.size - 3
                Bar(fraction, if (isRecent) MelookColors.Amber else MelookColors.Accent)
            }

            val lateNightHours = recentHours.filter { (it.hourBucket % 24) in 22..23 || (it.hourBucket % 24) in 0..4 }
            lateNightChangePercent = if (baseline.avgScreenTimeMillisPerDay > 0) {
                ((lateNightHours.sumOf { it.screenTimeMillis }.toFloat() / baseline.avgScreenTimeMillisPerDay.coerceAtLeast(1)) * 100)
                    .toInt()
                    .coerceIn(0, 200)
            } else 0

            showSuggestion = kotlin.math.abs(paceChangePercent) >= 15 || lateNightChangePercent >= 20
        }

        val longestInactiveStretchHours = recentHours.sortedBy { it.hourBucket }
            .fold(0 to 0) { (current, longest), stat ->
                if (stat.isInactive()) (current + 1) to maxOf(longest, current + 1) else 0 to longest
            }.second

        // Phone on/off schedule: average screen time per hour-of-day (0-23), normalized to
        // the busiest hour so the shape of the day is visible regardless of absolute usage.
        val byHourOfDay = recentHours.groupBy { (it.hourBucket % 24).toInt() }
        val maxAvgScreenTime = (0..23).maxOfOrNull { hour ->
            byHourOfDay[hour]?.map { it.screenTimeMillis }?.average() ?: 0.0
        }?.coerceAtLeast(1.0) ?: 1.0
        val hourlyActivityPattern = (0..23).map { hour ->
            val avg = byHourOfDay[hour]?.map { it.screenTimeMillis }?.average() ?: 0.0
            (avg / maxAvgScreenTime).toFloat().coerceIn(0f, 1f)
        }

        val lastSevenDaysAsc = byDay.entries.sortedByDescending { it.key }.take(7).map { it.value }.reversed()

        val maxBackspaceRate = lastSevenDaysAsc.maxOfOrNull { day ->
            day.filter { it.totalKeyPresses > 0 }.map { it.backspaceRate() }.average().let { if (it.isNaN()) 0.0 else it }
        }?.coerceAtLeast(0.01) ?: 0.01
        val backspaceRateBars = lastSevenDaysAsc.map { day ->
            val rate = day.filter { it.totalKeyPresses > 0 }.map { it.backspaceRate() }.average()
                .let { if (it.isNaN()) 0.0 else it }
            Bar((rate / maxBackspaceRate).toFloat().coerceIn(0.05f, 1f), MelookColors.Accent)
        }

        val sentimentTrendRecent = lastSevenDaysAsc.map { day ->
            day.mapNotNull { it.averageSentiment() }.average()
                .let { if (it.isNaN()) 0.5 else it }.toFloat().coerceIn(0f, 1f)
        }

        val maxAppSwitches = lastSevenDaysAsc.maxOfOrNull { day -> day.sumOf { it.appSwitchCount } }?.coerceAtLeast(1) ?: 1
        val appSwitchBars = lastSevenDaysAsc.map { day ->
            val switches = day.sumOf { it.appSwitchCount }.coerceAtLeast(0)
            Bar((switches.toFloat() / maxAppSwitches).coerceIn(0.05f, 1f), MelookColors.Amber)
        }

        val maxAppVariety = lastSevenDaysAsc.maxOfOrNull { day -> day.sumOf { it.distinctAppCount } }?.coerceAtLeast(1) ?: 1
        val appVarietyBars = lastSevenDaysAsc.map { day ->
            val variety = day.sumOf { it.distinctAppCount }.coerceAtLeast(0)
            Bar((variety.toFloat() / maxAppVariety).coerceIn(0.05f, 1f), MelookColors.Green)
        }

        _state.value = DashboardUiState(
            daysOfDataCollected = byDay.size,
            collectedToday = collectedToday,
            hasBaseline = baseline != null,
            paceChangePercent = paceChangePercent,
            weekBars = weekBars,
            lateNightChangePercent = lateNightChangePercent,
            appVarietyLower = true,
            showSuggestion = showSuggestion,
            hasEnoughWeeksForTrend = byDay.size >= 14,
            trendPoints = if (byDay.size >= 14) {
                byDay.entries.sortedBy { it.key }.map { (_, day) ->
                    day.mapNotNull { it.averageSentiment() }.average()
                        .let { if (it.isNaN()) 0.5f else it.toFloat() }
                        .coerceIn(0f, 1f)
                }
            } else emptyList(),
            quietStretchHours = longestInactiveStretchHours.toFloat(),
            quietStretchIncreased = baseline != null && longestInactiveStretchHours > baseline.avgLongestInactiveStretchHours,
            hourlyActivityPattern = hourlyActivityPattern,
            backspaceRateBars = backspaceRateBars,
            sentimentTrendRecent = sentimentTrendRecent,
            appSwitchBars = appSwitchBars,
            appVarietyBars = appVarietyBars,
            totalKeyPressesToday = today.sumOf { it.totalKeyPresses },
            totalBackspacesToday = today.sumOf { it.backspacePresses },
            totalWordsScoredToday = today.sumOf { it.wordsScored }
        )
    }
}

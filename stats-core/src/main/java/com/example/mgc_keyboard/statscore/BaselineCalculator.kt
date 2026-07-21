package com.example.mgc_keyboard.statscore

/** Pure functions over hourly history — no I/O, easy to unit test. */
object BaselineCalculator {

    /** Returns null if fewer than [MIN_BASELINE_DAYS] distinct days of data exist. */
    fun compute(history: List<HourlyStat>, nowMillis: Long = System.currentTimeMillis()): BehavioralBaseline? {
        val byDay = history.groupBy { it.dayBucket() }
        if (byDay.size < MIN_BASELINE_DAYS) return null

        val withKeys = history.filter { it.totalKeyPresses > 0 }
        val avgBackspaceRate = withKeys.map { it.backspaceRate() }.average().toFloat().orZero()

        val sentiments = history.mapNotNull { it.averageSentiment() }
        val avgSentiment = sentiments.average().toFloat().orZero()

        val avgAppSwitchesPerHour = history.map { it.appSwitchCount }.average().toFloat().orZero()

        val avgDistinctAppsPerDay = byDay.values
            .map { day -> day.sumOf { it.distinctAppCount } }
            .average().toFloat().orZero()

        val avgScreenTimeMillisPerDay = byDay.values
            .map { day -> day.sumOf { it.screenTimeMillis } }
            .average().orZero()

        val avgLongestInactiveStretchHours = byDay.values
            .map { day -> longestInactiveRun(day.sortedBy { it.hourBucket }) }
            .average().toFloat().orZero()

        return BehavioralBaseline(
            computedAt = nowMillis,
            daysOfDataUsed = byDay.size,
            avgBackspaceRate = avgBackspaceRate,
            avgSentiment = avgSentiment,
            avgAppSwitchesPerHour = avgAppSwitchesPerHour,
            avgDistinctAppsPerDay = avgDistinctAppsPerDay,
            avgScreenTimeMillisPerDay = avgScreenTimeMillisPerDay.toLong(),
            avgLongestInactiveStretchHours = avgLongestInactiveStretchHours
        )
    }

    /** US2-4: flags a withdrawal signal when today's app-diversity is well below baseline
     * or today's longest inactive stretch is well above baseline. */
    fun detectWithdrawal(
        today: List<HourlyStat>,
        baseline: BehavioralBaseline
    ): WithdrawalSignal? {
        val todaysDistinctApps = today.sumOf { it.distinctAppCount }
        val diversityDropPct = if (baseline.avgDistinctAppsPerDay > 0) {
            (1f - todaysDistinctApps / baseline.avgDistinctAppsPerDay) * 100f
        } else 0f

        val todaysInactiveRun = longestInactiveRun(today.sortedBy { it.hourBucket })
        val inactivityIncreasePct = if (baseline.avgLongestInactiveStretchHours > 0) {
            (todaysInactiveRun / baseline.avgLongestInactiveStretchHours - 1f) * 100f
        } else 0f

        return when {
            diversityDropPct >= DIVERSITY_DROP_THRESHOLD_PCT -> WithdrawalSignal(
                timestamp = System.currentTimeMillis(),
                reason = "app_diversity_drop",
                magnitudePercent = diversityDropPct
            )
            inactivityIncreasePct >= INACTIVITY_INCREASE_THRESHOLD_PCT -> WithdrawalSignal(
                timestamp = System.currentTimeMillis(),
                reason = "inactivity_increase",
                magnitudePercent = inactivityIncreasePct
            )
            else -> null
        }
    }

    private fun longestInactiveRun(dayHours: List<HourlyStat>): Float {
        var longest = 0
        var current = 0
        for (hour in dayHours) {
            if (hour.isInactive()) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 0
            }
        }
        return longest.toFloat()
    }

    private fun Float.orZero(): Float = if (isNaN()) 0f else this
    private fun Double.orZero(): Double = if (isNaN()) 0.0 else this

    private const val DIVERSITY_DROP_THRESHOLD_PCT = 30f
    private const val INACTIVITY_INCREASE_THRESHOLD_PCT = 50f
}

package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.alerts.AlertThresholdsStore
import com.example.mgc_keyboard.alerts.ThresholdMonitor
import com.example.mgc_keyboard.statscore.StatsAggregator
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.flow.first

/** Thin adapter: drains in-memory counters, forwards the delta to stats-core, then
 * checks the freshly-updated row against the user's alert thresholds (README §4.5). */
class KeyboardStatsSink(
    private val aggregator: StatsAggregator,
    private val repository: StatsRepository,
    private val thresholdMonitor: ThresholdMonitor,
    private val thresholdsStore: AlertThresholdsStore
) {
    suspend fun flush() {
        val latest = repository.mergeIntoCurrentHour(aggregator.drainAndReset())
        thresholdMonitor.checkAfterFlush(latest, thresholdsStore.thresholds.first())
    }
}

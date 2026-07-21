package com.example.mgc_keyboard.alerts

import com.example.mgc_keyboard.statscore.HourlyStat
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.flow.first

class ThresholdMonitor(
    private val repository: StatsRepository,
    private val notifier: AlertNotifier
) {
    /** Called synchronously right after each flush's mergeIntoCurrentHour() completes. */
    suspend fun checkAfterFlush(latest: HourlyStat, thresholds: AlertThresholds) {
        if (!thresholds.alertsEnabled) return

        val backspaceRate = latest.backspacePresses / latest.totalKeyPresses.coerceAtLeast(1).toFloat()
        if (backspaceRate >= thresholds.backspaceRateThreshold) {
            notifier.notifyBackspaceSpike(backspaceRate)
        }

        val recentHours = repository.observeRecentHours(thresholds.sentimentWindowHours).first()
        val allBelowFloor = recentHours.all {
            it.wordsScored > 0 && (it.sentimentSum / it.wordsScored) < thresholds.sentimentFloor
        }
        if (allBelowFloor && recentHours.size >= thresholds.sentimentWindowHours) {
            notifier.notifySentimentDip(thresholds.sentimentWindowHours)
        }
    }
}

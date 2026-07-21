package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.alerts.AlertThresholdsStore
import com.example.mgc_keyboard.alerts.ThresholdMonitor
import com.example.mgc_keyboard.statscore.DiagLog
import com.example.mgc_keyboard.statscore.StatsAggregator
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Thin adapter: drains in-memory counters, forwards the delta to stats-core, then
 * checks the freshly-updated row against the user's alert thresholds (README §4.5).
 *
 * flush() is called from three independent triggers (the periodic timer, onFinishInputView,
 * onDestroy) that can overlap in time. A Mutex serializes them — concurrent suspend-fun
 * calls into the same SQLCipher-backed Room connection previously caused native crashes
 * (SIGSEGV in SQLCipher's disk-IO threads) when two flushes landed back to back, e.g. during
 * the rapid IME create/destroy cycling that happens while unlocking the device. */
class KeyboardStatsSink(
    private val aggregator: StatsAggregator,
    private val repository: StatsRepository,
    private val thresholdMonitor: ThresholdMonitor,
    private val thresholdsStore: AlertThresholdsStore
) {
    private val mutex = Mutex()

    suspend fun flush() = mutex.withLock {
        DiagLog.d(TAG, "flush start")
        try {
            val latest = repository.mergeIntoCurrentHour(aggregator.drainAndReset())
            thresholdMonitor.checkAfterFlush(latest, thresholdsStore.thresholds.first())
            DiagLog.d(TAG, "flush ok bucket=${latest.hourBucket}")
        } catch (t: Throwable) {
            DiagLog.e(TAG, "flush failed", t)
            throw t
        }
    }

    private companion object {
        const val TAG = "KeyboardStatsSink"
    }
}

package com.example.mgc_keyboard.usagemonitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mgc_keyboard.alerts.AlertNotifier
import com.example.mgc_keyboard.alerts.AlertThresholdsStore
import com.example.mgc_keyboard.alerts.ThresholdMonitor
import com.example.mgc_keyboard.statscore.BaselineCalculator
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsDelta
import com.example.mgc_keyboard.statscore.StatsRepository
import com.example.mgc_keyboard.statscore.currentHourBucket
import com.example.mgc_keyboard.statscore.dayBucket
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Hourly WorkManager job. Zero dependency on ime/ — can be developed, tested,
 * and disabled independently. US2-1/US2-2: app-switch count, screen-time, app-diversity.
 */
class UsageMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val watcher = ForegroundSwitchWatcher(applicationContext)
        val repository = StatsRepository.from(StatsDatabase.getInstance(applicationContext))
        val thresholdMonitor = ThresholdMonitor(repository, AlertNotifier(applicationContext))
        val thresholdsStore = AlertThresholdsStore(applicationContext)

        if (!watcher.hasUsageAccess()) {
            AlertNotifier(applicationContext).notifyUsageAccessRevoked()
            return Result.success()
        }

        val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        val window = watcher.windowSince(since)

        repository.mergeIntoCurrentHour(StatsDelta(switches = window.switches))
        val latest = repository.recordUsage(currentHourBucket(), window.screenTimeMillis, window.distinctAppCount)
        thresholdMonitor.checkAfterFlush(latest, thresholdsStore.thresholds.first())
        return Result.success()
    }
}

/** US2-4/US3-1: once a day, (re)compute the personal baseline and check for a withdrawal signal. */
class BaselineWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = StatsRepository.from(StatsDatabase.getInstance(applicationContext))
        val history = repository.allHistory()
        if (history.isEmpty()) return Result.success()

        val baseline = BaselineCalculator.compute(history) ?: return Result.success()
        repository.saveBaseline(baseline)

        val today = history.filter { it.dayBucket() == history.last().dayBucket() }
        val signal = BaselineCalculator.detectWithdrawal(today, baseline)
        if (signal != null) {
            repository.recordWithdrawalSignal(signal)
            // README §4.5: alerts are off by default, explicit opt-in — this signal
            // must respect the same toggle as every other threshold-based notification.
            val alertsEnabled = AlertThresholdsStore(applicationContext).thresholds.first().alertsEnabled
            if (alertsEnabled) {
                AlertNotifier(applicationContext).notifyWithdrawalSignal(signal.reason)
            }
        }
        return Result.success()
    }
}

object UsageMonitorService {
    private const val PERIODIC_WORK_NAME = "usage-monitor-periodic"
    private const val BASELINE_WORK_NAME = "usage-monitor-baseline-daily"
    private const val ONE_OFF_WORK_NAME = "usage-monitor-one-off"

    fun schedule(context: Context) {
        // WorkManager's own minimum periodic interval; still only a backstop —
        // runOnce() below is what makes fresh data show up promptly in the UI.
        val periodic = PeriodicWorkRequestBuilder<UsageMonitorWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )

        val daily = PeriodicWorkRequestBuilder<BaselineWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BASELINE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            daily
        )

        runOnce(context)
    }

    /** Fires an immediate one-shot pass instead of waiting for the periodic tick — call this
     * on app start and right after usage-access permission is granted, so screen-time/app-switch
     * data shows up within seconds rather than being invisible for up to 15 minutes. */
    fun runOnce(context: Context) {
        val request = OneTimeWorkRequestBuilder<UsageMonitorWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_OFF_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

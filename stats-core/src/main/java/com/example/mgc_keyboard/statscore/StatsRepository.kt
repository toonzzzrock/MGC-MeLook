package com.example.mgc_keyboard.statscore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** The ONLY public entry point ime/, usage-monitor/ and dashboard/ are allowed to call. */
class StatsRepository(
    private val db: StatsDatabase,
    private val dao: StatsDao,
    private val baselineDao: BaselineDao,
    private val auditLogDao: AuditLogDao
) {

    companion object {
        fun from(db: StatsDatabase): StatsRepository =
            StatsRepository(db, db.statsDao(), db.baselineDao(), db.auditLogDao())
    }

    /** Wipes every locally-stored stat/baseline/signal/audit row. Irreversible — testing/reset use only. */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }

    suspend fun mergeIntoCurrentHour(delta: StatsDelta): HourlyStat {
        val bucket = currentHourBucket()
        dao.insertIfAbsent(HourlyStat(hourBucket = bucket))
        dao.mergeDelta(
            bucket,
            delta.keys,
            delta.backspace,
            delta.sentimentSum,
            delta.words,
            delta.switches
        )
        return dao.getByBucket(bucket) ?: HourlyStat(hourBucket = bucket)
    }

    suspend fun recordUsage(bucket: Long, screenTimeMillis: Long, distinctAppCount: Int): HourlyStat {
        dao.insertIfAbsent(HourlyStat(hourBucket = bucket))
        dao.setUsage(bucket, screenTimeMillis, distinctAppCount)
        return dao.getByBucket(bucket) ?: HourlyStat(hourBucket = bucket)
    }

    fun observeTrends(range: ClosedRange<Long>): Flow<List<HourlyStat>> =
        dao.observeRange(range.start, range.endInclusive)

    fun observeRecentHours(limit: Int): Flow<List<HourlyStat>> =
        dao.observeRecent(limit)

    suspend fun allHistory(): List<HourlyStat> = dao.getAll()

    fun observeBaseline(): Flow<BehavioralBaseline?> = baselineDao.observeLatest()

    suspend fun saveBaseline(baseline: BehavioralBaseline) = baselineDao.upsert(baseline)

    suspend fun recordWithdrawalSignal(signal: WithdrawalSignal) =
        baselineDao.insertWithdrawalSignal(signal)

    fun observeRecentWithdrawalSignals(limit: Int = 10): Flow<List<WithdrawalSignal>> =
        baselineDao.observeRecentWithdrawalSignals(limit)

    suspend fun recordAudit(eventType: String, detail: String = "") =
        auditLogDao.insert(AuditLogEntry(timestamp = System.currentTimeMillis(), eventType = eventType, detail = detail))

    fun observeAuditLog(): Flow<List<AuditLogEntry>> = auditLogDao.observeAll()

    /** Testing/demo only: backfills 16 days of synthetic hourly stats + a baseline so the
     * full dashboard UI (charts, trends) can be previewed before real data accumulates. */
    suspend fun seedDemoData() {
        val now = currentHourBucket()
        val random = kotlin.random.Random(42)
        val stats = (0 until 16 * 24).map { hoursAgo ->
            val bucket = now - hoursAgo
            val hourOfDay = (bucket % 24).toInt()
            val active = hourOfDay in 7..23
            val wordsScored = if (active) random.nextInt(5, 40) else 0
            HourlyStat(
                hourBucket = bucket,
                totalKeyPresses = if (active) random.nextInt(20, 200) else 0,
                backspacePresses = if (active) random.nextInt(2, 30) else 0,
                wordsScored = wordsScored,
                // sentimentSum must stay <= wordsScored so averageSentiment() = sum/scored is in [0,1],
                // matching the range every chart (LineChart Y-axis) assumes.
                sentimentSum = if (active) random.nextFloat() * wordsScored else 0f,
                appSwitchCount = if (active) random.nextInt(0, 10) else 0,
                screenTimeMillis = if (active) random.nextLong(60_000L, 1_800_000L) else 0L,
                distinctAppCount = if (active) random.nextInt(1, 6) else 0
            )
        }
        dao.insertAll(stats)
        baselineDao.upsert(
            BehavioralBaseline(
                computedAt = System.currentTimeMillis(),
                daysOfDataUsed = 14,
                avgBackspaceRate = 0.15f,
                avgSentiment = 0.55f,
                avgAppSwitchesPerHour = 3f,
                avgDistinctAppsPerDay = 8f,
                avgScreenTimeMillisPerDay = 4 * 3_600_000L,
                avgLongestInactiveStretchHours = 6f
            )
        )
    }
}

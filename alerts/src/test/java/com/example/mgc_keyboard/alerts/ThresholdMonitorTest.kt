package com.example.mgc_keyboard.alerts

import android.app.NotificationManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mgc_keyboard.statscore.HourlyStat
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThresholdMonitorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private lateinit var db: StatsDatabase
    private lateinit var repository: StatsRepository
    private lateinit var monitor: ThresholdMonitor

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, StatsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StatsRepository.from(db)
        monitor = ThresholdMonitor(repository, AlertNotifier(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `does nothing when alerts are disabled`() = runTest {
        val latest = HourlyStat(hourBucket = 1L, totalKeyPresses = 10, backspacePresses = 9)

        monitor.checkAfterFlush(latest, AlertThresholds(alertsEnabled = false))

        assertNull(shadowOf(manager).getNotification(1))
        assertNull(shadowOf(manager).getNotification(2))
    }

    @Test
    fun `posts a backspace spike notification when the rate is at or above threshold`() = runTest {
        val latest = HourlyStat(hourBucket = 1L, totalKeyPresses = 10, backspacePresses = 5)
        val thresholds = AlertThresholds(backspaceRateThreshold = 0.40f, alertsEnabled = true, sentimentWindowHours = 3)

        monitor.checkAfterFlush(latest, thresholds)

        assertNotNull(shadowOf(manager).getNotification(1))
    }

    @Test
    fun `does not post a backspace spike notification when the rate is below threshold`() = runTest {
        val latest = HourlyStat(hourBucket = 1L, totalKeyPresses = 100, backspacePresses = 5)
        val thresholds = AlertThresholds(backspaceRateThreshold = 0.40f, alertsEnabled = true, sentimentWindowHours = 3)

        monitor.checkAfterFlush(latest, thresholds)

        assertNull(shadowOf(manager).getNotification(1))
    }

    @Test
    fun `posts a sentiment dip notification when every hour in the window is below the floor`() = runTest {
        seedHours(
            HourlyStat(hourBucket = 10L, wordsScored = 10, sentimentSum = 1f),
            HourlyStat(hourBucket = 9L, wordsScored = 10, sentimentSum = 1f),
            HourlyStat(hourBucket = 8L, wordsScored = 10, sentimentSum = 1f),
        )

        val latest = HourlyStat(hourBucket = 10L, totalKeyPresses = 100, backspacePresses = 5)
        val thresholds = AlertThresholds(backspaceRateThreshold = 0.90f, sentimentFloor = 0.30f, sentimentWindowHours = 3, alertsEnabled = true)

        monitor.checkAfterFlush(latest, thresholds)

        assertNotNull(shadowOf(manager).getNotification(2))
    }

    @Test
    fun `does not post a sentiment dip notification when fewer hours than the window exist`() = runTest {
        seedHours(HourlyStat(hourBucket = 10L, wordsScored = 10, sentimentSum = 1f))

        val latest = HourlyStat(hourBucket = 10L, totalKeyPresses = 100, backspacePresses = 5)
        val thresholds = AlertThresholds(backspaceRateThreshold = 0.90f, sentimentFloor = 0.30f, sentimentWindowHours = 3, alertsEnabled = true)

        monitor.checkAfterFlush(latest, thresholds)

        assertNull(shadowOf(manager).getNotification(2))
    }

    @Test
    fun `does not post a sentiment dip notification when sentiment is above the floor`() = runTest {
        seedHours(
            HourlyStat(hourBucket = 10L, wordsScored = 10, sentimentSum = 9f),
            HourlyStat(hourBucket = 9L, wordsScored = 10, sentimentSum = 9f),
            HourlyStat(hourBucket = 8L, wordsScored = 10, sentimentSum = 9f),
        )

        val latest = HourlyStat(hourBucket = 10L, totalKeyPresses = 100, backspacePresses = 5)
        val thresholds = AlertThresholds(backspaceRateThreshold = 0.90f, sentimentFloor = 0.30f, sentimentWindowHours = 3, alertsEnabled = true)

        monitor.checkAfterFlush(latest, thresholds)

        assertNull(shadowOf(manager).getNotification(2))
    }

    /** observeRecentHours() reads straight from the DAO, so seed rows via the DAO rather than
     * through mergeIntoCurrentHour(), which always writes to the real current-time hour bucket. */
    private suspend fun seedHours(vararg stats: HourlyStat) {
        db.statsDao().insertAll(stats.toList())
    }
}

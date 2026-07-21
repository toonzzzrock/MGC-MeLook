package com.example.mgc_keyboard.statscore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineCalculatorTest {

    private fun hour(hourBucket: Long, keys: Int = 10, backspace: Int = 1, words: Int = 5, sentiment: Float = 3f, apps: Int = 2, screenMs: Long = 60_000) =
        HourlyStat(
            hourBucket = hourBucket,
            totalKeyPresses = keys,
            backspacePresses = backspace,
            sentimentSum = sentiment,
            wordsScored = words,
            distinctAppCount = apps,
            screenTimeMillis = screenMs
        )

    @Test
    fun `compute returns null with fewer than MIN_BASELINE_DAYS distinct days`() {
        // 2 days of data, one hour each — below the 3-day minimum.
        val history = listOf(hour(0), hour(24))
        assertNull(BaselineCalculator.compute(history))
    }

    @Test
    fun `compute averages backspace rate and sentiment across history`() {
        // 3 distinct days (hourBucket 0, 24, 48), one hour each.
        val history = listOf(
            hour(0, keys = 100, backspace = 10, words = 2, sentiment = 1f),
            hour(24, keys = 100, backspace = 20, words = 2, sentiment = 1f),
            hour(48, keys = 100, backspace = 30, words = 2, sentiment = 1f)
        )

        val baseline = BaselineCalculator.compute(history)

        assertNotNull(baseline)
        assertEquals(3, baseline!!.daysOfDataUsed)
        assertEquals(0.2f, baseline.avgBackspaceRate, 0.0001f) // (0.1+0.2+0.3)/3
        assertEquals(0.5f, baseline.avgSentiment, 0.0001f) // sentimentSum=1, words=2 -> 0.5 per hour, same each hour
    }

    @Test
    fun `detectWithdrawal flags large drop in app diversity vs baseline`() {
        val baseline = BehavioralBaseline(
            computedAt = 0, daysOfDataUsed = 3,
            avgBackspaceRate = 0.1f, avgSentiment = 0.5f,
            avgAppSwitchesPerHour = 2f, avgDistinctAppsPerDay = 10f,
            avgScreenTimeMillisPerDay = 100_000, avgLongestInactiveStretchHours = 2f
        )
        // Today: only 2 distinct apps vs a baseline of 10 -> 80% drop, over the 30% threshold.
        val today = listOf(hour(100, apps = 2))

        val signal = BaselineCalculator.detectWithdrawal(today, baseline)

        assertNotNull(signal)
        assertEquals("app_diversity_drop", signal!!.reason)
        assertTrue(signal.magnitudePercent >= 30f)
    }

    @Test
    fun `detectWithdrawal returns null when today matches baseline`() {
        val baseline = BehavioralBaseline(
            computedAt = 0, daysOfDataUsed = 3,
            avgBackspaceRate = 0.1f, avgSentiment = 0.5f,
            avgAppSwitchesPerHour = 2f, avgDistinctAppsPerDay = 5f,
            avgScreenTimeMillisPerDay = 100_000, avgLongestInactiveStretchHours = 2f
        )
        val today = listOf(hour(100, apps = 5))

        assertNull(BaselineCalculator.detectWithdrawal(today, baseline))
    }
}

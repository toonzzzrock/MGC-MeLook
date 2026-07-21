package com.example.mgc_keyboard.statscore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyStatTest {

    @Test
    fun `backspaceRate is zero when no keys pressed`() {
        val stat = HourlyStat(hourBucket = 0, totalKeyPresses = 0, backspacePresses = 0)
        assertEquals(0f, stat.backspaceRate())
    }

    @Test
    fun `backspaceRate divides backspaces by total keys`() {
        val stat = HourlyStat(hourBucket = 0, totalKeyPresses = 100, backspacePresses = 25)
        assertEquals(0.25f, stat.backspaceRate())
    }

    @Test
    fun `averageSentiment is null when no words scored`() {
        val stat = HourlyStat(hourBucket = 0, wordsScored = 0, sentimentSum = 0f)
        assertNull(stat.averageSentiment())
    }

    @Test
    fun `averageSentiment divides sum by word count`() {
        val stat = HourlyStat(hourBucket = 0, wordsScored = 4, sentimentSum = 2f)
        assertEquals(0.5f, stat.averageSentiment())
    }

    @Test
    fun `isInactive true only when no keys and no screen time`() {
        assertTrue(HourlyStat(hourBucket = 0, totalKeyPresses = 0, screenTimeMillis = 0).isInactive())
        assertTrue(!HourlyStat(hourBucket = 0, totalKeyPresses = 1, screenTimeMillis = 0).isInactive())
        assertTrue(!HourlyStat(hourBucket = 0, totalKeyPresses = 0, screenTimeMillis = 1000).isInactive())
    }

    @Test
    fun `dayBucket groups 24 consecutive hourBuckets together`() {
        val hours = (0 until 24).map { HourlyStat(hourBucket = it.toLong()) }
        assertTrue(hours.all { it.dayBucket() == 0L })
        assertEquals(1L, HourlyStat(hourBucket = 24).dayBucket())
    }

    @Test
    fun `currentHourBucket truncates millis to the hour`() {
        val oneHourMillis = 3_600_000L
        assertEquals(5L, currentHourBucket(nowMillis = 5 * oneHourMillis + 1234))
    }
}

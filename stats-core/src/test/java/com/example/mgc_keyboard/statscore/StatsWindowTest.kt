package com.example.mgc_keyboard.statscore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A day of stale history must not reach into today's comparison. Recording has gaps, so the
 * window has to be measured in hours of the calendar rather than in rows that happen to exist.
 */
class StatsWindowTest {

    private class FakeDao(private val rows: List<HourlyStat>) : StatsDao {
        var rangeFrom: Long = -1
        var rangeTo: Long = -1

        override fun observeRange(from: Long, to: Long): Flow<List<HourlyStat>> {
            rangeFrom = from
            rangeTo = to
            return flowOf(rows.filter { it.hourBucket in from..to }.sortedBy { it.hourBucket })
        }

        override fun observeRecent(limit: Int): Flow<List<HourlyStat>> =
            flowOf(rows.sortedByDescending { it.hourBucket }.take(limit))

        override suspend fun getAll(): List<HourlyStat> = rows
        override suspend fun getByBucket(bucket: Long): HourlyStat? = rows.find { it.hourBucket == bucket }
        override suspend fun insertIfAbsent(stat: HourlyStat) = Unit
        override suspend fun insertAll(stats: List<HourlyStat>) = Unit
        override suspend fun setUsage(bucket: Long, screenTimeMillis: Long, distinctAppCount: Int) = Unit
        override suspend fun mergeDelta(
            bucket: Long,
            dKeys: Int,
            dBackspace: Int,
            dSentimentSum: Float,
            dWords: Int,
            dSwitches: Int
        ) = Unit
    }

    /** The shape that broke on a real device: a dense block of old rows, a gap, then a few
     * recent ones. Asking for 336 rows hands back the old block; asking for 336 hours does not. */
    private fun historyWithAnOldBlock(): List<HourlyStat> {
        val now = currentHourBucket()
        val old = (0 until 384).map { HourlyStat(hourBucket = now - 24 * 40 + it, totalKeyPresses = 100) }
        val recent = (0 until 6).map { HourlyStat(hourBucket = now - it * 24, totalKeyPresses = 5) }
        return old + recent
    }

    @Test
    fun `hours window excludes history older than the window`() = runBlocking {
        val rows = historyWithAnOldBlock()
        val dao = FakeDao(rows)
        val window = 24 * 14

        val kept = dao.observeRange(windowStart(window), Long.MAX_VALUE).first()

        assertEquals("only the recent days survive a 14-day window", 6, kept.size)
        assertTrue("nothing older than the window", kept.all { it.hourBucket > currentHourBucket() - window })
    }

    @Test
    fun `row limit reaches back past the window`() = runBlocking {
        val dao = FakeDao(historyWithAnOldBlock())

        val byRows = dao.observeRecent(24 * 14).first()

        assertTrue(
            "a row limit pulls in 40-day-old history, which is the bug the hours window fixes",
            byRows.any { it.hourBucket < currentHourBucket() - 24 * 14 }
        )
    }

    @Test
    fun `window stays open at the top so hours recorded later still arrive`() = runBlocking {
        val dao = FakeDao(emptyList())

        dao.observeRange(windowStart(24), Long.MAX_VALUE).first()

        assertEquals(Long.MAX_VALUE, dao.rangeTo)
    }
}

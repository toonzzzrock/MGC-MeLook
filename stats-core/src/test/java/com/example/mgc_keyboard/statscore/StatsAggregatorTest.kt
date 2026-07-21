package com.example.mgc_keyboard.statscore

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StatsAggregatorTest {

    @Test
    fun `drainAndReset reflects key presses and backspaces`() {
        val aggregator = StatsAggregator()
        aggregator.onKeyPress(isBackspace = false)
        aggregator.onKeyPress(isBackspace = false)
        aggregator.onKeyPress(isBackspace = true)

        val delta = aggregator.drainAndReset()

        assertEquals(3, delta.keys)
        assertEquals(1, delta.backspace)
    }

    @Test
    fun `drainAndReset sums word sentiment scores`() {
        val aggregator = StatsAggregator()
        aggregator.onWordScored(0.8f)
        aggregator.onWordScored(0.2f)

        val delta = aggregator.drainAndReset()

        assertEquals(2, delta.words)
        assertEquals(1.0f, delta.sentimentSum, 0.0001f)
    }

    @Test
    fun `drainAndReset counts app switches`() {
        val aggregator = StatsAggregator()
        repeat(3) { aggregator.onAppSwitch() }

        assertEquals(3, aggregator.drainAndReset().switches)
    }

    @Test
    fun `drainAndReset zeroes counters so a second drain is empty`() {
        val aggregator = StatsAggregator()
        aggregator.onKeyPress(isBackspace = true)
        aggregator.drainAndReset()

        val second = aggregator.drainAndReset()

        assertEquals(0, second.keys)
        assertEquals(0, second.backspace)
        assertEquals(0f, second.sentimentSum)
    }

    @Test
    fun `concurrent onWordScored calls do not lose updates`() {
        val aggregator = StatsAggregator()
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(800)

        repeat(800) {
            pool.submit {
                aggregator.onWordScored(1f)
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        pool.shutdown()

        val delta = aggregator.drainAndReset()
        assertEquals(800, delta.words)
        assertEquals(800f, delta.sentimentSum, 0.01f)
    }
}

package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.statscore.StatsAggregator
import org.junit.Assert.assertEquals
import org.junit.Test

class BackspaceTrackerTest {

    @Test
    fun `backspace key code increments backspace count`() {
        val aggregator = StatsAggregator()
        val tracker = BackspaceTracker(aggregator)

        tracker.onKeyProcessed(BackspaceTracker.BACKSPACE_CODE)

        val delta = aggregator.drainAndReset()
        assertEquals(1, delta.keys)
        assertEquals(1, delta.backspace)
    }

    @Test
    fun `non-backspace key code counts as a key press only`() {
        val aggregator = StatsAggregator()
        val tracker = BackspaceTracker(aggregator)

        tracker.onKeyProcessed('a'.code)

        val delta = aggregator.drainAndReset()
        assertEquals(1, delta.keys)
        assertEquals(0, delta.backspace)
    }
}

package com.example.mgc_keyboard.dashboard

import com.example.mgc_keyboard.dashboard.charts.ChartPoint
import com.example.mgc_keyboard.dashboard.charts.xFractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The trend chart's claim is that its shape is the shape of time, and that the sentence under it
 * is not reading noise as a move. Both get a test. */
class TrendSeriesTest {

    @Test
    fun `a gap in recording takes its own width on the axis`() {
        // Three days, then nothing for a week, then two more.
        val points = listOf(0, 1, 2, 10, 11).map { ChartPoint(value = 0.5f, dayOffset = it) }
        val fractions = xFractions(points)
        assertEquals(0f, fractions.first(), 0.001f)
        assertEquals(1f, fractions.last(), 0.001f)
        // Day 2 to day 10 is eight of the eleven days charted, so it must take most of the width.
        assertTrue("the empty week is compressed away", fractions[3] - fractions[2] > 0.6f)
    }

    @Test
    fun `without day numbers points stay evenly spaced`() {
        val fractions = xFractions(List(5) { ChartPoint(value = 0.5f) })
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), fractions)
    }

    @Test
    fun `a move inside the earlier spread reads as no move`() {
        val earlier = listOf(4f, 6f, 5f, 7f, 3f, 6f, 5f)
        val recent = listOf(5f, 6f, 4f, 6f, 5f, 5f, 6f)
        assertTrue(summarise(earlier + recent) { "${it.toInt()}" }.contains("about the same as"))
    }

    @Test
    fun `a move past half the earlier spread is named`() {
        val earlier = listOf(4f, 6f, 5f, 7f, 3f, 6f, 5f)
        val recent = List(7) { 12f }
        assertTrue(summarise(earlier + recent) { "${it.toInt()}" }.contains("higher than"))
        assertTrue(summarise(earlier + List(7) { 0f }) { "${it.toInt()}" }.contains("lower than"))
    }

    @Test
    fun `too little history says so rather than comparing`() {
        val summary = summarise(List(8) { 5f }) { "${it.toInt()}" }
        assertTrue(summary.contains("Not enough earlier days"))
    }
}

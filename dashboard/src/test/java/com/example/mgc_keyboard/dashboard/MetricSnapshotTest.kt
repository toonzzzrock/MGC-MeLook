package com.example.mgc_keyboard.dashboard

import com.example.mgc_keyboard.dashboard.charts.ChartCitations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The whole redesign rests on "outside your own usual range" meaning something, so the
 * deviation rule gets a test rather than a screenshot. */
class MetricSnapshotTest {

    private fun snapshot(values: List<Float>, higherIsConcerning: Boolean = true) = metricFrom(
        key = "k",
        name = "Backspace rate",
        series = values.map { "Mon" to it },
        format = { String.format("%.1f", it) },
        deltaFormat = { String.format("%.1f", it) },
        higherIsConcerning = higherIsConcerning,
        unitCaption = "",
        sourceLabel = "Liu et al., 2024",
        howMeasured = "",
        info = ChartCitations.BACKSPACE_RATE
    )

    @Test
    fun `too few days yields no snapshot`() {
        assertNull(snapshot(listOf(5f, 5f, 6f)))
    }

    @Test
    fun `today far above a steady history is outside the range and concerning`() {
        val metric = snapshot(listOf(5f, 5.2f, 4.8f, 5.1f, 5f, 4.9f, 9f))!!
        assertTrue(metric.outsideUsualRange)
        assertTrue(metric.concerning)
        assertTrue(metric.higher)
        assertEquals("+4.0", metric.deltaLabel)
        assertEquals(6, metric.daysCompared)
    }

    @Test
    fun `a move within the usual spread is not flagged`() {
        val metric = snapshot(listOf(2f, 8f, 3f, 7f, 4f, 6f, 6f))!!
        assertFalse(metric.outsideUsualRange)
        assertFalse(metric.concerning)
    }

    @Test
    fun `a perfectly flat history never flags`() {
        val metric = snapshot(listOf(5f, 5f, 5f, 5f, 5f, 5f, 7f))!!
        assertFalse(metric.outsideUsualRange)
    }

    @Test
    fun `direction decides concern - a rise is fine when falling is the worry`() {
        val metric = snapshot(listOf(5f, 5.2f, 4.8f, 5.1f, 5f, 4.9f, 9f), higherIsConcerning = false)!!
        assertTrue(metric.outsideUsualRange)
        assertFalse(metric.concerning)
    }

    @Test
    fun `a drop reads as a minus and stays out of the concern set when rising is the worry`() {
        val metric = snapshot(listOf(5f, 5.2f, 4.8f, 5.1f, 5f, 4.9f, 1f))!!
        assertFalse(metric.higher)
        assertTrue(metric.deltaLabel.startsWith("−"))
        assertTrue(metric.outsideUsualRange)
        assertFalse(metric.concerning)
    }

    @Test
    fun `minutes format switches to hours past sixty`() {
        assertEquals("45m", formatMinutes(45f))
        assertEquals("2h 48m", formatMinutes(168f))
    }
}

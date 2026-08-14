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
    fun `earlier days are cut to the hour today has reached`() {
        // Six full days of hours 0..23, today only up to hour 9.
        val days = List(6) { (0..23).toList() } + listOf((0..9).toList())
        val aligned = alignToPartialDay(days) { it }
        assertEquals(List(7) { 10 }, aligned.map { it.size })
    }

    @Test
    fun `a part-day total does not read as a drop once aligned`() {
        // Six full days: 3 or 4 apps by 9am, 7 more over the afternoon. Today is 9am, 3 apps —
        // an ordinary morning, but a collapse if measured against whole days.
        fun day(index: Int) = (0..23).map { hour ->
            hour to when {
                hour < 10 -> if (hour < 3 + index % 2) 1 else 0
                hour == 10 -> 7
                else -> 0
            }
        }
        val days = List(6) { day(it) } + listOf(day(0).take(10))
        fun totals(input: List<List<Pair<Int, Int>>>) =
            input.map { "Mon" to it.sumOf { (_, apps) -> apps }.toFloat() }

        val unaligned = metricFrom(
            key = "k", name = "App variety", series = totals(days),
            format = { "${it.toInt()}" }, deltaFormat = { "${it.toInt()}" },
            higherIsConcerning = false, unitCaption = "", sourceLabel = "",
            howMeasured = "", info = ChartCitations.APP_VARIETY
        )
        val aligned = metricFrom(
            key = "k", name = "App variety", series = totals(alignToPartialDay(days) { it.first }),
            format = { "${it.toInt()}" }, deltaFormat = { "${it.toInt()}" },
            higherIsConcerning = false, unitCaption = "", sourceLabel = "",
            howMeasured = "", info = ChartCitations.APP_VARIETY
        )
        assertTrue("unaligned morning should look like a collapse", unaligned!!.concerning)
        assertFalse("aligned morning is an ordinary morning", aligned!!.concerning)
        assertFalse(aligned.outsideUsualRange)
    }

    @Test
    fun `a thin early-hour window cannot claim a total is unusual`() {
        // Prior mornings by 6am: mostly nothing, occasionally a couple of switches. Today's 3 is
        // several deviations above that near-zero mean, which is the hour talking, not the user.
        val series = listOf(0f, 0f, 1f, 0f, 0f, 2f, 3f).map { "Mon" to it }
        fun snap(hours: Int?) = metricFrom(
            key = "k", name = "App switching", series = series,
            format = { "${it.toInt()}" }, deltaFormat = { "${it.toInt()}" },
            higherIsConcerning = true, unitCaption = "", sourceLabel = "",
            howMeasured = "", info = ChartCitations.APP_SWITCHING, partialDayHours = hours
        )!!
        assertTrue("without the hour guard the morning flags", snap(null).concerning)
        assertFalse(snap(2).outsideUsualRange)
        assertFalse(snap(2).concerning)
        // The number itself is unaffected; only the claim about it is held back.
        assertEquals("3", snap(2).todayLabel)
        // Past the threshold the rule applies again.
        assertTrue(snap(MIN_HOURS_TO_FLAG).concerning)
    }

    @Test
    fun `a running total says which window it covers`() {
        val series = List(7) { "Mon" to 5f }
        fun snap(hours: Int?) = metricFrom(
            key = "k", name = "Screen-on time", series = series,
            format = { "${it.toInt()}m" }, deltaFormat = { "${it.toInt()}m" },
            higherIsConcerning = true, unitCaption = "", sourceLabel = "",
            howMeasured = "", info = ChartCitations.PHONE_SCHEDULE, partialDayHours = hours
        )!!
        assertEquals("6-day average", snap(null).baselineCaption)
        assertEquals("6-day average, same hours", snap(9).baselineCaption)
        assertTrue(snap(9).plainReading.contains("so far today"))
        assertTrue(snap(9).plainReading.contains("by this time"))
    }

    @Test
    fun `minutes format switches to hours past sixty`() {
        assertEquals("45m", formatMinutes(45f))
        assertEquals("2h 48m", formatMinutes(168f))
    }
}

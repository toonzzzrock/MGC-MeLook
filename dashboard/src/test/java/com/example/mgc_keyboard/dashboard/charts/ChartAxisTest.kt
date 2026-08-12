package com.example.mgc_keyboard.dashboard.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartAxisTest {

    @Test
    fun `all labels kept when they fit`() {
        assertEquals((0..6).toSet(), visibleLabelIndices(count = 7, slotWidthPx = 40f, labelWidthPx = 20f))
    }

    @Test
    fun `crowded axis thins but keeps first and last`() {
        // 21 points, each slot narrower than the label: the smear seen on the trends chart.
        val kept = visibleLabelIndices(count = 21, slotWidthPx = 15f, labelWidthPx = 45f)
        assertTrue(0 in kept)
        assertTrue(20 in kept)
        assertTrue(kept.size <= 21 / 3 + 1)
        // No two kept labels closer than one label width.
        val sorted = kept.sorted()
        sorted.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= 3) }
    }

    @Test
    fun `degenerate counts do not crash`() {
        assertEquals(emptySet<Int>(), visibleLabelIndices(0, 10f, 10f))
        assertEquals(setOf(0), visibleLabelIndices(1, 10f, 10f))
    }
}

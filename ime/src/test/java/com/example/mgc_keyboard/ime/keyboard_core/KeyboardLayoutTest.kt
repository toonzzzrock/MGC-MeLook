package com.example.mgc_keyboard.ime.keyboard_core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Row widths are hand-tuned unit counts, so removing or resizing a key silently leaves
 * a gap or pushes the row off-screen. 1000px makes every half-unit land on a whole
 * pixel, so the sums below are exact.
 *
 * Not every row is meant to fill the view: the ASDFGHJKL row is inset half a unit on
 * each side and the emoji grid is 8 keys wide. Only the action rows (the ones carrying
 * the space bar) have to span it.
 */
class KeyboardLayoutTest {

    private val width = 1000
    private val height = 400

    private fun rows(keys: List<Key>): List<List<Key>> =
        keys.groupBy { it.y }.toSortedMap().values.map { row -> row.sortedBy { it.x } }

    private fun assertNoOverflowOrOverlap(keys: List<Key>, layout: String) {
        assertTrue("$layout produced no keys", keys.isNotEmpty())
        rows(keys).forEach { row ->
            val y = row.first().y
            assertTrue(
                "$layout row at y=$y runs past the view",
                row.last().let { it.x + it.width } <= width
            )
            row.zipWithNext { left, right ->
                assertEquals("$layout row at y=$y has a gap or overlap", left.x + left.width, right.x)
            }
        }
    }

    /** The row holding the space bar: its units have to add up to the full 10. */
    private fun assertActionRowSpansView(keys: List<Key>, layout: String) {
        val spaceRow = rows(keys).single { row -> row.any { it.code == KeyCodes.SPACE } }
        assertEquals(
            "$layout space-bar row does not span the view",
            width,
            spaceRow.last().let { it.x + it.width }
        )
    }

    @Test
    fun `qwerty rows fit the view`() {
        val keys = KeyboardLayout.buildQwerty(width, height)
        assertNoOverflowOrOverlap(keys, "qwerty")
        assertActionRowSpansView(keys, "qwerty")
    }

    @Test
    fun `symbols rows fit the view`() {
        val keys = KeyboardLayout.buildSymbols(width, height)
        assertNoOverflowOrOverlap(keys, "symbols")
        assertActionRowSpansView(keys, "symbols")
    }

    @Test
    fun `emoji rows fit the view`() {
        assertNoOverflowOrOverlap(KeyboardLayout.buildEmoji(width, height), "emoji")
    }
}

package com.example.mgc_keyboard.dashboard

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MelookColorsTest {

    @After
    fun reset() {
        MelookColors.dark = false
    }

    @Test
    fun `surface and text tokens flip with the theme`() {
        MelookColors.dark = false
        val lightSurface = MelookColors.Surface
        val lightText = MelookColors.TextDark

        MelookColors.dark = true
        assertNotEquals(lightSurface, MelookColors.Surface)
        assertNotEquals(lightText, MelookColors.TextDark)
    }

    /** DashboardViewModel bakes these into chart data outside composition — they must not theme. */
    @Test
    fun `brand colours are theme-independent`() {
        MelookColors.dark = false
        val accent = MelookColors.Accent
        val amber = MelookColors.Amber
        val green = MelookColors.Green

        MelookColors.dark = true
        assertEquals(accent, MelookColors.Accent)
        assertEquals(amber, MelookColors.Amber)
        assertEquals(green, MelookColors.Green)
    }
}

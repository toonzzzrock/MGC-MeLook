package com.example.mgc_keyboard.sentiment

import org.junit.Assert.assertEquals
import org.junit.Test

class SentimentModelTest {

    private val model = SentimentModel()

    @Test
    fun `known positive word scores above neutral`() {
        assertEquals(0.9f, model.score("great"), 0.0001f)
    }

    @Test
    fun `known negative word scores below neutral`() {
        assertEquals(0.15f, model.score("sad"), 0.0001f)
    }

    @Test
    fun `unknown word scores neutral`() {
        assertEquals(0.5f, model.score("banana"), 0.0001f)
    }

    @Test
    fun `empty or blank text scores neutral`() {
        assertEquals(0.5f, model.score(""), 0.0001f)
        assertEquals(0.5f, model.score("   "), 0.0001f)
    }

    @Test
    fun `scoring is case-insensitive and trims whitespace`() {
        assertEquals(0.9f, model.score("  GREAT  "), 0.0001f)
        assertEquals(0.15f, model.score("Sad"), 0.0001f)
    }
}

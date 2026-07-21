package com.example.mgc_keyboard.ime.tracking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SentimentScorerTest {

    @Test
    fun `score delegates to the underlying model off the calling thread`() = runTest {
        val scorer = SentimentScorer(dispatcher = Dispatchers.Unconfined)

        assertEquals(0.8f, scorer.score("good"), 0.0001f)
    }
}

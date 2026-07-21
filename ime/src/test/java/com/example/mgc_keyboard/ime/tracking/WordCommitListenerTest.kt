package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.statscore.StatsAggregator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WordCommitListenerTest {

    @Test
    fun `committing a word scores it and records it in the aggregator`() = runTest {
        val aggregator = StatsAggregator()
        val scorer = SentimentScorer(dispatcher = kotlinx.coroutines.Dispatchers.Unconfined)
        val listener = WordCommitListener(scorer, aggregator)

        listener.onWordCommitted("great")

        val delta = aggregator.drainAndReset()
        assertEquals(1, delta.words)
        assertEquals(0.9f, delta.sentimentSum, 0.0001f)
    }

    @Test
    fun `committing multiple words accumulates sentiment sum`() = runTest {
        val aggregator = StatsAggregator()
        val scorer = SentimentScorer(dispatcher = kotlinx.coroutines.Dispatchers.Unconfined)
        val listener = WordCommitListener(scorer, aggregator)

        listener.onWordCommitted("great")
        listener.onWordCommitted("sad")

        val delta = aggregator.drainAndReset()
        assertEquals(2, delta.words)
        assertEquals(1.05f, delta.sentimentSum, 0.0001f)
    }
}

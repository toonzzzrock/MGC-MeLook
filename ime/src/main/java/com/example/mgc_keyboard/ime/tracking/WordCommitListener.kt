package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.statscore.StatsAggregator

/**
 * Hooked into the word-commit signal (space / punctuation typed after a word).
 * `word` is a function-scoped local — never referenced again after scoring it,
 * so it falls out of scope and nothing writes it to disk or logs.
 */
class WordCommitListener(
    private val scorer: SentimentScorer,
    private val aggregator: StatsAggregator
) {
    suspend fun onWordCommitted(word: String) {
        val score = scorer.score(word)
        aggregator.onWordScored(score)
    }
}

package com.example.mgc_keyboard.sentiment

/**
 * Scores a single word/short phrase 0f (negative) .. 1f (positive).
 *
 * Backed by a small on-device lexicon rather than the TFLite embedding model
 * described in the architecture plan (training that requires a labeled dataset
 * and a training pipeline this session doesn't have). Same score(text) contract,
 * so a real Interpreter-backed implementation can drop in without touching callers.
 */
class SentimentModel {

    fun score(text: String): Float {
        val word = text.trim().lowercase()
        if (word.isEmpty()) return NEUTRAL

        POSITIVE_WORDS[word]?.let { return it }
        NEGATIVE_WORDS[word]?.let { return it }
        return NEUTRAL
    }

    private companion object {
        const val NEUTRAL = 0.5f

        val POSITIVE_WORDS = mapOf(
            "good" to 0.8f, "great" to 0.9f, "happy" to 0.9f, "love" to 0.9f,
            "excited" to 0.85f, "fun" to 0.8f, "nice" to 0.75f, "awesome" to 0.9f,
            "thanks" to 0.75f, "glad" to 0.8f, "calm" to 0.7f, "relaxed" to 0.75f
        )

        val NEGATIVE_WORDS = mapOf(
            "tired" to 0.3f, "sad" to 0.15f, "bad" to 0.2f, "hate" to 0.1f,
            "stressed" to 0.2f, "anxious" to 0.15f, "worried" to 0.25f,
            "lonely" to 0.15f, "exhausted" to 0.2f, "angry" to 0.2f, "hopeless" to 0.05f
        )
    }
}

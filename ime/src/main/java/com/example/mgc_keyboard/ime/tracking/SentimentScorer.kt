package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.sentiment.SentimentModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Runs sentiment inference off the IME's main thread (section 5: a slow device
 * must never introduce input lag on the key-handling path).
 */
class SentimentScorer(
    private val model: SentimentModel = SentimentModel(),
    private val dispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
) {
    suspend fun score(word: String): Float =
        kotlinx.coroutines.withContext(dispatcher) { model.score(word) }
}

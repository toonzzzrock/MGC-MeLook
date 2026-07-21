package com.example.mgc_keyboard.statscore

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory counters for a single source (keyboard or usage-monitor).
 * Cheap to mutate on a hot path; drained and reset by a periodic flush worker.
 */
class StatsAggregator {
    private val keys = AtomicInteger(0)
    private val backspace = AtomicInteger(0)
    private val sentimentSumBits = AtomicLong(0L) // Float bits, accumulated via CAS
    private val words = AtomicInteger(0)
    private val switches = AtomicInteger(0)

    fun onKeyPress(isBackspace: Boolean) {
        keys.incrementAndGet()
        if (isBackspace) backspace.incrementAndGet()
    }

    fun onWordScored(score: Float) {
        words.incrementAndGet()
        addFloat(sentimentSumBits, score)
    }

    fun onAppSwitch() {
        switches.incrementAndGet()
    }

    fun drainAndReset(): StatsDelta {
        val delta = StatsDelta(
            keys = keys.getAndSet(0),
            backspace = backspace.getAndSet(0),
            sentimentSum = Float.fromBits(sentimentSumBits.getAndSet(0L).toInt()),
            words = words.getAndSet(0),
            switches = switches.getAndSet(0)
        )
        return delta
    }

    private fun addFloat(bits: AtomicLong, value: Float) {
        while (true) {
            val current = bits.get()
            val updated = (Float.fromBits(current.toInt()) + value).toRawBits().toLong()
            if (bits.compareAndSet(current, updated)) return
        }
    }
}

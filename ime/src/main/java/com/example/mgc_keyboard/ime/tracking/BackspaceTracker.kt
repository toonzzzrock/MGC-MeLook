package com.example.mgc_keyboard.ime.tracking

import com.example.mgc_keyboard.statscore.StatsAggregator

/** Hooked into the IME's key-processing path. Pure counters, no text. */
class BackspaceTracker(private val aggregator: StatsAggregator) {

    fun onKeyProcessed(keyCode: Int) {
        aggregator.onKeyPress(isBackspace = keyCode == BACKSPACE_CODE)
    }

    companion object {
        const val BACKSPACE_CODE = -5
    }
}

/*
 * Key-code constants ported from AnySoftKeyboard
 * Copyright (c) 2009 Menny Even-Danan — Apache License 2.0
 * https://github.com/AnySoftKeyboard/AnySoftKeyboard
 *
 * Only the subset used by this project is retained.
 */
package com.example.mgc_keyboard.ime.keyboard_core

object KeyCodes {
    // Printable characters use their Unicode code points directly.

    const val SPACE = 32
    const val ENTER = 10
    const val PERIOD = 46

    const val DELETE = -5        // backspace
    const val SHIFT = -1
    const val SHIFT_LOCK = -14  // caps-lock (double-tap shift)
    const val MODE_SYMBOLS = -2  // switch to symbols/numbers layer
    const val MODE_ALPHABET = -99 // switch back to alpha layer
    const val EMOJI = -100       // emoji key (no-op for now)
}

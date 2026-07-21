package com.example.mgc_keyboard.statscore

data class StatsDelta(
    val keys: Int = 0,
    val backspace: Int = 0,
    val sentimentSum: Float = 0f,
    val words: Int = 0,
    val switches: Int = 0
)

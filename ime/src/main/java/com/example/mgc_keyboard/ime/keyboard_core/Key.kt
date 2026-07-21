/*
 * Key model inspired by AnySoftKeyboard's Keyboard.Key
 * Copyright (c) 2013 Menny Even-Danan — Apache License 2.0
 * https://github.com/AnySoftKeyboard/AnySoftKeyboard
 *
 * Significantly simplified: no addon/theme system, no popup keyboards,
 * pixel positions are baked in at layout time rather than parsed from XML.
 */
package com.example.mgc_keyboard.ime.keyboard_core

/**
 * Immutable description of a single key on the keyboard.
 *
 * [x], [y], [width], [height] are in raw pixels; they are computed by
 * [KeyboardLayout] after the view reports its measured size.
 *
 * [isSpecial] drives rendering: special keys (Shift, Delete, 123, Return)
 * get the medium-gray background; regular letter keys get white.
 *
 * [isRepeatable] is currently only set for Delete — the view posts repeat
 * callbacks while the key is held.
 */
data class Key(
    val code: Int,
    val label: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val isRepeatable: Boolean = false,
    val isSpecial: Boolean = false
)

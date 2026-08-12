package com.example.mgc_keyboard.ime.keyboard_core

import android.graphics.Color

/** Colours [MGCKeyboardView] draws with. LIGHT is the original Figma palette. */
data class KeyboardPalette(
    val board: Int,       // keyboard surround
    val keyFace: Int,     // regular letter key
    val keySpecial: Int,  // shift, del, 123, return
    val keyPressed: Int,  // any key while pressed
    val keyShadow: Int,   // bottom-edge shadow line
    val text: Int,        // label text
    val textShift: Int    // shift-active label tint
) {
    companion object {
        val LIGHT = KeyboardPalette(
            board      = Color.parseColor("#D1D5DB"),
            keyFace    = Color.WHITE,
            keySpecial = Color.parseColor("#AEB3BC"),
            keyPressed = Color.parseColor("#C2C8D0"),
            keyShadow  = Color.parseColor("#8E96A5"),
            text       = Color.parseColor("#1A1A1A"),
            textShift  = Color.parseColor("#2563EB")
        )

        val DARK = KeyboardPalette(
            board      = Color.parseColor("#0E141F"),
            keyFace    = Color.parseColor("#28313F"),
            keySpecial = Color.parseColor("#1A2231"),
            keyPressed = Color.parseColor("#3C4A5F"),
            keyShadow  = Color.parseColor("#080C13"),
            text       = Color.parseColor("#EDF1F7"),
            textShift  = Color.parseColor("#6E9BFF")
        )
    }
}

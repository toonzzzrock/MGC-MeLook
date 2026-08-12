package com.example.mgc_keyboard.statscore

import android.content.Context

/**
 * Dark theme for the IME keyboard only — independent of the app's own theme.
 *
 * SharedPreferences rather than DataStore on purpose: the IME service and the dashboard
 * Activity can run in separate processes, and DataStore gives no multi-process guarantee.
 * The keyboard re-reads this on every onStartInputView, so a change in Customize shows up
 * the next time the keyboard opens.
 */
object KeyboardThemePrefs {

    private const val FILE = "mgc_keyboard_theme"
    private const val KEY_DARK = "dark"

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    fun setDark(context: Context, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, value).apply()
    }
}

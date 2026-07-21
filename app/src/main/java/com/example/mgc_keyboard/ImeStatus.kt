package com.example.mgc_keyboard

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

object ImeStatus {
    fun isEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == context.packageName }
    }

    fun isSelected(context: Context): Boolean {
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return current?.startsWith("${context.packageName}/") == true
    }
}

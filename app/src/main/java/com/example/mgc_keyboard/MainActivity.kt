package com.example.mgc_keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mgc_keyboard.dashboard.MelookNavHost
import com.example.mgc_keyboard.usagemonitor.UsageMonitorService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UsageMonitorService.schedule(applicationContext)
        checkKeyboardStatus()
        setContent {
            MelookNavHost()
        }
    }

    private fun checkKeyboardStatus() {
        when {
            !ImeStatus.isEnabled(this) -> {
                Toast.makeText(
                    this,
                    "Enable the Mental Melook keyboard to start real tracking",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            !ImeStatus.isSelected(this) -> {
                Toast.makeText(
                    this,
                    "Switch to the Mental Melook keyboard when typing to enable live tracking",
                    Toast.LENGTH_LONG
                ).show()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        }
    }
}

package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

private const val PIN_LENGTH = 4

@Composable
private fun PinDots(filled: Int, dark: Boolean = true) {
    val filledColor = MelookColors.Accent
    val emptyColor = if (dark) MelookColors.Divider else Color(0x55FFFFFF)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(PIN_LENGTH) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) filledColor else emptyColor)
            )
        }
    }
}

/** Dots are the only visible UI; this field just captures digits off-screen. */
@Composable
private fun HiddenPinField(value: String, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(PIN_LENGTH)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier
            .size(1.dp)
            .focusRequester(focusRequester)
    )
    // requestFocus() alone doesn't reliably raise the IME for a BasicTextField —
    // without an explicit show(), users land on this screen with no way to type
    // their PIN and are stuck forever, unable to reach the dashboard.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

/** US1-4..1-7 stand-in: this app has no backend, so "account security" is a local device
 * passcode rather than a real login system. Set once at the end of onboarding. */
@Composable
fun SetPinScreen(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val awaitingConfirm = pin.length == PIN_LENGTH

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("SECURE YOUR DATA", color = MelookColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (!awaitingConfirm) "Choose a 4-digit passcode" else "Confirm your passcode",
            color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This stays on your phone only — it protects your local data, no account or server involved.",
            color = MelookColors.TextGray, fontSize = 12.sp
        )
        Spacer(Modifier.height(20.dp))

        PinDots(filled = if (!awaitingConfirm) pin.length else confirm.length)
        HiddenPinField(
            value = if (!awaitingConfirm) pin else confirm,
            onValueChange = { digits ->
                if (!awaitingConfirm) pin = digits else confirm = digits
                error = null
            }
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            enabled = if (!awaitingConfirm) pin.length == PIN_LENGTH else confirm.length == PIN_LENGTH,
            onClick = {
                if (!awaitingConfirm) return@Button
                if (confirm == pin) onPinSet(pin) else {
                    error = "Passcodes don't match, try again"
                    confirm = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (!awaitingConfirm) "Next" else "Confirm")
        }
    }
}

@Composable
fun VerifyPinScreen(expectedHash: String, verify: suspend (String, String) -> Boolean, onVerified: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            if (verify(pin, expectedHash)) {
                onVerified()
            } else {
                error = true
                pin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Navy)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Text("Enter your passcode", color = MelookColors.Surface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        PinDots(filled = pin.length, dark = false)
        HiddenPinField(
            value = pin,
            onValueChange = { digits ->
                pin = digits
                error = false
            }
        )
        if (error) {
            Spacer(Modifier.height(8.dp))
            Text("Incorrect passcode", color = MelookColors.Amber, fontSize = 12.sp)
        }
    }
}

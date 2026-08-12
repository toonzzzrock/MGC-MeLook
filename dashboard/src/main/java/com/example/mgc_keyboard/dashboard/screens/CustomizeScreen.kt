package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.statscore.KeyboardThemePrefs

/** Appearance settings. The only UI surface that calls [AppPreferencesStore.setDarkTheme]. */
@Composable
fun CustomizeScreen(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    // Keyboard theme is deliberately independent of the app theme and lives in
    // SharedPreferences, since the IME reads it from its own service.
    val context = LocalContext.current
    var keyboardDark by remember { mutableStateOf(KeyboardThemePrefs.isDark(context)) }
    Scaffold(containerColor = MelookColors.Surface) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MelookColors.Surface)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MelookColors.TextDark)
                }
                Text("Customize", color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))

            ToggleCard(
                icon = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = "Dark theme (app)",
                subtitle = if (darkTheme) "Dark background, light text" else "White background, dark text",
                checked = darkTheme,
                onCheckedChange = onDarkThemeChange
            )
            Spacer(Modifier.height(12.dp))
            ToggleCard(
                icon = Icons.Default.Keyboard,
                title = "Dark theme (keyboard)",
                subtitle = if (keyboardDark) "Dark keys, applies next time the keyboard opens"
                           else "Light keys, applies next time the keyboard opens",
                checked = keyboardDark,
                onCheckedChange = { enabled ->
                    keyboardDark = enabled
                    KeyboardThemePrefs.setDark(context, enabled)
                }
            )
        }
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MelookColors.BackgroundLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MelookColors.Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MelookColors.TextDark)
                Text(subtitle, fontSize = 12.sp, color = MelookColors.TextGray)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

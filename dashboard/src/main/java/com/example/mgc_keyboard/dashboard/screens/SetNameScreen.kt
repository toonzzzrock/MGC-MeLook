package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

@Composable
fun SetNameScreen(onNameSet: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Text("BEFORE WE START", color = MelookColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("What should we call you?", color = MelookColors.TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Just a first name — used only to greet you in your summaries, stays on your phone.",
            color = MelookColors.TextGray,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            singleLine = true,
            placeholder = { Text("Your name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            enabled = name.isNotBlank(),
            onClick = { onNameSet(name.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

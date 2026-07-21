package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

/** US7-1: transparent data-sharing screen. Mode B (clinician sharing) isn't part of this
 * build, so the honest answer is that nothing leaves the device — this screen states that
 * plainly rather than describing a sharing flow that doesn't exist yet. */
@Composable
fun DataSharingScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MelookColors.TextDark)
            }
            Text("Data sharing", color = MelookColors.TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        Surface(shape = RoundedCornerShape(16.dp), color = MelookColors.AccentSoft, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🔒  Nothing is being shared", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MelookColors.TextDark)
                Spacer(Modifier.height(6.dp))
                Text(
                    "This app has no clinician or clinic connection enabled, and it never requests internet access. All behavioral data is generated, scored, and stored only on this device.",
                    fontSize = 13.sp,
                    color = MelookColors.TextGray
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("If a clinician connection is ever enabled", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MelookColors.TextDark)
        Spacer(Modifier.height(8.dp))
        Text(
            "You would see an explicit consent step here first, and this screen would list the exact metrics being shared — never raw text, calls, or messages.",
            fontSize = 12.sp,
            color = MelookColors.TextGray
        )
        Spacer(Modifier.height(24.dp))
    }
}

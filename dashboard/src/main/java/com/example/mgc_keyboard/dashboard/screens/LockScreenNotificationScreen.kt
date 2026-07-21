package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

@Composable
fun LockScreenNotificationScreen(onOpenNotification: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Navy)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Surface(
            color = Color(0xFF17233F),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                "EXAMPLE · not a real notification yet",
                color = MelookColors.TextGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.weight(0.5f))
        Text(
            "21:47",
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Sunday 19 July",
            color = MelookColors.TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            color = Color(0xFF17233F),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenNotification() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(color = MelookColors.Accent, shape = CircleShape) {
                    Text(
                        "🤖",
                        modifier = Modifier.padding(8.dp),
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "MENTAL MELOOK · now",
                        color = MelookColors.TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Your weekly summary is ready",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Your typing pace changed vs your baseline — take a look when you have a minute.",
                        color = MelookColors.TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "tap to open",
            color = MelookColors.TextGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LockScreenIcon(Icons.Default.FlashOn)
            LockScreenIcon(Icons.Default.PhotoCamera)
        }
    }
}

@Composable
private fun LockScreenIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = Color(0xFF1B2A48), shape = CircleShape) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(14.dp)
        )
    }
}

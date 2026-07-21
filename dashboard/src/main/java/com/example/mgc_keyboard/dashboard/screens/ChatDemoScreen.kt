package com.example.mgc_keyboard.dashboard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mgc_keyboard.dashboard.MelookColors

@Composable
fun ChatDemoScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Navy)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("Alex", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }

        Text(
            "EXAMPLE CONVERSATION · here's the idea",
            color = MelookColors.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "Mental Melook reads your typing patterns in the background — not what you write. This is a mockup of what that looks like.",
            color = MelookColors.TextGray,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChatBubble(text = "how've you been? 😊", incoming = true)
            ChatBubble(text = "been busy with finals 🔥", incoming = false)
            ChatBubble(text = "wanna call later?", incoming = true)
        }

        // "learning your typing rhythm" pill
        Row(
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
        ) {
            Surface(
                color = MelookColors.NavyCard,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "🤖  Mental Melook · learning your typing rhythm",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MelookColors.NavyCard,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "I'm okay, just tired lal",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(color = MelookColors.Accent, shape = CircleShape) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⌫ backspace · 3", color = MelookColors.TextGray, fontSize = 11.sp)
            Text("⏸ pause 1.8 s", color = MelookColors.TextGray, fontSize = 11.sp)
        }

        DemoQwertyKeyboard(onNext = onNext)
    }
}

@Composable
private fun ChatBubble(text: String, incoming: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (incoming) MelookColors.BubbleIncoming else MelookColors.Accent,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (incoming) 4.dp else 16.dp,
                bottomEnd = if (incoming) 16.dp else 4.dp
            )
        ) {
            Text(
                text,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

private val KEY_ROWS = listOf(
    "q w e r t y u i o p",
    "a s d f g h j k l",
    "z x c v b n m"
)

@Composable
private fun DemoQwertyKeyboard(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1B33))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KEY_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.split(" ").forEach { key ->
                    KeyCap(key)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Color(0xFF1E2C4A),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNext() }
            ) {
                Text(
                    "space",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyCap(label: String) {
    Surface(
        color = Color(0xFF1E2C4A),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        )
    }
}

package com.example.mgc_keyboard.dashboard.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.mgc_keyboard.dashboard.MelookColors
import com.example.mgc_keyboard.usagemonitor.ForegroundSwitchWatcher
import com.example.mgc_keyboard.usagemonitor.UsageMonitorService

/** US1-2: one guided step to grant passive-sensing permissions; declining runs the app in
 * limited mode rather than blocking access. */
@Composable
fun PermissionsScreen(onContinue: (allGranted: Boolean) -> Unit) {
    val context = LocalContext.current
    val watcher = remember { ForegroundSwitchWatcher(context) }

    var usageGranted by remember { mutableStateOf(watcher.hasUsageAccess()) }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasGranted = usageGranted
                usageGranted = watcher.hasUsageAccess()
                if (usageGranted && !wasGranted) UsageMonitorService.runOnce(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MelookColors.Surface)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("ONE MORE STEP", color = MelookColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Turn on passive sensing", color = MelookColors.TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant both so the app can collect signals automatically, without any manual check-ins. You can change either later in Settings.",
            color = MelookColors.TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        PermissionRow(
            title = "Usage access",
            detail = "Lets the app see screen-time and app-switch counts (not app content).",
            granted = usageGranted
        ) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            title = "Notifications",
            detail = "Lets the app gently nudge you when a weekly summary is ready.",
            granted = notificationsGranted
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                notificationsGranted = true
            }
        }

        if (!usageGranted || !notificationsGranted) {
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = MelookColors.BackgroundLight, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Running in limited mode: skipped permissions mean fewer signals are captured, but the app still works.",
                    color = MelookColors.TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onContinue(usageGranted && notificationsGranted) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(title: String, detail: String, granted: Boolean, onGrant: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MelookColors.BackgroundLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MelookColors.TextDark)
                Text(detail, fontSize = 12.sp, color = MelookColors.TextGray)
            }
            if (granted) {
                Text("Granted", color = MelookColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else {
                TextButton(onClick = onGrant) { Text("Grant") }
            }
        }
    }
}

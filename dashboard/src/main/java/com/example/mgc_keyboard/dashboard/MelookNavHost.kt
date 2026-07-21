package com.example.mgc_keyboard.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mgc_keyboard.dashboard.screens.AllStatsScreen
import com.example.mgc_keyboard.dashboard.screens.ChatDemoScreen
import com.example.mgc_keyboard.dashboard.screens.DataSharingScreen
import com.example.mgc_keyboard.dashboard.screens.LockScreenNotificationScreen
import com.example.mgc_keyboard.dashboard.screens.OnboardingBaselineScreen
import com.example.mgc_keyboard.dashboard.screens.PermissionsScreen
import com.example.mgc_keyboard.dashboard.screens.PrivacyExplainerScreen
import com.example.mgc_keyboard.dashboard.screens.SetNameScreen
import com.example.mgc_keyboard.dashboard.screens.SetPinScreen
import com.example.mgc_keyboard.dashboard.screens.SettingsScreen
import com.example.mgc_keyboard.dashboard.screens.TrendsScreen
import com.example.mgc_keyboard.dashboard.screens.VerifyPinScreen
import com.example.mgc_keyboard.dashboard.screens.WeeklySummaryScreen
import com.example.mgc_keyboard.statscore.AuditEventType
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.launch

object MelookRoutes {
    const val SPLASH = "splash"
    const val CHAT = "chat"
    const val SET_NAME = "set_name"
    const val PRIVACY = "privacy"
    const val PRIVACY_INFO = "privacy_info"
    const val PERMISSIONS = "permissions"
    const val ONBOARDING = "onboarding"
    const val SET_PIN = "set_pin"
    const val LOCK = "lock"
    const val VERIFY_PIN = "verify_pin"
    const val SUMMARY = "summary"
    const val TRENDS = "trends"
    const val SETTINGS = "settings"
    const val DATA_SHARING = "data_sharing"
    const val ALL_STATS = "all_stats"
}

/**
 * Start destination is gated on [AppPreferencesStore]: returning users with onboarding already
 * complete skip straight to [MelookRoutes.VERIFY_PIN]/[MelookRoutes.SUMMARY] instead of replaying
 * the demo + consent + permissions + PIN-setup flow (US1-1).
 */
@Composable
fun MelookNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val prefsStore = remember { AppPreferencesStore(context) }
    val repository = remember { StatsRepository.from(StatsDatabase.getInstance(context)) }
    val scope = rememberCoroutineScope()
    val dashboardViewModel: DashboardViewModel = viewModel()

    NavHost(navController = navController, startDestination = MelookRoutes.SPLASH) {
        composable(MelookRoutes.SPLASH) {
            val prefs by prefsStore.state.collectAsState(initial = null)
            LaunchedEffect(prefs) {
                val value = prefs ?: return@LaunchedEffect
                val destination = when {
                    !value.onboardingComplete -> MelookRoutes.CHAT
                    value.pinHash != null -> MelookRoutes.VERIFY_PIN
                    else -> MelookRoutes.SUMMARY
                }
                navController.navigate(destination) {
                    popUpTo(MelookRoutes.SPLASH) { inclusive = true }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize().background(MelookColors.Navy),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_mark),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Mental Melook",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 22.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        composable(MelookRoutes.CHAT) {
            ChatDemoScreen(onNext = { navController.navigate(MelookRoutes.SET_NAME) })
        }
        composable(MelookRoutes.SET_NAME) {
            SetNameScreen(onNameSet = { name ->
                scope.launch { prefsStore.setDisplayName(name) }
                navController.navigate(MelookRoutes.PRIVACY)
            })
        }
        composable(MelookRoutes.PRIVACY) {
            PrivacyExplainerScreen(onContinue = {
                scope.launch { repository.recordAudit(AuditEventType.ONBOARDING_CONSENT_ACCEPTED, "onboarding") }
                navController.navigate(MelookRoutes.PERMISSIONS)
            })
        }
        composable(MelookRoutes.PRIVACY_INFO) {
            PrivacyExplainerScreen(standalone = true, onContinue = { navController.popBackStack() })
        }
        composable(MelookRoutes.PERMISSIONS) {
            PermissionsScreen(onContinue = { allGranted ->
                scope.launch {
                    repository.recordAudit(
                        if (allGranted) AuditEventType.PERMISSION_GRANTED else AuditEventType.PERMISSION_DENIED,
                        "usage_access+notifications"
                    )
                }
                navController.navigate(MelookRoutes.SET_PIN)
            })
        }
        composable(MelookRoutes.SET_PIN) {
            SetPinScreen(onPinSet = { pin ->
                scope.launch {
                    prefsStore.setPin(pin)
                    prefsStore.setOnboardingComplete(true)
                    repository.recordAudit(AuditEventType.PIN_SET, "")
                }
                navController.navigate(MelookRoutes.ONBOARDING) {
                    popUpTo(MelookRoutes.CHAT) { inclusive = true }
                }
            })
        }
        composable(MelookRoutes.ONBOARDING) {
            val state by dashboardViewModel.state.collectAsState()
            OnboardingBaselineScreen(
                daysCollected = state.daysOfDataCollected,
                collectedToday = state.collectedToday,
                onNext = { navController.navigate(MelookRoutes.LOCK) }
            )
        }
        composable(MelookRoutes.LOCK) {
            LockScreenNotificationScreen(onOpenNotification = {
                navController.navigate(MelookRoutes.SUMMARY) {
                    popUpTo(MelookRoutes.CHAT) { inclusive = true }
                }
            })
        }
        composable(MelookRoutes.VERIFY_PIN) {
            val prefs by prefsStore.state.collectAsState(initial = null)
            val hash = prefs?.pinHash
            if (hash != null) {
                VerifyPinScreen(
                    expectedHash = hash,
                    verify = { pin, expected -> prefsStore.verifyPin(pin, expected) },
                    onVerified = {
                        navController.navigate(MelookRoutes.SUMMARY) {
                            popUpTo(MelookRoutes.VERIFY_PIN) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable(MelookRoutes.SUMMARY) {
            val state by dashboardViewModel.state.collectAsState()
            val prefs by prefsStore.state.collectAsState(initial = null)
            Box(Modifier.fillMaxSize()) {
                WeeklySummaryScreen(
                    hasBaseline = state.hasBaseline,
                    paceChangePercent = state.paceChangePercent,
                    weekBars = state.weekBars,
                    lateNightChangePercent = state.lateNightChangePercent,
                    appVarietyLower = state.appVarietyLower,
                    showSuggestion = state.showSuggestion,
                    collectedToday = state.collectedToday,
                    displayName = prefs?.displayName ?: "there",
                    onNext = { navController.navigate(MelookRoutes.TRENDS) }
                )
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 8.dp)) {
                    IconButton(onClick = { navController.navigate(MelookRoutes.ALL_STATS) }) {
                        Icon(Icons.Default.Insights, contentDescription = "Everything we track")
                    }
                    IconButton(onClick = { navController.navigate(MelookRoutes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
        composable(MelookRoutes.TRENDS) {
            val state by dashboardViewModel.state.collectAsState()
            Box(Modifier.fillMaxSize()) {
                TrendsScreen(
                    hasEnoughWeeksForTrend = state.hasEnoughWeeksForTrend,
                    trendPoints = state.trendPoints.ifEmpty { listOf(0.5f) },
                    quietStretchHours = state.quietStretchHours,
                    quietStretchIncreased = state.quietStretchIncreased,
                    daysOfDataCollected = state.daysOfDataCollected,
                    onNext = {
                        navController.navigate(MelookRoutes.SUMMARY) {
                            popUpTo(MelookRoutes.SUMMARY) { inclusive = true }
                        }
                    }
                )
                IconButton(
                    onClick = { navController.navigate(MelookRoutes.SETTINGS) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 8.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
        composable(MelookRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacy = { navController.navigate(MelookRoutes.PRIVACY_INFO) },
                onOpenDataSharing = { navController.navigate(MelookRoutes.DATA_SHARING) },
                onOpenAllStats = { navController.navigate(MelookRoutes.ALL_STATS) }
            )
        }
        composable(MelookRoutes.DATA_SHARING) {
            DataSharingScreen(onBack = { navController.popBackStack() })
        }
        composable(MelookRoutes.ALL_STATS) {
            val state by dashboardViewModel.state.collectAsState()
            AllStatsScreen(
                hourlyActivityPattern = state.hourlyActivityPattern,
                backspaceRateBars = state.backspaceRateBars,
                sentimentTrendRecent = state.sentimentTrendRecent,
                appSwitchBars = state.appSwitchBars,
                appVarietyBars = state.appVarietyBars,
                totalKeyPressesToday = state.totalKeyPressesToday,
                totalBackspacesToday = state.totalBackspacesToday,
                totalWordsScoredToday = state.totalWordsScoredToday,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

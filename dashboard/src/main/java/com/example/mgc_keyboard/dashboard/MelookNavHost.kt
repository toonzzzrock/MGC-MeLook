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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.example.mgc_keyboard.dashboard.screens.ChatDemoScreen
import com.example.mgc_keyboard.dashboard.screens.CustomizeScreen
import com.example.mgc_keyboard.dashboard.screens.DataSharingScreen
import com.example.mgc_keyboard.dashboard.screens.HomeScreen
import com.example.mgc_keyboard.dashboard.screens.LockScreenNotificationScreen
import com.example.mgc_keyboard.dashboard.screens.MelookBottomBar
import com.example.mgc_keyboard.dashboard.screens.MelookTab
import com.example.mgc_keyboard.dashboard.screens.MetricDetailScreen
import com.example.mgc_keyboard.dashboard.screens.MetricsScreen
import com.example.mgc_keyboard.dashboard.screens.OnboardingBaselineScreen
import com.example.mgc_keyboard.dashboard.screens.PermissionsScreen
import com.example.mgc_keyboard.dashboard.screens.PrivacyExplainerScreen
import com.example.mgc_keyboard.dashboard.screens.SetNameScreen
import com.example.mgc_keyboard.dashboard.screens.SetPinScreen
import com.example.mgc_keyboard.dashboard.screens.SettingsScreen
import com.example.mgc_keyboard.dashboard.screens.TrendsScreen
import com.example.mgc_keyboard.dashboard.screens.VerifyPinScreen
import com.example.mgc_keyboard.statscore.AuditEventType
import com.example.mgc_keyboard.statscore.StatsDatabase
import com.example.mgc_keyboard.statscore.StatsRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    const val METRICS = "metrics"
    const val METRIC_DETAIL = "metric/{key}"
    const val CUSTOMIZE = "customize"

    fun metricDetail(key: String) = "metric/$key"
}

/** Tab switches must not stack: without this, four taps around the bottom bar leave four
 * entries on the back stack and the back gesture walks the tab history instead of leaving. */
private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(MelookRoutes.SUMMARY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
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

    // Set once here so every screen picks it up. MelookColors.dark drives our own palette; the
    // MaterialTheme wrap below drives the M3 components (Switch, Slider, OutlinedTextField,
    // AlertDialog, Snackbar). DataStore emits after the first frame, so a returning dark-theme
    // user gets one light frame — hidden today because SPLASH is Navy in both themes.
    val themePrefs by prefsStore.state.collectAsState(initial = null)
    LaunchedEffect(themePrefs?.darkTheme) {
        MelookColors.dark = themePrefs?.darkTheme ?: false
    }

    MaterialTheme(
        colorScheme = if (MelookColors.dark) {
            darkColorScheme(primary = MelookColors.Accent)
        } else {
            lightColorScheme(primary = MelookColors.Accent)
        }
    ) {
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
            // Once per calendar day, not once per visit: the Home tab is re-entered constantly
            // and a modal that reopens every time is a modal people learn to dismiss unread.
            val today = LocalDate.now().toEpochDay()
            val showRiskDialog = prefs != null && prefs?.riskDialogShownDay != today
            HomeScreen(
                metrics = state.metrics,
                daysOfDataCollected = state.daysOfDataCollected,
                collectedToday = state.collectedToday,
                currentHour = state.currentHour,
                showRiskDialog = showRiskDialog,
                onRiskDialogDismissed = { scope.launch { prefsStore.setRiskDialogShownDay(today) } },
                onOpenMetric = { key -> navController.navigate(MelookRoutes.metricDetail(key)) },
                onOpenMetrics = { navigateTab(navController, MelookRoutes.METRICS) },
                bottomBar = { MelookBottomBar(MelookTab.HOME) { tab -> navigateTab(navController, tab.route) } }
            )
        }
        composable(MelookRoutes.TRENDS) {
            val state by dashboardViewModel.state.collectAsState()
            TrendsScreen(
                hasEnoughWeeksForTrend = state.hasEnoughWeeksForTrend,
                trends = state.trends,
                daysOfDataCollected = state.daysOfDataCollected,
                onOpenMetric = { key -> navController.navigate(MelookRoutes.metricDetail(key)) },
                bottomBar = { MelookBottomBar(MelookTab.TRENDS) { tab -> navigateTab(navController, tab.route) } }
            )
        }
        composable(MelookRoutes.METRICS) {
            val state by dashboardViewModel.state.collectAsState()
            MetricsScreen(
                metrics = state.metrics,
                heatmapDays = state.heatmapDays,
                daysOfDataCollected = state.daysOfDataCollected,
                onOpenMetric = { key -> navController.navigate(MelookRoutes.metricDetail(key)) },
                bottomBar = { MelookBottomBar(MelookTab.METRICS) { tab -> navigateTab(navController, tab.route) } }
            )
        }
        composable(MelookRoutes.METRIC_DETAIL) { entry ->
            val state by dashboardViewModel.state.collectAsState()
            val key = entry.arguments?.getString("key").orEmpty()
            MetricDetailScreen(
                metricKey = key,
                metric = state.metrics.firstOrNull { it.key == key },
                heatmapDays = state.heatmapDays,
                hourlyActivityPattern = state.hourlyActivityPattern,
                dailyActivityPatternMonth = state.dailyActivityPatternMonth,
                onBack = { navController.popBackStack() }
            )
        }
        composable(MelookRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacy = { navController.navigate(MelookRoutes.PRIVACY_INFO) },
                onOpenDataSharing = { navController.navigate(MelookRoutes.DATA_SHARING) },
                onOpenAllStats = { navigateTab(navController, MelookRoutes.METRICS) },
                onOpenCustomize = { navController.navigate(MelookRoutes.CUSTOMIZE) }
            )
        }
        composable(MelookRoutes.DATA_SHARING) {
            val bridgePrefsStore = remember { com.example.mgc_keyboard.dashboard.bridge.ClinicalBridgePreferences(context) }
            val bridgeState by bridgePrefsStore.state.collectAsState(initial = com.example.mgc_keyboard.dashboard.bridge.ClinicalBridgeState())
            DataSharingScreen(bridgeState = bridgeState, onBack = { navController.popBackStack() })
        }
        composable(MelookRoutes.CUSTOMIZE) {
            val prefs by prefsStore.state.collectAsState(initial = null)
            CustomizeScreen(
                darkTheme = prefs?.darkTheme ?: false,
                onDarkThemeChange = { enabled -> scope.launch { prefsStore.setDarkTheme(enabled) } },
                onBack = { navController.popBackStack() }
            )
        }
    }
    }
}

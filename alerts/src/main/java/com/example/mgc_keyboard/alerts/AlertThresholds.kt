package com.example.mgc_keyboard.alerts

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.alertsDataStore by preferencesDataStore(name = "alert_thresholds")

data class AlertThresholds(
    val backspaceRateThreshold: Float = 0.40f,
    val sentimentFloor: Float = 0.30f,
    val sentimentWindowHours: Int = 3,
    val alertsEnabled: Boolean = false
)

class AlertThresholdsStore(private val context: Context) {

    val thresholds: Flow<AlertThresholds> = context.alertsDataStore.data.map { prefs ->
        AlertThresholds(
            backspaceRateThreshold = prefs[BACKSPACE_RATE_KEY] ?: 0.40f,
            sentimentFloor = prefs[SENTIMENT_FLOOR_KEY] ?: 0.30f,
            sentimentWindowHours = prefs[SENTIMENT_WINDOW_KEY] ?: 3,
            alertsEnabled = prefs[ALERTS_ENABLED_KEY] ?: false
        )
    }

    suspend fun update(thresholds: AlertThresholds) {
        context.alertsDataStore.edit { prefs ->
            prefs[BACKSPACE_RATE_KEY] = thresholds.backspaceRateThreshold
            prefs[SENTIMENT_FLOOR_KEY] = thresholds.sentimentFloor
            prefs[SENTIMENT_WINDOW_KEY] = thresholds.sentimentWindowHours
            prefs[ALERTS_ENABLED_KEY] = thresholds.alertsEnabled
        }
    }

    private companion object {
        val BACKSPACE_RATE_KEY = floatPreferencesKey("backspace_rate_threshold")
        val SENTIMENT_FLOOR_KEY = floatPreferencesKey("sentiment_floor")
        val SENTIMENT_WINDOW_KEY = intPreferencesKey("sentiment_window_hours")
        val ALERTS_ENABLED_KEY = booleanPreferencesKey("alerts_enabled")
    }
}

package com.example.mgc_keyboard.dashboard.bridge

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bridgePrefsDataStore by preferencesDataStore(name = "clinical_bridge_prefs")

data class ClinicalBridgeState(
    val enabled: Boolean = false,
    val serverUrl: String = "",
    val deviceToken: String? = null,
    val pairingCode: String? = null,
    val lastSyncAtMillis: Long? = null
)

/**
 * Settings > Clinical Bridge. Fully opt-in: [enabled] defaults false and no
 * network call happens anywhere in the app unless this is true and a
 * [deviceToken] has been obtained via [ClinicalBridgeClient.register].
 */
class ClinicalBridgePreferences(private val context: Context) {

    val state: Flow<ClinicalBridgeState> = context.bridgePrefsDataStore.data.map { prefs ->
        ClinicalBridgeState(
            enabled = prefs[ENABLED] ?: false,
            serverUrl = prefs[SERVER_URL] ?: "",
            deviceToken = prefs[DEVICE_TOKEN],
            pairingCode = prefs[PAIRING_CODE],
            lastSyncAtMillis = prefs[LAST_SYNC_AT]
        )
    }

    suspend fun setEnabled(value: Boolean) {
        context.bridgePrefsDataStore.edit { it[ENABLED] = value }
    }

    suspend fun setServerUrl(url: String) {
        context.bridgePrefsDataStore.edit { it[SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun setRegistration(deviceToken: String, pairingCode: String) {
        context.bridgePrefsDataStore.edit {
            it[DEVICE_TOKEN] = deviceToken
            it[PAIRING_CODE] = pairingCode
        }
    }

    suspend fun setLastSyncNow() {
        context.bridgePrefsDataStore.edit { it[LAST_SYNC_AT] = System.currentTimeMillis() }
    }

    suspend fun clearRegistration() {
        context.bridgePrefsDataStore.edit {
            it.remove(DEVICE_TOKEN)
            it.remove(PAIRING_CODE)
            it.remove(LAST_SYNC_AT)
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("bridge_enabled")
        val SERVER_URL = stringPreferencesKey("bridge_server_url")
        val DEVICE_TOKEN = stringPreferencesKey("bridge_device_token")
        val PAIRING_CODE = stringPreferencesKey("bridge_pairing_code")
        val LAST_SYNC_AT = longPreferencesKey("bridge_last_sync_at")
    }
}

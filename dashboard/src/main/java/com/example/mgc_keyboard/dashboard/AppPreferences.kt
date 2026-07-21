package com.example.mgc_keyboard.dashboard

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")

data class AppPrefsState(
    val onboardingComplete: Boolean = false,
    val pinHash: String? = null,
    val displayName: String? = null
)

/** US1-1 returning-user skip + US1-4..7 local-PIN stand-in (no backend account exists offline). */
class AppPreferencesStore(private val context: Context) {

    val state: Flow<AppPrefsState> = context.appPrefsDataStore.data.map { prefs ->
        AppPrefsState(
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false,
            pinHash = prefs[PIN_HASH],
            displayName = prefs[DISPLAY_NAME]
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.appPrefsDataStore.edit { it[ONBOARDING_COMPLETE] = value }
    }

    suspend fun setPin(pin: String) {
        context.appPrefsDataStore.edit { it[PIN_HASH] = hashPin(pin) }
    }

    suspend fun setDisplayName(name: String) {
        context.appPrefsDataStore.edit { it[DISPLAY_NAME] = name }
    }

    suspend fun verifyPin(pin: String, expectedHash: String): Boolean = hashPin(pin) == expectedHash

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }
}

private fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

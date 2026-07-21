package com.example.mgc_keyboard.statscore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import android.util.Base64

/** US2-3: random-per-install SQLCipher passphrase, itself protected by the Android Keystore
 * via EncryptedSharedPreferences so the raw key is never stored in plaintext. */
internal object DbPassphrase {
    private const val PREFS_NAME = "mental_melook_db_key"
    private const val KEY_ENTRY = "sqlcipher_passphrase"

    fun getOrCreate(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_ENTRY, null)
        if (existing != null) return Base64.decode(existing, Base64.NO_WRAP)

        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_ENTRY, Base64.encodeToString(fresh, Base64.NO_WRAP)).apply()
        return fresh
    }
}

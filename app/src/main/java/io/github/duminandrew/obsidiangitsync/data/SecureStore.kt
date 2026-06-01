package io.github.duminandrew.obsidiangitsync.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for the GitHub Personal Access Token (PAT).
 *
 * The PAT is the most sensitive secret in the app. It is stored ONLY inside
 * [EncryptedSharedPreferences] (AES-256-GCM, key material in the Android
 * Keystore) and is NEVER written to logs, plain prefs, or backups.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Persists the PAT in encrypted storage. Pass an empty string to clear. */
    fun saveToken(token: String) {
        prefs.edit().apply {
            if (token.isBlank()) remove(KEY_PAT) else putString(KEY_PAT, token)
            apply()
        }
    }

    /** Returns the stored PAT or `null` if none has been configured. */
    fun getToken(): String? = prefs.getString(KEY_PAT, null)?.takeIf { it.isNotBlank() }

    /** True if a non-blank PAT is configured. */
    fun hasToken(): Boolean = getToken() != null

    fun clearToken() = prefs.edit().remove(KEY_PAT).apply()

    private companion object {
        const val FILE_NAME = "ogs_secure_prefs"
        const val KEY_PAT = "github_pat"
    }
}

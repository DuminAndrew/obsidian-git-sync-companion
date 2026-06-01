package io.github.duminandrew.obsidiangitsync.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Non-secret configuration: repository coordinates, branch, the persisted SAF
 * tree URI of the Obsidian vault, and the auto-sync toggle.
 *
 * The PAT is intentionally NOT stored here — see [SecureStore].
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var repoOwner: String
        get() = prefs.getString(KEY_OWNER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_OWNER, value.trim()).apply()

    var repoName: String
        get() = prefs.getString(KEY_REPO, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_REPO, value.trim()).apply()

    var branch: String
        get() = prefs.getString(KEY_BRANCH, "main").orEmpty().ifBlank { "main" }
        set(value) = prefs.edit().putString(KEY_BRANCH, value.trim()).apply()

    /** Persisted SAF tree URI (string) of the chosen vault folder, or null. */
    var vaultTreeUri: String?
        get() = prefs.getString(KEY_VAULT_URI, null)
        set(value) = prefs.edit().putString(KEY_VAULT_URI, value).apply()

    var autoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    /** Only sync on un-metered (Wi-Fi) connections when true. */
    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    fun isConfigured(): Boolean =
        repoOwner.isNotBlank() && repoName.isNotBlank() && !vaultTreeUri.isNullOrBlank()

    private companion object {
        const val FILE_NAME = "ogs_settings"
        const val KEY_OWNER = "repo_owner"
        const val KEY_REPO = "repo_name"
        const val KEY_BRANCH = "branch"
        const val KEY_VAULT_URI = "vault_tree_uri"
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_WIFI_ONLY = "wifi_only"
    }
}

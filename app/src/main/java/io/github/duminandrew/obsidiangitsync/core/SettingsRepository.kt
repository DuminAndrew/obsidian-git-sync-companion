package io.github.duminandrew.obsidiangitsync.core

/**
 * Non-secret configuration needed to run a sync, decoupled from Android's
 * `SharedPreferences`. The concrete on-device store implements [load]/[save];
 * the pure validation/derivation logic below is testable on the JVM.
 */
data class SyncConfig(
    val repoOwner: String = "",
    val repoName: String = "",
    val branch: String = "main",
    val vaultUri: String? = null,
    val autoSyncEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
) {
    /** Branch falls back to "main" when blank. */
    val effectiveBranch: String get() = branch.ifBlank { "main" }

    /** Repo + vault all set: a sync has everything except possibly the token. */
    val isConfigured: Boolean
        get() = repoOwner.isNotBlank() && repoName.isNotBlank() && !vaultUri.isNullOrBlank()
}

/** Persistence seam for [SyncConfig]; faked in unit tests. */
interface SettingsStorage {
    fun load(): SyncConfig
    fun save(config: SyncConfig)
}

/**
 * Coordinates the PAT ([TokenStore]) and non-secret config ([SettingsStorage])
 * and exposes the small amount of derived logic the UI/engine relies on. Pure
 * and fully testable with in-memory fakes.
 */
class SettingsRepository(
    private val tokenStore: TokenStore,
    private val settingsStorage: SettingsStorage,
) {
    fun config(): SyncConfig = settingsStorage.load()

    fun updateConfig(transform: (SyncConfig) -> SyncConfig) {
        settingsStorage.save(transform(settingsStorage.load()).normalized())
    }

    fun saveToken(token: String) = tokenStore.saveToken(token.trim())

    fun clearToken() = tokenStore.clearToken()

    fun hasToken(): Boolean = tokenStore.hasToken()

    /** True only when repo + vault are set AND a token is present. */
    fun isReadyToSync(): Boolean = config().isConfigured && hasToken()

    /**
     * A human-readable reason the app is not ready to sync, or `null` if it is.
     * Mirrors the guard checks performed at the top of the sync engine.
     */
    fun blockingReason(): String? {
        val c = config()
        return when {
            !hasToken() -> "No GitHub token configured."
            c.repoOwner.isBlank() || c.repoName.isBlank() -> "Repository owner/name not set."
            c.vaultUri.isNullOrBlank() -> "Vault folder not selected."
            else -> null
        }
    }
}

/** Trims free-text fields and applies the branch fallback. */
fun SyncConfig.normalized(): SyncConfig = copy(
    repoOwner = repoOwner.trim(),
    repoName = repoName.trim(),
    branch = branch.trim().ifBlank { "main" },
    vaultUri = vaultUri?.trim()?.ifBlank { null },
)

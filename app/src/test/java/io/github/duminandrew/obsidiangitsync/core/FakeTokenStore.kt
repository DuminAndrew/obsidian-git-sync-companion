package io.github.duminandrew.obsidiangitsync.core

/**
 * In-memory [TokenStore] for unit tests. This deliberately replaces the
 * production `EncryptedSharedPreferences`-backed store so tests never touch the
 * Android Keystore or real encrypted preferences.
 */
class FakeTokenStore(initial: String? = null) : TokenStore {
    private var token: String? = initial

    override fun getToken(): String? = token?.takeIf { it.isNotBlank() }

    override fun saveToken(token: String) {
        this.token = token.ifBlank { null }
    }

    override fun clearToken() {
        token = null
    }
}

/** In-memory [SettingsStorage] for unit tests. */
class FakeSettingsStorage(initial: SyncConfig = SyncConfig()) : SettingsStorage {
    private var config: SyncConfig = initial
    var saveCount: Int = 0
        private set

    override fun load(): SyncConfig = config

    override fun save(config: SyncConfig) {
        this.config = config
        saveCount++
    }
}

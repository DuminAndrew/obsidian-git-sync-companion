package io.github.duminandrew.obsidiangitsync.sync

import android.content.Context
import android.net.Uri
import io.github.duminandrew.obsidiangitsync.data.SecureStore
import io.github.duminandrew.obsidiangitsync.data.SettingsStore
import io.github.duminandrew.obsidiangitsync.data.SyncStateStore
import io.github.duminandrew.obsidiangitsync.data.VaultStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for sync. Builds a [SyncEngine] on demand and exposes
 * the latest [SyncResult] (and a running flag) as flows the UI can observe,
 * regardless of whether the run was triggered manually or by WorkManager.
 *
 * Process-singleton via [getInstance].
 */
class SyncRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val secureStore = SecureStore(appContext)
    val settings = SettingsStore(appContext)
    private val syncState = SyncStateStore(appContext)

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _lastResult = MutableStateFlow<SyncResult?>(null)
    val lastResult: StateFlow<SyncResult?> = _lastResult.asStateFlow()

    val hasToken: Boolean get() = secureStore.hasToken()

    fun saveToken(token: String) = secureStore.saveToken(token)
    fun clearToken() = secureStore.clearToken()

    /** Resets the sync baseline; the next run treats everything as new. */
    fun resetSyncState() = syncState.clear()

    suspend fun runSync(): SyncResult {
        if (_running.value) {
            return _lastResult.value ?: SyncResult(false, error = "Sync already running.")
        }
        _running.value = true
        try {
            val engine = SyncEngine(
                secureStore = secureStore,
                settings = settings,
                syncState = syncState,
                vaultFactory = { uri: Uri -> VaultStorage(appContext, uri) },
            )
            val result = engine.sync()
            _lastResult.value = result
            return result
        } finally {
            _running.value = false
        }
    }

    companion object {
        @Volatile
        private var instance: SyncRepository? = null

        fun getInstance(context: Context): SyncRepository =
            instance ?: synchronized(this) {
                instance ?: SyncRepository(context).also { instance = it }
            }
    }
}

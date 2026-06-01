package io.github.duminandrew.obsidiangitsync.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.duminandrew.obsidiangitsync.sync.SyncRepository
import io.github.duminandrew.obsidiangitsync.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel mediating between the Compose UI and [SyncRepository].
 * Holds [SyncUiState] and persists settings through the repository's stores.
 */
class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SyncRepository.getInstance(app)
    private val settings get() = repo.settings

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        // Reflect repository running/result flags into UI state.
        viewModelScope.launch {
            combine(repo.running, repo.lastResult) { running, result -> running to result }
                .collect { (running, result) ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = running,
                        lastResult = result ?: _uiState.value.lastResult,
                    )
                }
        }
    }

    private fun loadInitialState(): SyncUiState = SyncUiState(
        repoOwner = settings.repoOwner,
        repoName = settings.repoName,
        branch = settings.branch,
        tokenEntered = repo.hasToken,
        vaultUri = settings.vaultTreeUri,
        vaultLabel = settings.vaultTreeUri?.let { labelFor(Uri.parse(it)) },
        autoSyncEnabled = settings.autoSyncEnabled,
        wifiOnly = settings.wifiOnly,
        lastResult = repo.lastResult.value,
    )

    fun onOwnerChange(value: String) {
        settings.repoOwner = value
        _uiState.value = _uiState.value.copy(repoOwner = value)
    }

    fun onRepoChange(value: String) {
        settings.repoName = value
        _uiState.value = _uiState.value.copy(repoName = value)
    }

    fun onBranchChange(value: String) {
        settings.branch = value
        _uiState.value = _uiState.value.copy(branch = value)
    }

    /** Stores the PAT in encrypted storage. The plaintext is never kept in UI state. */
    fun onTokenEntered(token: String) {
        repo.saveToken(token)
        _uiState.value = _uiState.value.copy(
            tokenEntered = repo.hasToken,
            message = if (repo.hasToken) "Token saved (encrypted)." else "Token cleared.",
        )
    }

    fun onClearToken() {
        repo.clearToken()
        _uiState.value = _uiState.value.copy(tokenEntered = false, message = "Token cleared.")
    }

    /** Persist the SAF tree URI and take a persistable permission grant. */
    fun onVaultSelected(uri: Uri, flags: Int) {
        val resolver = getApplication<Application>().contentResolver
        val takeFlags = flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { resolver.takePersistableUriPermission(uri, takeFlags) }
        settings.vaultTreeUri = uri.toString()
        _uiState.value = _uiState.value.copy(
            vaultUri = uri.toString(),
            vaultLabel = labelFor(uri),
            message = "Vault folder selected.",
        )
    }

    fun onAutoSyncToggle(enabled: Boolean) {
        settings.autoSyncEnabled = enabled
        if (enabled) {
            SyncScheduler.enablePeriodic(getApplication(), settings.wifiOnly)
        } else {
            SyncScheduler.disablePeriodic(getApplication())
        }
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
    }

    fun onWifiOnlyToggle(wifiOnly: Boolean) {
        settings.wifiOnly = wifiOnly
        if (settings.autoSyncEnabled) {
            SyncScheduler.enablePeriodic(getApplication(), wifiOnly)
        }
        _uiState.value = _uiState.value.copy(wifiOnly = wifiOnly)
    }

    /** Run a sync immediately in-process (results stream back via the repository). */
    fun syncNow() {
        if (!_uiState.value.isConfigured) {
            _uiState.value = _uiState.value.copy(
                message = "Set repo, branch, token and vault folder first.",
            )
            return
        }
        viewModelScope.launch { repo.runSync() }
    }

    fun resetBaseline() {
        repo.resetSyncState()
        _uiState.value = _uiState.value.copy(message = "Sync baseline reset.")
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun labelFor(uri: Uri): String {
        // Tree URIs end with .../tree/<docId>; show the last path segment readably.
        val raw = uri.lastPathSegment ?: uri.toString()
        return raw.substringAfterLast(':').substringAfterLast('/').ifBlank { raw }
    }
}

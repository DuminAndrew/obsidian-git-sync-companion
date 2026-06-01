package io.github.duminandrew.obsidiangitsync.ui

import io.github.duminandrew.obsidiangitsync.sync.SyncResult

/** Immutable UI state for the single-screen app. */
data class SyncUiState(
    val repoOwner: String = "",
    val repoName: String = "",
    val branch: String = "main",
    val tokenEntered: Boolean = false,   // a PAT is stored (we never expose the value)
    val vaultUri: String? = null,
    val vaultLabel: String? = null,      // human-readable folder name
    val autoSyncEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val isSyncing: Boolean = false,
    val lastResult: SyncResult? = null,
    val message: String? = null,         // transient one-shot message for snackbar
) {
    val isConfigured: Boolean
        get() = repoOwner.isNotBlank() && repoName.isNotBlank() &&
            !vaultUri.isNullOrBlank() && tokenEntered
}

package io.github.duminandrew.obsidiangitsync.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.duminandrew.obsidiangitsync.sync.SyncLogEntry
import io.github.duminandrew.obsidiangitsync.sync.SyncResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(viewModel: SyncViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Surface transient messages from the ViewModel.
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val openTree = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            viewModel.onVaultSelected(uri, flags)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Obsidian Git-Sync") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RepoCard(state, viewModel)
            TokenCard(state, viewModel)
            VaultCard(state, onPick = { openTree.launch(null) })
            AutoSyncCard(state, viewModel)
            ActionsCard(state, viewModel)
            StatusCard(state.lastResult, state.isSyncing)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun RepoCard(state: SyncUiState, vm: SyncViewModel) {
    SectionCard("GitHub repository") {
        OutlinedTextField(
            value = state.repoOwner,
            onValueChange = vm::onOwnerChange,
            label = { Text("Owner (user or org)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.repoName,
            onValueChange = vm::onRepoChange,
            label = { Text("Repository name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.branch,
            onValueChange = vm::onBranchChange,
            label = { Text("Branch") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TokenCard(state: SyncUiState, vm: SyncViewModel) {
    var token by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }

    SectionCard("Personal Access Token") {
        Text(
            if (state.tokenEntered) "A token is stored (encrypted on-device)."
            else "No token stored yet.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("GitHub PAT (scope: repo / contents)") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle token visibility",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    vm.onTokenEntered(token.trim())
                    token = ""
                    visible = false
                },
                enabled = token.isNotBlank(),
            ) { Text("Save token") }
            if (state.tokenEntered) {
                OutlinedButton(onClick = {
                    vm.onClearToken()
                    token = ""
                }) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun VaultCard(state: SyncUiState, onPick: () -> Unit) {
    SectionCard("Obsidian vault folder") {
        Text(
            state.vaultLabel?.let { "Selected: $it" } ?: "No folder selected.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(if (state.vaultUri == null) "Choose vault folder" else "Change folder")
        }
    }
}

@Composable
private fun AutoSyncCard(state: SyncUiState, vm: SyncViewModel) {
    SectionCard("Background auto-sync") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Periodic sync (every ~15 min)")
            Switch(checked = state.autoSyncEnabled, onCheckedChange = vm::onAutoSyncToggle)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Wi-Fi only (un-metered)")
            Switch(checked = state.wifiOnly, onCheckedChange = vm::onWifiOnlyToggle)
        }
    }
}

@Composable
private fun ActionsCard(state: SyncUiState, vm: SyncViewModel) {
    SectionCard("Actions") {
        Button(
            onClick = vm::syncNow,
            enabled = state.isConfigured && !state.isSyncing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(8.dp))
                Text("Syncing…")
            } else {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Synchronize now")
            }
        }
        TextButton(onClick = vm::resetBaseline, enabled = !state.isSyncing) {
            Text("Reset sync baseline")
        }
    }
}

@Composable
private fun StatusCard(result: SyncResult?, isSyncing: Boolean) {
    SectionCard("Status & log") {
        when {
            isSyncing -> Text("Sync in progress…")
            result == null -> Text("No sync run yet.")
            else -> {
                val summary = if (result.success) {
                    "OK — pushed ${result.pushed}, pulled ${result.pulled}, " +
                        "conflicts ${result.conflicts}, skipped ${result.skipped}"
                } else {
                    "Failed: ${result.error ?: "unknown error"}"
                }
                Text(summary, style = MaterialTheme.typography.bodyMedium)
                if (result.logs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                        result.logs.forEach { entry ->
                            Text(
                                text = "${fmt.format(Date(entry.timestampMillis))}  " +
                                    "[${entry.level.tag()}] ${entry.message}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SyncLogEntry.Level.tag(): String = when (this) {
    SyncLogEntry.Level.INFO -> "INFO"
    SyncLogEntry.Level.PUSH -> "PUSH"
    SyncLogEntry.Level.PULL -> "PULL"
    SyncLogEntry.Level.CONFLICT -> "CONF"
    SyncLogEntry.Level.ERROR -> "ERR "
}

package io.github.duminandrew.obsidiangitsync.sync

import android.net.Uri
import android.util.Base64
import io.github.duminandrew.obsidiangitsync.data.SecureStore
import io.github.duminandrew.obsidiangitsync.data.SettingsStore
import io.github.duminandrew.obsidiangitsync.data.SyncStateStore
import io.github.duminandrew.obsidiangitsync.data.VaultStorage
import io.github.duminandrew.obsidiangitsync.data.github.GitHubApi
import io.github.duminandrew.obsidiangitsync.data.github.GitHubClient
import io.github.duminandrew.obsidiangitsync.data.github.PutContentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Conflict-free two-way sync between the local Obsidian vault (SAF) and a
 * private GitHub repo (REST Git Data + Contents API).
 *
 * Three-way comparison per path using:
 *   base   = last-synced git blob SHA ([SyncStateStore])
 *   local  = git blob SHA of current vault bytes ([GitBlobSha])
 *   remote = blob SHA from the GitHub recursive tree
 *
 * Decision table:
 *   local==base & remote==base                 -> no-op
 *   local!=base & remote==base                 -> push local
 *   local==base & remote!=base                 -> pull remote
 *   local!=base & remote!=base & local!=remote -> CONFLICT (keep-both)
 *   missing locally, present remote            -> pull (new)
 *   present locally, missing remote            -> push (new)
 *
 * Conflict resolution (never lose data, never silently overwrite):
 *   1. Write the REMOTE version to "<name> (conflict <ts>).md" in the vault.
 *   2. Push the LOCAL version to the repo (so remote becomes == local).
 *   3. Advance base SHA. User merges the two files inside Obsidian.
 */
class SyncEngine(
    private val secureStore: SecureStore,
    private val settings: SettingsStore,
    private val syncState: SyncStateStore,
    private val vaultFactory: (Uri) -> VaultStorage,
) {
    private val logs = mutableListOf<SyncLogEntry>()
    private var pushed = 0
    private var pulled = 0
    private var conflicts = 0
    private var skipped = 0

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        logs.clear(); pushed = 0; pulled = 0; conflicts = 0; skipped = 0

        val token = secureStore.getToken()
            ?: return@withContext fail("No GitHub token configured.")
        if (!settings.isConfigured()) {
            return@withContext fail("Repository or vault folder not configured.")
        }

        val owner = settings.repoOwner
        val repo = settings.repoName
        val branch = settings.branch
        val vaultUri = Uri.parse(settings.vaultTreeUri)
        val vault = vaultFactory(vaultUri)
        val api = GitHubClient.create(token)

        try {
            log(SyncLogEntry.Level.INFO, "Resolving branch '$branch'…")
            val ref = api.getBranchRef(owner, repo, branch).requireBody("getBranchRef")
            val commit = api.getCommit(owner, repo, ref.`object`.sha).requireBody("getCommit")
            val tree = api.getTree(owner, repo, commit.tree.sha).requireBody("getTree")

            if (tree.truncated) {
                log(SyncLogEntry.Level.INFO,
                    "Warning: repo tree truncated by GitHub; very large repos may not fully sync.")
            }

            // remote path -> blob SHA (files only)
            val remote: Map<String, String> = tree.tree
                .filter { it.type == "blob" && it.sha != null }
                .filter { !it.path.startsWith(".git") }
                .associate { it.path to it.sha!! }

            val localPaths = vault.listFiles().toSet()
            val allPaths = (remote.keys + localPaths).toSortedSet()

            log(SyncLogEntry.Level.INFO,
                "Comparing ${allPaths.size} paths (local=${localPaths.size}, remote=${remote.size}).")

            for (path in allPaths) {
                val remoteSha = remote[path]
                val localBytes = if (path in localPaths) vault.readBytes(path) else null
                val localSha = localBytes?.let { GitBlobSha.of(it) }
                val baseSha = syncState.baseSha(path)

                reconcile(api, vault, owner, repo, branch, path, localBytes, localSha, remoteSha, baseSha)
            }

            log(SyncLogEntry.Level.INFO,
                "Done. pushed=$pushed pulled=$pulled conflicts=$conflicts skipped=$skipped")
            SyncResult(true, pushed, pulled, conflicts, skipped, logs.toList())
        } catch (e: SyncException) {
            fail(e.message ?: "Sync failed.")
        } catch (e: Exception) {
            // Never include the token; exception messages from OkHttp/Retrofit don't carry it.
            fail("Unexpected error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private suspend fun reconcile(
        api: GitHubApi,
        vault: VaultStorage,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        localBytes: ByteArray?,
        localSha: String?,
        remoteSha: String?,
        baseSha: String?,
    ) {
        when {
            // Present on both sides.
            localSha != null && remoteSha != null -> {
                when {
                    localSha == remoteSha -> {
                        // Already identical; just record base.
                        if (baseSha != remoteSha) syncState.setBaseSha(path, remoteSha)
                    }
                    localSha != baseSha && remoteSha == baseSha ->
                        push(api, owner, repo, branch, path, localBytes!!, remoteSha)
                    localSha == baseSha && remoteSha != baseSha ->
                        pull(api, vault, owner, repo, branch, path, remoteSha)
                    else -> // both diverged from base -> conflict
                        resolveConflict(api, vault, owner, repo, branch, path, localBytes!!, remoteSha)
                }
            }
            // Only local exists -> push as new file.
            localSha != null && remoteSha == null ->
                push(api, owner, repo, branch, path, localBytes!!, currentRemoteSha = null)

            // Only remote exists -> pull as new file.
            localSha == null && remoteSha != null ->
                pull(api, vault, owner, repo, branch, path, remoteSha)
        }
    }

    private suspend fun push(
        api: GitHubApi,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        bytes: ByteArray,
        currentRemoteSha: String?,
    ) {
        val body = PutContentRequest(
            message = "Sync: update $path",
            content = Base64.encodeToString(bytes, Base64.NO_WRAP),
            branch = branch,
            sha = currentRemoteSha,
        )
        val resp = api.putContent(owner, repo, encodePath(path), body).requireBody("putContent($path)")
        val newSha = resp.content?.sha ?: GitBlobSha.of(bytes)
        syncState.setBaseSha(path, newSha)
        pushed++
        log(SyncLogEntry.Level.PUSH, "Pushed: $path")
    }

    private suspend fun pull(
        api: GitHubApi,
        vault: VaultStorage,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        remoteSha: String,
    ) {
        val content = api.getContent(owner, repo, encodePath(path), branch)
            .requireBody("getContent($path)")
        val bytes = decodeContent(content.content)
        if (bytes == null) {
            skipped++
            log(SyncLogEntry.Level.INFO, "Skipped (un-decodable content): $path")
            return
        }
        if (vault.writeBytes(path, bytes)) {
            syncState.setBaseSha(path, remoteSha)
            pulled++
            log(SyncLogEntry.Level.PULL, "Pulled: $path")
        } else {
            skipped++
            log(SyncLogEntry.Level.ERROR, "Failed to write locally: $path")
        }
    }

    private suspend fun resolveConflict(
        api: GitHubApi,
        vault: VaultStorage,
        owner: String,
        repo: String,
        branch: String,
        path: String,
        localBytes: ByteArray,
        remoteSha: String,
    ) {
        // 1. Save the remote version next to the local one (keep-both).
        val remote = api.getContent(owner, repo, encodePath(path), branch)
            .requireBody("getContent(conflict $path)")
        val remoteBytes = decodeContent(remote.content)
        val conflictPath = conflictName(path)
        if (remoteBytes != null) {
            vault.writeBytes(conflictPath, remoteBytes)
        }
        // 2. Push local so remote == local going forward.
        val body = PutContentRequest(
            message = "Sync: resolve conflict on $path (remote kept as $conflictPath)",
            content = Base64.encodeToString(localBytes, Base64.NO_WRAP),
            branch = branch,
            sha = remoteSha,
        )
        val resp = api.putContent(owner, repo, encodePath(path), body)
            .requireBody("putContent(conflict $path)")
        val newSha = resp.content?.sha ?: GitBlobSha.of(localBytes)
        syncState.setBaseSha(path, newSha)
        conflicts++
        log(SyncLogEntry.Level.CONFLICT,
            "Conflict on $path -> kept local; remote saved as $conflictPath")
    }

    /** Inserts " (conflict <timestamp>)" before the file extension. */
    private fun conflictName(path: String): String {
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val dot = path.lastIndexOf('.')
        val slash = path.lastIndexOf('/')
        return if (dot > slash) {
            path.substring(0, dot) + " (conflict $ts)" + path.substring(dot)
        } else {
            "$path (conflict $ts)"
        }
    }

    private fun decodeContent(content: String?): ByteArray? {
        if (content.isNullOrEmpty()) return null
        return try {
            Base64.decode(content.replace("\n", ""), Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Percent-encode each path segment but keep "/" separators for the API. */
    private fun encodePath(path: String): String =
        path.split('/').joinToString("/") { Uri.encode(it) }

    private fun log(level: SyncLogEntry.Level, message: String) {
        logs += SyncLogEntry(System.currentTimeMillis(), level, message)
    }

    private fun fail(message: String): SyncResult {
        log(SyncLogEntry.Level.ERROR, message)
        return SyncResult(false, pushed, pulled, conflicts, skipped, logs.toList(), message)
    }

    private fun <T> Response<T>.requireBody(op: String): T {
        if (!isSuccessful) {
            throw SyncException("$op failed: HTTP $code() ${message()}")
        }
        return body() ?: throw SyncException("$op returned empty body")
    }

    private class SyncException(message: String) : Exception(message)
}

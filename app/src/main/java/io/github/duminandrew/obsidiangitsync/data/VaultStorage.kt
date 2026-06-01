package io.github.duminandrew.obsidiangitsync.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream

/**
 * Reads and writes files inside the user-selected Obsidian vault folder using
 * the Storage Access Framework (SAF) / [DocumentFile]. No storage permission
 * is required — access comes from a persisted tree-URI grant.
 *
 * All paths are repo-relative POSIX paths ("notes/todo.md") to line up with
 * the GitHub tree paths.
 */
class VaultStorage(
    private val context: Context,
    treeUri: Uri,
) {
    private val root: DocumentFile = requireNotNull(
        DocumentFile.fromTreeUri(context.applicationContext, treeUri)
    ) { "Could not open vault tree URI" }

    /** Lists every regular file in the vault as repo-relative paths. */
    fun listFiles(): List<String> {
        val out = mutableListOf<String>()
        walk(root, "", out)
        return out
    }

    private fun walk(dir: DocumentFile, prefix: String, out: MutableList<String>) {
        for (child in dir.listFiles()) {
            val name = child.name ?: continue
            // Skip the local git plumbing and OS noise; sync only vault content.
            if (name == ".git" || name == ".trash" || name == ".obsidian.lock") continue
            val rel = if (prefix.isEmpty()) name else "$prefix/$name"
            if (child.isDirectory) {
                walk(child, rel, out)
            } else {
                out += rel
            }
        }
    }

    fun exists(path: String): Boolean = findFile(path) != null

    fun readBytes(path: String): ByteArray? {
        val file = findFile(path) ?: return null
        return context.contentResolver.openInputStream(file.uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            input.copyTo(buffer)
            buffer.toByteArray()
        }
    }

    /** Writes [bytes] to [path], creating parent directories as needed. */
    fun writeBytes(path: String, bytes: ByteArray): Boolean {
        val segments = path.split('/')
        var dir = root
        for (i in 0 until segments.size - 1) {
            dir = dir.findFile(segments[i])?.takeIf { it.isDirectory }
                ?: dir.createDirectory(segments[i])
                ?: return false
        }
        val fileName = segments.last()
        val existing = dir.findFile(fileName)
        val target = existing ?: dir.createFile(mimeFor(fileName), fileName) ?: return false
        return context.contentResolver.openOutputStream(target.uri, "wt")?.use { output ->
            output.write(bytes)
            output.flush()
            true
        } ?: false
    }

    private fun findFile(path: String): DocumentFile? {
        val segments = path.split('/')
        var node: DocumentFile? = root
        for (seg in segments) {
            node = node?.findFile(seg) ?: return null
        }
        return node?.takeIf { it.isFile }
    }

    private fun mimeFor(fileName: String): String = when {
        fileName.endsWith(".md", true) -> "text/markdown"
        fileName.endsWith(".txt", true) -> "text/plain"
        fileName.endsWith(".json", true) -> "application/json"
        fileName.endsWith(".png", true) -> "image/png"
        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
        fileName.endsWith(".pdf", true) -> "application/pdf"
        else -> "application/octet-stream"
    }
}

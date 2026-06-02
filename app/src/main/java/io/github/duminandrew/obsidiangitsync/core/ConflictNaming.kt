package io.github.duminandrew.obsidiangitsync.core

/**
 * Pure logic for the "keep-both" conflict filename used when local and remote
 * versions of a path have both diverged from the last-synced base.
 *
 * Inserts " (conflict <timestamp>)" immediately before the file extension so the
 * conflicted copy sits next to the original in the vault and remains a valid
 * Markdown/asset file. The timestamp is injected for deterministic testing.
 */
object ConflictNaming {

    /**
     * @param path repo-relative POSIX path, e.g. "notes/todo.md".
     * @param timestamp marker inserted into the name, e.g. "20260602-101500".
     */
    fun conflictName(path: String, timestamp: String): String {
        require(path.isNotBlank()) { "path must not be blank" }
        val dot = path.lastIndexOf('.')
        val slash = path.lastIndexOf('/')
        // Only treat the dot as an extension if it is in the final segment and
        // not a leading dot (dotfiles like ".gitignore" have no real extension).
        return if (dot > slash + 1 && dot > 0) {
            path.substring(0, dot) + " (conflict $timestamp)" + path.substring(dot)
        } else {
            "$path (conflict $timestamp)"
        }
    }
}

package io.github.duminandrew.obsidiangitsync.sync

/** A single human-readable log line produced during a sync run. */
data class SyncLogEntry(
    val timestampMillis: Long,
    val level: Level,
    val message: String,
) {
    enum class Level { INFO, PUSH, PULL, CONFLICT, ERROR }
}

/** Aggregate outcome of one sync run. */
data class SyncResult(
    val success: Boolean,
    val pushed: Int = 0,
    val pulled: Int = 0,
    val conflicts: Int = 0,
    val skipped: Int = 0,
    val logs: List<SyncLogEntry> = emptyList(),
    val error: String? = null,
)

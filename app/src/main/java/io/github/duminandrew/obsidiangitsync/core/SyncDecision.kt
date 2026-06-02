package io.github.duminandrew.obsidiangitsync.core

/**
 * The action to take for a single path after comparing three git blob SHAs:
 *
 *  - **base**   last-synced SHA (null if the path was never synced),
 *  - **local**  SHA of current vault bytes (null if the file is absent locally),
 *  - **remote** SHA from the GitHub recursive tree (null if absent on the remote).
 */
enum class SyncAction {
    /** Both sides match (or nothing to do); only the base SHA may need recording. */
    NOOP,

    /** Local changed, remote unchanged -> upload local to the repo. */
    PUSH,

    /** Remote changed, local unchanged -> download remote into the vault. */
    PULL,

    /**
     * Both sides diverged from base and differ from each other -> keep-both
     * conflict: save remote alongside local, then push local.
     */
    CONFLICT,
}

/**
 * Framework-free three-way reconciliation used by the sync engine. This encodes
 * exactly the decision table documented on `SyncEngine`, in a form that is fully
 * deterministic and unit-testable on the JVM (no Android, no network).
 *
 * The function never decides to DELETE: a file missing on one side is treated as
 * "new on the other side" and propagated, matching the engine's conservative,
 * data-loss-averse behaviour.
 */
object SyncDecision {

    fun decide(base: String?, local: String?, remote: String?): SyncAction = when {
        // Present on both sides.
        local != null && remote != null -> when {
            local == remote -> SyncAction.NOOP
            local != base && remote == base -> SyncAction.PUSH
            local == base && remote != base -> SyncAction.PULL
            // Both diverged from base (or there was no base) and they differ.
            else -> SyncAction.CONFLICT
        }

        // Only local exists -> push as a new file.
        local != null && remote == null -> SyncAction.PUSH

        // Only remote exists -> pull as a new file.
        local == null && remote != null -> SyncAction.PULL

        // Neither side has the file: nothing to do.
        else -> SyncAction.NOOP
    }
}

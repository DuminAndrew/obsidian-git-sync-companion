package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDecisionTest {

    private val base = "BASE"
    private val other = "OTHER"
    private val third = "THIRD"

    @Test
    fun `identical local and remote is a no-op`() {
        assertEquals(SyncAction.NOOP, SyncDecision.decide(base = base, local = base, remote = base))
    }

    @Test
    fun `identical local and remote is a no-op even if base lags`() {
        // Two devices already converged; only the base SHA needs recording.
        assertEquals(SyncAction.NOOP, SyncDecision.decide(base = null, local = other, remote = other))
    }

    @Test
    fun `local changed remote unchanged pushes`() {
        assertEquals(SyncAction.PUSH, SyncDecision.decide(base = base, local = other, remote = base))
    }

    @Test
    fun `remote changed local unchanged pulls`() {
        assertEquals(SyncAction.PULL, SyncDecision.decide(base = base, local = base, remote = other))
    }

    @Test
    fun `both diverged and differ is a conflict`() {
        assertEquals(SyncAction.CONFLICT, SyncDecision.decide(base = base, local = other, remote = third))
    }

    @Test
    fun `both changed identically converges to no-op`() {
        // local == remote takes precedence over the conflict branch.
        assertEquals(SyncAction.NOOP, SyncDecision.decide(base = base, local = other, remote = other))
    }

    @Test
    fun `file only present locally is pushed as new`() {
        assertEquals(SyncAction.PUSH, SyncDecision.decide(base = null, local = other, remote = null))
    }

    @Test
    fun `file only present remotely is pulled as new`() {
        assertEquals(SyncAction.PULL, SyncDecision.decide(base = null, local = null, remote = other))
    }

    @Test
    fun `file absent on both sides is a no-op`() {
        assertEquals(SyncAction.NOOP, SyncDecision.decide(base = base, local = null, remote = null))
    }

    @Test
    fun `new on both sides with differing content conflicts`() {
        // Never synced before (base null) and both created the file differently.
        assertEquals(SyncAction.CONFLICT, SyncDecision.decide(base = null, local = other, remote = third))
    }

    @Test
    fun `decision is deterministic and idempotent after a push`() {
        // After pushing local, a rerun where base advanced to local and remote == local is a no-op.
        val afterPushBase = other
        assertEquals(
            SyncAction.NOOP,
            SyncDecision.decide(base = afterPushBase, local = other, remote = other),
        )
    }
}

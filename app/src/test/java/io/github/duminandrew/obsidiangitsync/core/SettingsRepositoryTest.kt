package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    private fun repo(
        token: String? = null,
        config: SyncConfig = SyncConfig(),
    ) = SettingsRepository(FakeTokenStore(token), FakeSettingsStorage(config))

    @Test
    fun `not ready when nothing is configured`() {
        val r = repo()
        assertFalse(r.isReadyToSync())
        assertEquals("No GitHub token configured.", r.blockingReason())
    }

    @Test
    fun `blocking reason reports missing repo before missing vault`() {
        val r = repo(
            token = "ghp_x",
            config = SyncConfig(repoOwner = "", repoName = "", vaultUri = null),
        )
        assertEquals("Repository owner/name not set.", r.blockingReason())
    }

    @Test
    fun `blocking reason reports missing vault when repo is set`() {
        val r = repo(
            token = "ghp_x",
            config = SyncConfig(repoOwner = "o", repoName = "r", vaultUri = null),
        )
        assertEquals("Vault folder not selected.", r.blockingReason())
    }

    @Test
    fun `ready to sync when token repo and vault are all present`() {
        val r = repo(
            token = "ghp_x",
            config = SyncConfig(repoOwner = "o", repoName = "r", vaultUri = "content://vault"),
        )
        assertTrue(r.isReadyToSync())
        assertNull(r.blockingReason())
    }

    @Test
    fun `saveToken trims whitespace and hasToken reflects it`() {
        val r = repo()
        assertFalse(r.hasToken())
        r.saveToken("  ghp_token  ")
        assertTrue(r.hasToken())
    }

    @Test
    fun `blank token does not count as configured`() {
        val r = repo(token = "   ")
        assertFalse(r.hasToken())
    }

    @Test
    fun `clearToken removes the token`() {
        val r = repo(token = "ghp_token")
        assertTrue(r.hasToken())
        r.clearToken()
        assertFalse(r.hasToken())
    }

    @Test
    fun `updateConfig normalizes fields`() {
        val storage = FakeSettingsStorage()
        val r = SettingsRepository(FakeTokenStore(), storage)
        r.updateConfig {
            it.copy(repoOwner = "  duminandrew ", repoName = " vault ", branch = "  ", vaultUri = "  ")
        }
        val c = r.config()
        assertEquals("duminandrew", c.repoOwner)
        assertEquals("vault", c.repoName)
        assertEquals("main", c.branch) // blank branch falls back to main
        assertNull(c.vaultUri) // blank vault uri becomes null
    }

    @Test
    fun `effective branch falls back to main when blank`() {
        assertEquals("main", SyncConfig(branch = "").effectiveBranch)
        assertEquals("develop", SyncConfig(branch = "develop").effectiveBranch)
    }

    @Test
    fun `isConfigured is independent of the token`() {
        val configured = SyncConfig(repoOwner = "o", repoName = "r", vaultUri = "u")
        assertTrue(configured.isConfigured)
        assertFalse(SyncConfig(repoOwner = "o", repoName = "", vaultUri = "u").isConfigured)
    }
}

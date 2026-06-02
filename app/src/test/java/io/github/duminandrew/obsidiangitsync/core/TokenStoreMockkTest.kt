package io.github.duminandrew.obsidiangitsync.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Demonstrates the [TokenStore] interface seam being mocked so that PAT logic is
 * exercised without the real `EncryptedSharedPreferences`/Android Keystore.
 */
class TokenStoreMockkTest {

    @Test
    fun `hasToken default delegates to getToken`() {
        val store = mockk<TokenStore> {
            every { getToken() } returns "ghp_value"
            every { hasToken() } answers { callOriginal() }
        }
        assertTrue(store.hasToken())
        verify { store.getToken() }
    }

    @Test
    fun `hasToken is false when getToken is null`() {
        val store = mockk<TokenStore> {
            every { getToken() } returns null
            every { hasToken() } answers { callOriginal() }
        }
        assertFalse(store.hasToken())
    }

    @Test
    fun `repository reports ready using a mocked token store`() {
        val tokenStore = mockk<TokenStore> {
            every { getToken() } returns "ghp_value"
            every { hasToken() } answers { callOriginal() }
        }
        val repo = SettingsRepository(
            tokenStore,
            FakeSettingsStorage(SyncConfig(repoOwner = "o", repoName = "r", vaultUri = "u")),
        )
        assertTrue(repo.isReadyToSync())
    }
}

package io.github.duminandrew.obsidiangitsync.core

/**
 * Storage abstraction for the GitHub Personal Access Token (PAT).
 *
 * The production implementation is backed by `EncryptedSharedPreferences`
 * (AES-256-GCM, Android Keystore). Depending on this interface — rather than the
 * concrete `SecureStore` — lets the PAT-handling logic be unit-tested with an
 * in-memory fake so tests never touch the Android Keystore or real encrypted
 * preferences.
 */
interface TokenStore {

    /** Returns the stored PAT, or `null` if none is configured. */
    fun getToken(): String?

    /** Persists [token]; a blank value clears any stored token. */
    fun saveToken(token: String)

    /** Removes any stored token. */
    fun clearToken()

    /** True when a non-blank token is configured. */
    fun hasToken(): Boolean = !getToken().isNullOrBlank()
}

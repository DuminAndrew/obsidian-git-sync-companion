package io.github.duminandrew.obsidiangitsync.core

import kotlinx.coroutines.delay

/**
 * Small framework-free retry-with-exponential-backoff helper for transient
 * network failures (e.g. GitHub 5xx / timeouts) during sync. Kept pure and
 * `suspend`-based so it is unit-testable with `kotlinx-coroutines-test` (virtual
 * time, no real waiting) and free of Android dependencies.
 */
object Retry {

    /**
     * Runs [block], retrying up to [maxAttempts] times while [retryOn] returns
     * true for the thrown exception. Backoff doubles from [initialDelayMillis]
     * up to [maxDelayMillis]. The last failure is rethrown.
     *
     * @return the value returned by the first successful [block] invocation.
     */
    suspend fun <T> withBackoff(
        maxAttempts: Int = 3,
        initialDelayMillis: Long = 500,
        maxDelayMillis: Long = 8_000,
        factor: Double = 2.0,
        retryOn: (Throwable) -> Boolean = { true },
        block: suspend (attempt: Int) -> T,
    ): T {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        var delayMillis = initialDelayMillis
        var lastError: Throwable? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block(attempt)
            } catch (t: Throwable) {
                lastError = t
                if (attempt == maxAttempts || !retryOn(t)) throw t
                delay(delayMillis)
                delayMillis = (delayMillis * factor).toLong().coerceAtMost(maxDelayMillis)
            }
        }
        // Unreachable: the loop either returns or throws.
        throw lastError ?: IllegalStateException("retry failed without an error")
    }
}

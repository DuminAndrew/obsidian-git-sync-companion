package io.github.duminandrew.obsidiangitsync.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RetryTest {

    @Test
    fun `returns immediately on first success`() = runTest {
        var calls = 0
        val result = Retry.withBackoff(maxAttempts = 3) {
            calls++
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(1, calls)
    }

    @Test
    fun `retries until success and uses virtual time`() = runTest {
        var calls = 0
        val result = Retry.withBackoff(maxAttempts = 5, initialDelayMillis = 1000) { attempt ->
            calls++
            if (attempt < 3) throw IOException("transient")
            "recovered"
        }
        assertEquals("recovered", result)
        assertEquals(3, calls)
    }

    @Test
    fun `rethrows after exhausting attempts`() = runTest {
        var calls = 0
        val error = runCatching {
            Retry.withBackoff(maxAttempts = 3, initialDelayMillis = 10) {
                calls++
                throw IOException("always fails")
            }
        }.exceptionOrNull()
        assert(error is IOException)
        assertEquals(3, calls)
    }

    @Test
    fun `does not retry when predicate rejects the error`() = runTest {
        var calls = 0
        val error = runCatching {
            Retry.withBackoff(
                maxAttempts = 5,
                initialDelayMillis = 10,
                retryOn = { it is IOException },
            ) {
                calls++
                error("non-retryable")
            }
        }.exceptionOrNull()
        assertEquals(1, calls)
        assert(error is IllegalStateException)
    }

    @Test
    fun `rejects invalid maxAttempts`() = runTest {
        val error = runCatching {
            Retry.withBackoff(maxAttempts = 0) { "x" }
        }.exceptionOrNull()
        assert(error is IllegalArgumentException)
    }
}

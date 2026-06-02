package io.github.duminandrew.obsidiangitsync.sync

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that [GitBlobSha] matches `git hash-object` exactly, so local files
 * can be compared against the SHAs in the GitHub tree without uploading them.
 * Expected values are produced by `git hash-object --stdin`.
 */
class GitBlobShaTest {

    @Test
    fun `empty content matches git empty blob`() {
        assertEquals(
            "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391",
            GitBlobSha.of(ByteArray(0)),
        )
    }

    @Test
    fun `hello without newline matches git`() {
        assertEquals(
            "b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0",
            GitBlobSha.of("hello".toByteArray(Charsets.UTF_8)),
        )
    }

    @Test
    fun `hello with trailing newline matches git`() {
        assertEquals(
            "ce013625030ba8dba906f756967f9e9ca394464a",
            GitBlobSha.of("hello\n".toByteArray(Charsets.UTF_8)),
        )
    }

    @Test
    fun `output is 40 lower-case hex characters`() {
        val sha = GitBlobSha.of("anything".toByteArray())
        assertEquals(40, sha.length)
        assertEquals(sha.lowercase(), sha)
        assert(sha.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `different content yields different sha`() {
        val a = GitBlobSha.of("a".toByteArray())
        val b = GitBlobSha.of("b".toByteArray())
        assert(a != b)
    }
}

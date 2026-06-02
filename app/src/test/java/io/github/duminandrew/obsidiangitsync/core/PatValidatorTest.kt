package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatValidatorTest {

    @Test
    fun `null and blank are not present`() {
        assertFalse(PatValidator.isPresent(null))
        assertFalse(PatValidator.isPresent(""))
        assertFalse(PatValidator.isPresent("   "))
    }

    @Test
    fun `non-blank token is present`() {
        assertTrue(PatValidator.isPresent("x"))
    }

    @Test
    fun `blank classifies as BLANK`() {
        assertEquals(PatValidator.Result.BLANK, PatValidator.validate(null))
        assertEquals(PatValidator.Result.BLANK, PatValidator.validate("  "))
    }

    @Test
    fun `fine grained token is valid`() {
        val token = "github_pat_" + "A".repeat(22) + "_" + "b".repeat(59)
        assertEquals(PatValidator.Result.VALID, PatValidator.validate(token))
        assertTrue(PatValidator.isValidFormat(token))
    }

    @Test
    fun `classic ghp token is valid`() {
        val token = "ghp_" + "A1b2C3".repeat(7) // 42 chars after prefix
        assertEquals(PatValidator.Result.VALID, PatValidator.validate(token))
    }

    @Test
    fun `legacy 40-char hex token is valid`() {
        val token = "0123456789abcdef0123456789abcdef01234567"
        assertEquals(40, token.length)
        assertEquals(PatValidator.Result.VALID, PatValidator.validate(token))
    }

    @Test
    fun `leading and trailing whitespace is tolerated`() {
        val token = "  ghp_" + "A1b2C3".repeat(7) + "  "
        assertEquals(PatValidator.Result.VALID, PatValidator.validate(token))
    }

    @Test
    fun `garbage and too-short tokens are invalid format`() {
        assertEquals(PatValidator.Result.INVALID_FORMAT, PatValidator.validate("not-a-token"))
        assertEquals(PatValidator.Result.INVALID_FORMAT, PatValidator.validate("ghp_short"))
        assertEquals(PatValidator.Result.INVALID_FORMAT, PatValidator.validate("github_pat_"))
    }

    @Test
    fun `mask never reveals the full token`() {
        val token = "github_pat_" + "A".repeat(22) + "secret9999"
        val masked = PatValidator.mask(token)
        assertTrue(masked.startsWith("github_pat_"))
        assertTrue(masked.endsWith("9999"))
        assertFalse(masked.contains("secret"))
        assertEquals("(none)", PatValidator.mask(null))
        assertEquals("(none)", PatValidator.mask("   "))
    }

    @Test
    fun `mask of classic token keeps ghp prefix`() {
        assertTrue(PatValidator.mask("ghp_abcdEFGH1234").startsWith("ghp_"))
        assertTrue(PatValidator.mask("ghp_abcdEFGH1234").endsWith("1234"))
    }
}

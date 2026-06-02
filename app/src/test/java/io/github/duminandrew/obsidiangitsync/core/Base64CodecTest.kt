package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Base64CodecTest {

    @Test
    fun `encode produces canonical padded base64`() {
        // "hello" -> aGVsbG8=
        assertEquals("aGVsbG8=", Base64Codec.encodeUtf8("hello"))
    }

    @Test
    fun `encode of empty bytes is empty string`() {
        assertEquals("", Base64Codec.encode(ByteArray(0)))
    }

    @Test
    fun `round trip preserves arbitrary bytes`() {
        val bytes = ByteArray(256) { it.toByte() }
        val encoded = Base64Codec.encode(bytes)
        assertArrayEquals(bytes, Base64Codec.decode(encoded))
    }

    @Test
    fun `round trip preserves unicode text`() {
        val text = "héllo — Obsidian 🗒️\n# heading"
        assertEquals(text, Base64Codec.decodeUtf8(Base64Codec.encodeUtf8(text)))
    }

    @Test
    fun `decode tolerates github line-wrapped content`() {
        // GitHub wraps base64 at 60 chars with newlines; the MIME decoder ignores them.
        val wrapped = "aGVs\nbG8=\n"
        assertEquals("hello", Base64Codec.decodeUtf8(wrapped))
    }

    @Test
    fun `encode never contains newlines`() {
        val big = ByteArray(1000) { (it % 251).toByte() }
        val encoded = Base64Codec.encode(big)
        assert(!encoded.contains('\n')) { "single-line base64 expected" }
        assert(!encoded.contains('\r'))
    }

    @Test
    fun `decode of blank or null returns null`() {
        assertNull(Base64Codec.decode(null))
        assertNull(Base64Codec.decode(""))
        assertNull(Base64Codec.decode("   \n  "))
    }

    @Test
    fun `decode of invalid base64 returns null instead of throwing`() {
        assertNull(Base64Codec.decode("!!!! not base64 @@@"))
    }
}

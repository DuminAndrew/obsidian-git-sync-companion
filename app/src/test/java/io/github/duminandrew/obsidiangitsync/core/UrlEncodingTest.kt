package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlEncodingTest {

    @Test
    fun `unreserved characters pass through unchanged`() {
        val unreserved = "ABCabc123-._~"
        assertEquals(unreserved, UrlEncoding.encodeComponent(unreserved))
    }

    @Test
    fun `spaces are percent encoded as upper-case hex`() {
        assertEquals("a%20b", UrlEncoding.encodeComponent("a b"))
    }

    @Test
    fun `slash is encoded by encodeComponent`() {
        assertEquals("a%2Fb", UrlEncoding.encodeComponent("a/b"))
    }

    @Test
    fun `reserved url characters are encoded`() {
        assertEquals("%3F%23%26%3D%2B", UrlEncoding.encodeComponent("?#&=+"))
    }

    @Test
    fun `multi-byte utf8 is encoded per byte`() {
        // é is U+00E9 -> C3 A9 in UTF-8.
        assertEquals("r%C3%A9", UrlEncoding.encodeComponent("ré"))
    }

    @Test
    fun `empty string encodes to empty string`() {
        assertEquals("", UrlEncoding.encodeComponent(""))
    }
}

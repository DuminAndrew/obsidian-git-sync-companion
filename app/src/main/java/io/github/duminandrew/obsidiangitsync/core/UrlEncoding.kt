package io.github.duminandrew.obsidiangitsync.core

/**
 * Minimal RFC 3986 "unreserved + path" percent-encoder used to build GitHub REST
 * URLs. Kept framework-free (no `android.net.Uri`) so the path-encoding logic can
 * be unit-tested on the JVM and reused identically on-device.
 *
 * Encodes a single path segment: unreserved characters (A-Z a-z 0-9 - . _ ~)
 * pass through unchanged; everything else is percent-encoded from its UTF-8 bytes.
 * Notably "/" IS encoded by [encodeComponent], so callers join already-split
 * segments with literal "/" to preserve directory separators.
 */
object UrlEncoding {

    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private val HEX = "0123456789ABCDEF".toCharArray()

    fun encodeComponent(value: String): String {
        val sb = StringBuilder(value.length)
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xFF
            if (c < 128 && UNRESERVED.indexOf(c.toChar()) >= 0) {
                sb.append(c.toChar())
            } else {
                sb.append('%')
                sb.append(HEX[c ushr 4])
                sb.append(HEX[c and 0x0F])
            }
        }
        return sb.toString()
    }
}

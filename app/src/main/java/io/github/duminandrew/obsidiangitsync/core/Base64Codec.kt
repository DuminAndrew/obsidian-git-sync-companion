package io.github.duminandrew.obsidiangitsync.core

/**
 * Pure-JVM base64 codec for GitHub Contents API payloads.
 *
 * On-device the app uses `android.util.Base64`, which is unavailable in JVM unit
 * tests; this class wraps `java.util.Base64` (present on both the JVM and Android
 * API 26+, the app's `minSdk`) so the encode/decode behaviour can be verified
 * offline and reused on-device.
 *
 * - [encode] produces a single line with no line breaks (GitHub accepts this for
 *   PUT /contents).
 * - [decode] tolerates the line-wrapped base64 GitHub returns from GET /contents
 *   by stripping CR/LF and surrounding whitespace before decoding.
 */
object Base64Codec {

    // Standard padded encoder (GitHub expects canonical padding, single line).
    private val encoder = java.util.Base64.getEncoder()

    // MIME decoder tolerates the line-wrapped base64 GitHub returns.
    private val decoder = java.util.Base64.getMimeDecoder()

    /** Encodes [bytes] to a single-line base64 string (no embedded newlines). */
    fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)

    /** Convenience: UTF-8 encodes [text] then base64-encodes it. */
    fun encodeUtf8(text: String): String = encode(text.toByteArray(Charsets.UTF_8))

    /**
     * Decodes base64 [content], ignoring CR/LF/whitespace that GitHub inserts in
     * GET /contents responses. Returns `null` for blank input or invalid base64
     * rather than throwing, mirroring the engine's defensive handling.
     */
    fun decode(content: String?): ByteArray? {
        if (content.isNullOrBlank()) return null
        return try {
            decoder.decode(content)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Decodes base64 [content] and interprets the bytes as UTF-8 text, or `null`. */
    fun decodeUtf8(content: String?): String? =
        decode(content)?.toString(Charsets.UTF_8)
}

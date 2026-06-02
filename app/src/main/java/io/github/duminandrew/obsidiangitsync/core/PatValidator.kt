package io.github.duminandrew.obsidiangitsync.core

/**
 * Lightweight, offline validation of a GitHub Personal Access Token (PAT).
 *
 * This does NOT contact GitHub; it only checks presence and surface-level format
 * so the UI can give immediate feedback and avoid obviously-malformed tokens.
 * The authoritative check is the first authenticated API call.
 *
 * Recognised shapes:
 *  - Fine-grained tokens: `github_pat_` followed by base62/underscore characters.
 *  - Classic tokens: `ghp_` followed by 36+ base62 characters.
 *  - Legacy 40-char hex tokens (older classic PATs).
 */
object PatValidator {

    private val FINE_GRAINED = Regex("^github_pat_[A-Za-z0-9_]{20,}$")
    private val CLASSIC = Regex("^ghp_[A-Za-z0-9]{36,}$")
    private val LEGACY_HEX = Regex("^[0-9a-fA-F]{40}$")

    enum class Result { BLANK, INVALID_FORMAT, VALID }

    /** True if [token] is a non-null, non-blank string. */
    fun isPresent(token: String?): Boolean = !token.isNullOrBlank()

    /** Full classification of [token] presence and format. */
    fun validate(token: String?): Result {
        if (token.isNullOrBlank()) return Result.BLANK
        val t = token.trim()
        val looksValid = FINE_GRAINED.matches(t) || CLASSIC.matches(t) || LEGACY_HEX.matches(t)
        return if (looksValid) Result.VALID else Result.INVALID_FORMAT
    }

    /** Convenience: true only when [validate] returns [Result.VALID]. */
    fun isValidFormat(token: String?): Boolean = validate(token) == Result.VALID

    /**
     * Masks a token for safe display/logging, e.g. `github_pat_…ab12`. Never
     * returns more than the last 4 characters; blank input yields "(none)".
     * The full token is never logged anywhere in the app.
     */
    fun mask(token: String?): String {
        if (token.isNullOrBlank()) return "(none)"
        val t = token.trim()
        val tail = t.takeLast(4)
        val prefix = when {
            t.startsWith("github_pat_") -> "github_pat_"
            t.startsWith("ghp_") -> "ghp_"
            else -> ""
        }
        return "$prefix…$tail"
    }
}

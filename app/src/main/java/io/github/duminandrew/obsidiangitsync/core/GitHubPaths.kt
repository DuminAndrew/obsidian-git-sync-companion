package io.github.duminandrew.obsidiangitsync.core

/**
 * Pure, framework-free builders for the GitHub REST API paths and URLs used by
 * the sync engine. Centralising them here keeps the Retrofit interface thin and,
 * more importantly, makes the (easy-to-get-wrong) path encoding unit-testable on
 * the JVM without an Android device or network.
 *
 * All builders are relative to [BASE_URL] and assume repo-relative POSIX paths
 * ("notes/todo.md") to line up with GitHub tree paths.
 */
object GitHubPaths {

    const val BASE_URL: String = "https://api.github.com/"

    /** Path of the Git ref for a branch head: `repos/{owner}/{repo}/git/ref/heads/{branch}`. */
    fun branchRef(owner: String, repo: String, branch: String): String =
        "repos/${owner.requireSegment("owner")}/${repo.requireSegment("repo")}" +
            "/git/ref/heads/${branch.requireSegment("branch")}"

    /** Path of a single commit object: `repos/{owner}/{repo}/git/commits/{sha}`. */
    fun commit(owner: String, repo: String, commitSha: String): String =
        "repos/${owner.requireSegment("owner")}/${repo.requireSegment("repo")}" +
            "/git/commits/${commitSha.requireSegment("commitSha")}"

    /** Path of a tree object: `repos/{owner}/{repo}/git/trees/{sha}`. */
    fun tree(owner: String, repo: String, treeSha: String, recursive: Boolean = true): String {
        val base = "repos/${owner.requireSegment("owner")}/${repo.requireSegment("repo")}" +
            "/git/trees/${treeSha.requireSegment("treeSha")}"
        return if (recursive) "$base?recursive=1" else base
    }

    /**
     * Path of the Contents endpoint for a file, with each path segment percent
     * encoded but the "/" separators preserved: `repos/{owner}/{repo}/contents/{path}`.
     * An optional `ref` (branch/sha) query is appended when provided.
     */
    fun contents(owner: String, repo: String, filePath: String, ref: String? = null): String {
        val base = "repos/${owner.requireSegment("owner")}/${repo.requireSegment("repo")}" +
            "/contents/${encodePath(filePath)}"
        return if (ref.isNullOrEmpty()) base else "$base?ref=${UrlEncoding.encodeComponent(ref)}"
    }

    /** Absolute URL for any of the relative paths above. */
    fun absolute(path: String): String = BASE_URL + path.removePrefix("/")

    /**
     * Percent-encodes each segment of a repo-relative path while keeping the "/"
     * separators, matching the encoding used when calling the Contents API.
     */
    fun encodePath(filePath: String): String {
        require(filePath.isNotBlank()) { "file path must not be blank" }
        require(!filePath.startsWith("/")) { "file path must be repo-relative, not absolute" }
        return filePath.split('/').joinToString("/") { UrlEncoding.encodeComponent(it) }
    }

    private fun String.requireSegment(name: String): String {
        require(isNotBlank()) { "$name must not be blank" }
        require(!contains('/')) { "$name must not contain '/'" }
        return this
    }
}

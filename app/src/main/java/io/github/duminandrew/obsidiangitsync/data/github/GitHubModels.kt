package io.github.duminandrew.obsidiangitsync.data.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal data-transfer objects for the GitHub REST API endpoints we use:
 *  - Git Data API: refs, commits, trees (for a cheap recursive snapshot).
 *  - Contents API: read/write individual files (base64 encoded).
 */

@Serializable
data class RefObject(
    val sha: String,
    val type: String,
    val url: String,
)

@Serializable
data class GitRef(
    val ref: String,
    @SerialName("node_id") val nodeId: String? = null,
    val url: String? = null,
    val `object`: RefObject,
)

@Serializable
data class CommitTreeRef(
    val sha: String,
    val url: String? = null,
)

@Serializable
data class GitCommit(
    val sha: String,
    val tree: CommitTreeRef,
)

@Serializable
data class TreeEntry(
    val path: String,
    val mode: String,
    val type: String,          // "blob" | "tree"
    val sha: String? = null,
    val size: Long? = null,
    val url: String? = null,
)

@Serializable
data class GitTree(
    val sha: String,
    val url: String? = null,
    val tree: List<TreeEntry> = emptyList(),
    val truncated: Boolean = false,
)

/** Response from GET /contents/{path}. `content` is base64 (may contain newlines). */
@Serializable
data class ContentFile(
    val type: String,
    val name: String,
    val path: String,
    val sha: String,
    val size: Long = 0,
    val content: String? = null,
    val encoding: String? = null,
)

/** Body for PUT /contents/{path}. `content` is base64-encoded file bytes. */
@Serializable
data class PutContentRequest(
    val message: String,
    val content: String,
    val branch: String? = null,
    /** Required when updating an existing file: its current blob SHA. */
    val sha: String? = null,
)

@Serializable
data class ContentCommitInfo(
    val sha: String? = null,
)

@Serializable
data class PutContentResponse(
    val content: ContentFile? = null,
    val commit: ContentCommitInfo? = null,
)

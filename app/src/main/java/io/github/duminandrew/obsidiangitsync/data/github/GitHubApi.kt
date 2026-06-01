package io.github.duminandrew.obsidiangitsync.data.github

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the subset of the GitHub REST API used for sync.
 *
 * Authentication is added by an OkHttp interceptor (Authorization: Bearer PAT),
 * so no token parameter appears here and it can never be logged via URLs.
 *
 * Base URL: https://api.github.com/
 */
interface GitHubApi {

    /** Resolve a branch ref to its commit SHA. */
    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
    ): Response<GitRef>

    /** Fetch a commit to obtain its root tree SHA. */
    @GET("repos/{owner}/{repo}/git/commits/{commitSha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("commitSha") commitSha: String,
    ): Response<GitCommit>

    /** Recursively list the whole tree in one call (cheap snapshot of all paths + SHAs). */
    @GET("repos/{owner}/{repo}/git/trees/{treeSha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("treeSha") treeSha: String,
        @Query("recursive") recursive: Int = 1,
    ): Response<GitTree>

    /**
     * Read a single file's content (base64). [path] must be the URL-encoded
     * repo-relative path; Retrofit's {path} with encoded=false re-encodes "/"
     * correctly for nested paths.
     */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String,
    ): Response<ContentFile>

    /** Create or update a file (base64 content). Include `sha` in the body to update. */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: PutContentRequest,
    ): Response<PutContentResponse>

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}

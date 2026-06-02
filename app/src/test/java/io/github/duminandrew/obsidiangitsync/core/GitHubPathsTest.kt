package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubPathsTest {

    @Test
    fun `base url is the public github api`() {
        assertEquals("https://api.github.com/", GitHubPaths.BASE_URL)
    }

    @Test
    fun `branch ref path is built correctly`() {
        assertEquals(
            "repos/duminandrew/obsidian-git-sync-companion/git/ref/heads/main",
            GitHubPaths.branchRef("duminandrew", "obsidian-git-sync-companion", "main"),
        )
    }

    @Test
    fun `commit and tree paths are built correctly`() {
        assertEquals(
            "repos/o/r/git/commits/abc123",
            GitHubPaths.commit("o", "r", "abc123"),
        )
        assertEquals(
            "repos/o/r/git/trees/deadbeef?recursive=1",
            GitHubPaths.tree("o", "r", "deadbeef"),
        )
        assertEquals(
            "repos/o/r/git/trees/deadbeef",
            GitHubPaths.tree("o", "r", "deadbeef", recursive = false),
        )
    }

    @Test
    fun `contents path keeps slashes but encodes segments`() {
        assertEquals(
            "repos/o/r/contents/notes/todo.md",
            GitHubPaths.contents("o", "r", "notes/todo.md"),
        )
    }

    @Test
    fun `contents path encodes spaces and unicode but not slashes`() {
        val path = GitHubPaths.contents("o", "r", "daily notes/2026 résumé.md")
        assertEquals(
            "repos/o/r/contents/daily%20notes/2026%20r%C3%A9sum%C3%A9.md",
            path,
        )
        // Directory separators must survive encoding.
        assertTrue(path.contains("daily%20notes/2026"))
    }

    @Test
    fun `contents path appends ref query when provided`() {
        assertEquals(
            "repos/o/r/contents/a.md?ref=main",
            GitHubPaths.contents("o", "r", "a.md", ref = "main"),
        )
        assertEquals(
            "repos/o/r/contents/a.md",
            GitHubPaths.contents("o", "r", "a.md", ref = null),
        )
    }

    @Test
    fun `absolute prefixes base url and de-duplicates leading slash`() {
        assertEquals(
            "https://api.github.com/repos/o/r/contents/a.md",
            GitHubPaths.absolute(GitHubPaths.contents("o", "r", "a.md")),
        )
        assertEquals(
            "https://api.github.com/x",
            GitHubPaths.absolute("/x"),
        )
    }

    @Test
    fun `blank owner repo or branch is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GitHubPaths.branchRef("", "r", "main")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GitHubPaths.branchRef("o", "  ", "main")
        }
    }

    @Test
    fun `absolute path must be repo-relative`() {
        assertThrows(IllegalArgumentException::class.java) {
            GitHubPaths.encodePath("/etc/passwd")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GitHubPaths.encodePath("")
        }
    }

    @Test
    fun `owner segment may not contain a slash`() {
        assertThrows(IllegalArgumentException::class.java) {
            GitHubPaths.commit("o/evil", "r", "sha")
        }
    }
}

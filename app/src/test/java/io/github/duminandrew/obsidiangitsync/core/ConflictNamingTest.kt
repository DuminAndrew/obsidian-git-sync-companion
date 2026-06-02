package io.github.duminandrew.obsidiangitsync.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConflictNamingTest {

    private val ts = "20260602-101500"

    @Test
    fun `inserts marker before the extension`() {
        assertEquals(
            "notes/todo (conflict $ts).md",
            ConflictNaming.conflictName("notes/todo.md", ts),
        )
    }

    @Test
    fun `handles file at vault root`() {
        assertEquals(
            "todo (conflict $ts).md",
            ConflictNaming.conflictName("todo.md", ts),
        )
    }

    @Test
    fun `file without extension appends marker at end`() {
        assertEquals(
            "notes/LICENSE (conflict $ts)",
            ConflictNaming.conflictName("notes/LICENSE", ts),
        )
    }

    @Test
    fun `dotfile is treated as having no extension`() {
        assertEquals(
            ".gitignore (conflict $ts)",
            ConflictNaming.conflictName(".gitignore", ts),
        )
    }

    @Test
    fun `dot in a directory name is not mistaken for an extension`() {
        // The only dot is in the directory segment, file segment has none.
        assertEquals(
            "my.folder/notes (conflict $ts)",
            ConflictNaming.conflictName("my.folder/notes", ts),
        )
    }

    @Test
    fun `multiple dots use the last as the extension`() {
        assertEquals(
            "archive.tar (conflict $ts).gz",
            ConflictNaming.conflictName("archive.tar.gz", ts),
        )
    }

    @Test
    fun `blank path is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConflictNaming.conflictName("", ts)
        }
    }
}

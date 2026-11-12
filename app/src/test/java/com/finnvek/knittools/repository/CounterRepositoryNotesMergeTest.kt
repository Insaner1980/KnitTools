package com.finnvek.knittools.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class CounterRepositoryNotesMergeTest {
    @Test
    fun `merge replaces unchanged base with requested notes`() {
        val result =
            mergeProjectNotes(
                baseNotes = "Base",
                requestedNotes = "Local edit",
                currentNotes = "Base",
            )

        assertEquals("Local edit", result)
    }

    @Test
    fun `merge keeps external append when local text was edited`() {
        val result =
            mergeProjectNotes(
                baseNotes = "Base",
                requestedNotes = "Local edit",
                currentNotes = "Base\nVoice note",
            )

        assertEquals("Local edit\nVoice note", result)
    }

    @Test
    fun `merge appends local addition onto externally appended notes`() {
        val result =
            mergeProjectNotes(
                baseNotes = "Base",
                requestedNotes = "Base\nJournal note",
                currentNotes = "Base\nVoice note",
            )

        assertEquals("Base\nVoice note\nJournal note", result)
    }

    @Test
    fun `merge keeps both conflicting replacements`() {
        val result =
            mergeProjectNotes(
                baseNotes = "Base",
                requestedNotes = "Local edit",
                currentNotes = "External edit",
            )

        assertEquals("Local edit\n\n---\n\nExternal edit", result)
    }

    @Test
    fun `merge separates concurrent additions from empty notes`() {
        val result =
            mergeProjectNotes(
                baseNotes = "",
                requestedNotes = "Local note",
                currentNotes = "External note",
            )

        assertEquals("Local note\n\n---\n\nExternal note", result)
    }

    @Test
    fun `merge preserves boundary whitespace from concurrent note additions`() {
        val result =
            mergeProjectNotes(
                baseNotes = "",
                requestedNotes = "Beta  ",
                currentNotes = "  Alpha",
            )

        assertEquals("Beta  \n\n---\n\n  Alpha", result)
    }

    @Test
    fun `retrying a merged replacement does not duplicate its note block`() {
        val result =
            mergeProjectNotes(
                baseNotes = "Base",
                requestedNotes = "Local edit",
                currentNotes = "Local edit\n\n---\n\nExternal edit",
            )

        assertEquals("Local edit\n\n---\n\nExternal edit", result)
    }

    @Test
    fun `merge preserves local append when external replacement has a matching suffix`() {
        assertEquals(
            "Hook: 33",
            mergeProjectNotes("Needle: 3", "Needle: 33", "Hook: 3"),
        )
    }

    @Test
    fun `matching concurrent suffix is not proof that the local append was saved`() {
        assertEquals(
            "Base\nExternal addition\nLocal addition\nLocal addition",
            mergeProjectNotes("Base", "Base\nLocal addition", "Base\nExternal addition\nLocal addition"),
        )
    }

    @Test
    fun `retry using the confirmed merged result preserves a later external append`() {
        val committedNotes =
            mergeProjectNotes(
                baseNotes = "Base",
                requestedNotes = "Base\nLocal addition",
                currentNotes = "Base\nExternal addition",
            )
        assertEquals("Base\nExternal addition\nLocal addition", committedNotes)

        val retriedNotes =
            mergeProjectNotes(
                baseNotes = committedNotes,
                requestedNotes = committedNotes,
                currentNotes = "$committedNotes\nLater addition",
            )

        assertEquals("Base\nExternal addition\nLocal addition\nLater addition", retriedNotes)
    }
}

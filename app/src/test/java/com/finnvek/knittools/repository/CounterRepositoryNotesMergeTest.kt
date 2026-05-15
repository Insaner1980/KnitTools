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
}

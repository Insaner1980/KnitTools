package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterProjectYarnGateTest {
    @Test
    fun `locked yarn feature does not run save action`() {
        var savedNoteId: Long? = null

        runProjectYarnNoteSaveIfAllowed(
            noteId = 7L,
            canUseYarnCards = false,
        ) { noteId ->
            savedNoteId = noteId
        }

        assertNull(savedNoteId)
    }

    @Test
    fun `unlocked yarn feature runs save action`() {
        var savedNoteId: Long? = null

        runProjectYarnNoteSaveIfAllowed(
            noteId = 7L,
            canUseYarnCards = true,
        ) { noteId ->
            savedNoteId = noteId
        }

        assertEquals(7L, savedNoteId)
    }
}

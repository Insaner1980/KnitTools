package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSetupProgressTest {
    @Test
    fun `starting point accepts a count or a target`() {
        assertFalse(progress().isDone(ProjectSetupItem.STARTING_POINT))
        assertTrue(progress(count = 37).isDone(ProjectSetupItem.STARTING_POINT))
        assertTrue(progress(target = 120).isDone(ProjectSetupItem.STARTING_POINT))
    }

    @Test
    fun `each existing content fact completes only its own item`() {
        assertEquals(setOf(ProjectSetupItem.PATTERN), progress(pattern = true).completed)
        assertEquals(setOf(ProjectSetupItem.YARN), progress(yarn = true).completed)
        assertEquals(setOf(ProjectSetupItem.WORK_TIME), progress(session = true).completed)
    }

    @Test
    fun `unfinished items precede finished items in their original order`() {
        assertEquals(
            listOf(
                ProjectSetupItem.STARTING_POINT,
                ProjectSetupItem.YARN,
                ProjectSetupItem.PATTERN,
                ProjectSetupItem.WORK_TIME,
            ),
            progress(pattern = true, session = true).items,
        )
        assertEquals(ProjectSetupItem.entries, progress(count = 1, pattern = true, yarn = true, session = true).items)
        assertEquals(4, progress(count = 1, pattern = true, yarn = true, session = true).doneCount)
    }

    private fun progress(
        count: Int = 0,
        target: Int? = null,
        pattern: Boolean = false,
        yarn: Boolean = false,
        session: Boolean = false,
    ) = projectSetupProgress(count, target, pattern, yarn, session)
}

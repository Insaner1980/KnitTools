package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSetupProgressTest {
    @Test
    fun `only an actual target completes the optional target suggestion`() {
        assertFalse(progress().isDone(ProjectSetupItem.TARGET))
        assertTrue(progress(target = 120).isDone(ProjectSetupItem.TARGET))
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
                ProjectSetupItem.TARGET,
                ProjectSetupItem.YARN,
                ProjectSetupItem.PATTERN,
                ProjectSetupItem.WORK_TIME,
            ),
            progress(pattern = true, session = true).items,
        )
        assertEquals(
            ProjectSetupItem.entries,
            progress(target = 120, pattern = true, yarn = true, session = true).items,
        )
        assertEquals(4, progress(target = 120, pattern = true, yarn = true, session = true).doneCount)
    }

    private fun progress(
        target: Int? = null,
        pattern: Boolean = false,
        yarn: Boolean = false,
        session: Boolean = false,
    ) = projectSetupProgress(target, pattern, yarn, session)
}

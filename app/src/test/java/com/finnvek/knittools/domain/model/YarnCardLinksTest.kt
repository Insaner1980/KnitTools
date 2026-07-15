package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class YarnCardLinksTest {
    @Test
    fun `parse ignores blanks and duplicate ids`() {
        assertEquals(listOf(1L, 2L, 3L), parseYarnCardIds(" 1, ,2,1,invalid,3,2 "))
    }

    @Test
    fun `format writes each id once`() {
        assertEquals("1,2,3", formatYarnCardIds(listOf(1L, 2L, 1L, 3L, 2L)))
    }
}

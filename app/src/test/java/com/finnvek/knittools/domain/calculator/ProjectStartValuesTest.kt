package com.finnvek.knittools.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectStartValuesTest {
    @Test
    fun `starting count accepts empty zero and inclusive maximum`() {
        assertEquals(0, parseProjectStartCount(""))
        assertEquals(0, parseProjectStartCount("0"))
        assertEquals(37, parseProjectStartCount("37"))
        assertEquals(MAX_PROJECT_START_COUNT, parseProjectStartCount("99999"))
    }

    @Test
    fun `starting count rejects signs whitespace fractions nonnumbers and overflow`() {
        listOf("100000", "-1", "+1", " 1", "1 ", " ", "1.5", "1,5", "1e2", "one", "99999999999999999999")
            .forEach { assertNull(it, parseProjectStartCount(it)) }
    }

    @Test
    fun `target distinguishes no target valid positive count and invalid values`() {
        assertEquals(TargetInput.NoTarget, parseTargetRows(""))
        assertEquals(TargetInput.Valid(1), parseTargetRows("1"))
        assertEquals(TargetInput.Valid(120), parseTargetRows("120"))
        assertEquals(TargetInput.Valid(MAX_TARGET_ROWS), parseTargetRows("99999"))
        listOf("0", "100000", "-1", "+1", " 1", "1 ", " ", "1.5", "1,5", "1e2", "one", "99999999999999999999")
            .forEach { assertEquals(it, TargetInput.Invalid, parseTargetRows(it)) }
    }
}

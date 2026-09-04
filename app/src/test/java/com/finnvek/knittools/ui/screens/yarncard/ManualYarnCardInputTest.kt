package com.finnvek.knittools.ui.screens.yarncard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualYarnCardInputTest {
    @Test
    fun `quantity parser accepts positive integers only`() {
        assertEquals(1, parseManualYarnQuantity("1"))
        assertEquals(Int.MAX_VALUE, parseManualYarnQuantity(Int.MAX_VALUE.toString()))
        assertNull(parseManualYarnQuantity(""))
        assertNull(parseManualYarnQuantity("0"))
        assertNull(parseManualYarnQuantity("999999999999999999999"))
    }
}

package com.finnvek.knittools.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class YarnColorPaletteTest {
    @Test
    fun `same project id maps to the same colour across two orderings of the same list`() {
        val projectIds = listOf(4L, 17L, 2L, 91L, 33L)
        val reordered = projectIds.sortedDescending()

        val byId = projectIds.associateWith(::yarnColorForId)
        val byReorderedId = reordered.associateWith(::yarnColorForId)

        assertEquals(byId, byReorderedId)
    }

    @Test
    fun `colour is derived from the id, never from list position`() {
        assertEquals(yarnColorForId(9L), yarnColorForId(9L + YarnColors.size))
        assertNotEquals(yarnColorForId(0L), yarnColorForId(1L))
    }

    @Test
    fun `negative ids stay inside the palette`() {
        val color = yarnColorForId(-3L)

        assertEquals(YarnColors[YarnColors.size - 3], color)
    }
}

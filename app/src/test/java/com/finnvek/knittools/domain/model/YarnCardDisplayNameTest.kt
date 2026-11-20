package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class YarnCardDisplayNameTest {
    @Test
    fun omitsBrandWhenNameAlreadyStartsWithIt() {
        assertEquals("Alpine Merino DK", displayName("Alpine", "Alpine Merino DK"))
        assertEquals("ALPINE Merino DK", displayName(" Alpine ", " ALPINE Merino DK "))
        assertEquals("alpine", displayName("Alpine", "alpine"))
    }

    @Test
    fun joinsDistinctBrandAndName() {
        assertEquals("Alpine Merino DK", displayName("Alpine", "Merino DK"))
        assertEquals("Alp Alpine Merino", displayName("Alp", "Alpine Merino"))
    }

    @Test
    fun usesAvailableNameOrFallsBackToCardId() {
        assertEquals("Merino DK", displayName(" ", " Merino DK "))
        assertEquals("Alpine", displayName(" Alpine ", " "))
        assertEquals("Yarn #42", displayName(" ", " "))
    }

    private fun displayName(
        brand: String,
        name: String,
    ): String = YarnCard(id = 42, brand = brand, yarnName = name).displayName { "Yarn #$it" }
}

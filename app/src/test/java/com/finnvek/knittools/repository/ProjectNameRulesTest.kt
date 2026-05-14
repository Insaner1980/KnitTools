package com.finnvek.knittools.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectNameRulesTest {
    @Test
    fun `normalize palauttaa nullin tyhjalle nimelle`() {
        assertNull(ProjectNameRules.normalize("   "))
    }

    @Test
    fun `uniqueName lisaa jarjestysnumeron saman nimiseen projektiin`() {
        val name =
            ProjectNameRules.uniqueName(
                requestedName = "Sukat",
                existingNames = listOf("Sukat", "Sukat (2)"),
            )

        assertEquals("Sukat (3)", name)
    }
}

package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PatternAnnotationOwnerTest {
    @Test
    fun `saved document key is stable and versioned`() {
        assertEquals("saved:42:v1", PatternAnnotationDocumentKey.savedPattern(42L))
    }

    @Test
    fun `legacy project document key is stable`() {
        assertEquals("legacy-project:7", PatternAnnotationDocumentKey.legacyProject(7L))
    }
}

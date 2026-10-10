package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDisplayNamesTest {
    @Test
    fun `upper case file name becomes title case without extension`() {
        assertEquals("Step By Step Sweater V3", PatternDisplayNames.fromFileName("STEP_BY_STEP_SWEATER_V3.pdf"))
    }

    @Test
    fun `mixed case name keeps its casing and loses trailing dots`() {
        assertEquals("Scales and Fins Scarf 2026", PatternDisplayNames.fromFileName("Scales and Fins Scarf 2026. .pdf"))
    }

    @Test
    fun `lower case name is title cased and underscores collapse`() {
        assertEquals("Harbor Socks", PatternDisplayNames.fromFileName("harbor__socks.PDF"))
    }

    @Test
    fun `name without letters falls back to the original`() {
        assertEquals("..pdf", PatternDisplayNames.fromFileName("..pdf"))
    }

    @Test
    fun `document label fits the project document label limit`() {
        val label =
            PatternDisplayNames.documentLabel(
                "A_VERY_LONG_PATTERN_NAME_FROM_A_DESIGNER_SHOP_WITH_SIZES_AND_VERSION_2026.pdf",
            )

        assertTrue(label.length <= PROJECT_DOCUMENT_LABEL_MAX_LENGTH)
        assertEquals(ProjectDocumentLabelValidation.Valid(label), validateProjectDocumentLabel(label))
        assertEquals("Harbor Socks", PatternDisplayNames.documentLabel("harbor__socks.PDF"))
    }

    @Test
    fun `display keeps labels that are not file names`() {
        assertEquals("My sweater_notes", PatternDisplayNames.forDisplay("My sweater_notes"))
        assertEquals("Step By Step Sweater V3", PatternDisplayNames.forDisplay("STEP_BY_STEP_SWEATER_V3.pdf"))
    }
}

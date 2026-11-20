package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDisplayNamesTest {
    @Test
    fun `document label does not split a supplementary character at the limit`() {
        val prefix = "Ab" + "x".repeat(PROJECT_DOCUMENT_LABEL_MAX_LENGTH - 3)
        assertEquals(prefix, PatternDisplayNames.documentLabel("$prefix\uD83E\uDDF6.pdf"))
        val fittingPrefix = prefix.dropLast(1)
        assertEquals(
            "$fittingPrefix\uD83E\uDDF6",
            PatternDisplayNames.documentLabel("$fittingPrefix\uD83E\uDDF6.pdf"),
        )
    }

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

    @Test
    fun `legacy photo labels use the translated fallback without changing similar user labels`() {
        val fallback = "Ohjeen kuvat"
        assertEquals(fallback, PatternDisplayNames.forDisplay("pattern-images-1795083818707.pdf", fallback))
        assertEquals(fallback, PatternDisplayNames.forDisplay("Pattern-images-1795083818707-2", fallback))
        assertEquals("Pattern-images-my-photo", PatternDisplayNames.forDisplay("Pattern-images-my-photo", fallback))
    }

    @Test
    fun `photo labels reserve room for the next available suffix`() {
        val prefix = "Project " + "A".repeat(39)
        val label = "$prefix\uD83E\uDDF6x"
        assertEquals(50, label.length)
        assertEquals(label, PatternDisplayNames.uniqueDocumentLabel(label, emptySet()))
        assertEquals(
            "$prefix 3",
            PatternDisplayNames.uniqueDocumentLabel(label, setOf(label, "$prefix 2")),
        )
    }
}

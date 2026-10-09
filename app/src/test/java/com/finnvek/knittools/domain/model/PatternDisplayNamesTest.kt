package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
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
    fun `display keeps labels that are not file names`() {
        assertEquals("My sweater_notes", PatternDisplayNames.forDisplay("My sweater_notes"))
        assertEquals("Step By Step Sweater V3", PatternDisplayNames.forDisplay("STEP_BY_STEP_SWEATER_V3.pdf"))
    }
}

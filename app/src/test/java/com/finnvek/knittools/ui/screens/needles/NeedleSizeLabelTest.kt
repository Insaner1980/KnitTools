package com.finnvek.knittools.ui.screens.needles

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NeedleSizeLabelTest {
    @Test
    fun `decimal size labels use the locale separator like the mm column`() {
        assertEquals("1,5", localizedSizeLabel("1.5", Locale.forLanguageTag("fi")))
        assertEquals("1.5", localizedSizeLabel("1.5", Locale.US))
    }

    @Test
    fun `non decimal size labels stay unchanged`() {
        listOf("000", "14", "—", "10.5/11").forEach { label ->
            assertEquals(label, localizedSizeLabel(label, Locale.forLanguageTag("fi")))
        }
    }
}

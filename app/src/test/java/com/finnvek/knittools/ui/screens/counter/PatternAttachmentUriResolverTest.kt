package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatternAttachmentUriResolverTest {
    @Test
    fun `app owned pattern keeps original uri without a copy`() {
        val result =
            resolvePatternAttachmentUri(
                sourceUriString = "file:///data/user/0/app/files/pattern_pdfs/1/pattern.pdf",
                copiedUriString = null,
                isSourceAppOwned = true,
            )

        assertEquals("file:///data/user/0/app/files/pattern_pdfs/1/pattern.pdf", result)
    }

    @Test
    fun `external pattern uses copied uri`() {
        val result =
            resolvePatternAttachmentUri(
                sourceUriString = "content://external/pattern.pdf",
                copiedUriString = "file:///data/user/0/app/files/pattern_pdfs/2/pattern.pdf",
                isSourceAppOwned = false,
            )

        assertEquals("file:///data/user/0/app/files/pattern_pdfs/2/pattern.pdf", result)
    }

    @Test
    fun `external pattern is not attached when copy fails`() {
        val result =
            resolvePatternAttachmentUri(
                sourceUriString = "content://external/pattern.pdf",
                copiedUriString = null,
                isSourceAppOwned = false,
            )

        assertNull(result)
    }
}

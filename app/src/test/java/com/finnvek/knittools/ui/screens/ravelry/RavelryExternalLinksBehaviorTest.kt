package com.finnvek.knittools.ui.screens.ravelry

import com.finnvek.knittools.data.remote.PatternDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RavelryExternalLinksBehaviorTest {
    @Test
    fun `detail ravelry URL rejects non-ravelry canonical URL`() {
        val detail =
            PatternDetail(
                id = 42,
                permalink = "cozy-hat",
                canonicalUrl = "https://example.com/patterns/library/cozy-hat",
            )

        assertNull(detail.ravelryUrlOrNull())
    }

    @Test
    fun `detail ravelry URL accepts canonical ravelry URL`() {
        val detail =
            PatternDetail(
                id = 42,
                permalink = "cozy-hat",
                canonicalUrl = "https://www.ravelry.com/patterns/library/cozy-hat",
            )

        assertEquals(
            "https://www.ravelry.com/patterns/library/cozy-hat",
            detail.ravelryUrlOrNull(),
        )
    }
}

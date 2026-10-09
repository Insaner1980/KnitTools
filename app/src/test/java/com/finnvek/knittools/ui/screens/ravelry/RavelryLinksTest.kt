package com.finnvek.knittools.ui.screens.ravelry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RavelryLinksTest {
    @Test
    fun `ravelry page URL accepts https ravelry pattern page`() {
        assertEquals(
            "https://www.ravelry.com/patterns/library/cozy-hat",
            ravelryPageUrlOrNull(" https://www.ravelry.com/patterns/library/cozy-hat "),
        )
    }

    @Test
    fun `ravelry page URL rejects non-https scheme`() {
        assertNull(ravelryPageUrlOrNull("http://www.ravelry.com/patterns/library/cozy-hat"))
    }

    @Test
    fun `ravelry page URL rejects host confusion`() {
        assertNull(ravelryPageUrlOrNull("https://www.ravelry.com.evil.example/patterns/library/cozy-hat"))
        assertNull(ravelryPageUrlOrNull("https://www.ravelry.com@evil.example/patterns/library/cozy-hat"))
    }

    @Test
    fun `ravelry page URL rejects malformed input`() {
        assertNull(ravelryPageUrlOrNull("not a url"))
    }
}

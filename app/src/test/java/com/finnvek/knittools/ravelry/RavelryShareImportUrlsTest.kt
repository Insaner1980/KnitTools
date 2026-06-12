package com.finnvek.knittools.ravelry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RavelryShareImportUrlsTest {
    @Test
    fun `extracts ravelry pattern url from shared text`() {
        assertEquals(
            "https://www.ravelry.com/patterns/library/cozy-hat",
            extractPatternUrl("Save this: https://www.ravelry.com/patterns/library/cozy-hat"),
        )
    }

    @Test
    fun `accepts ravelry host without www and preserves query`() {
        assertEquals(
            "https://ravelry.com/patterns/library/cozy-hat?utm_source=share",
            extractPatternUrl("https://ravelry.com/patterns/library/cozy-hat?utm_source=share"),
        )
    }

    @Test
    fun `trims trailing punctuation from shared url`() {
        assertEquals(
            "https://www.ravelry.com/patterns/library/cozy-hat",
            extractPatternUrl("Look: https://www.ravelry.com/patterns/library/cozy-hat."),
        )
    }

    @Test
    fun `rejects non pattern ravelry urls`() {
        assertNull(extractPatternUrl("https://www.ravelry.com/patterns/search"))
    }

    @Test
    fun `rejects non ravelry hosts`() {
        assertNull(extractPatternUrl("https://evil.example/patterns/library/cozy-hat"))
    }

    @Test
    fun `rejects non https ravelry urls`() {
        assertNull(extractPatternUrl("http://www.ravelry.com/patterns/library/cozy-hat"))
    }

    private fun extractPatternUrl(text: String?): String? {
        val type =
            runCatching {
                Class.forName("com.finnvek.knittools.ravelry.RavelryShareImportUrls")
            }.getOrElse { error ->
                throw AssertionError("RavelryShareImportUrls object is missing", error)
            }
        val instance = type.getField("INSTANCE").get(null)
        return type
            .getMethod("extractPatternUrl", String::class.java)
            .invoke(instance, text) as String?
    }
}

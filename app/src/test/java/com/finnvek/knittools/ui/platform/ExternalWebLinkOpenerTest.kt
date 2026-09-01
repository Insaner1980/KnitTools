package com.finnvek.knittools.ui.platform

import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalWebLinkOpenerTest {
    @Test
    fun `valid link launches the preserved original url`() {
        val launched = mutableListOf<String>()

        val result =
            openExternalWebLink("  https://example.com/Pattern?Size=XL#Notes  ") { url ->
                launched += url
            }

        assertEquals(ExternalWebLinkOpenResult.Opened, result)
        assertEquals(listOf("https://example.com/Pattern?Size=XL#Notes"), launched)
    }

    @Test
    fun `invalid link never reaches the external launcher`() {
        var launched = false

        val result = openExternalWebLink("content://patterns/1") { launched = true }

        assertEquals(ExternalWebLinkOpenResult.InvalidUrl, result)
        assertTrue(!launched)
    }

    @Test
    fun `missing browser and rejected launch have distinct safe results`() {
        assertEquals(
            ExternalWebLinkOpenResult.NoBrowser,
            openExternalWebLink("https://example.com/pattern") { throw ActivityNotFoundException() },
        )
        assertEquals(
            ExternalWebLinkOpenResult.Failed,
            openExternalWebLink("https://example.com/pattern") { throw SecurityException() },
        )
    }
}

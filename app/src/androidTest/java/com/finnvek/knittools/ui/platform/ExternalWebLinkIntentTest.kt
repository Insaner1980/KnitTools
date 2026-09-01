package com.finnvek.knittools.ui.platform

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalWebLinkIntentTest {
    @Test
    fun externalWebLinkIntent_isBrowsableViewIntentWithoutTargetPackage() {
        val intent = createExternalWebLinkIntent("https://example.com/Pattern?Size=XL#Notes")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://example.com/Pattern?Size=XL#Notes", intent.dataString)
        assertTrue(intent.categories.orEmpty().contains(Intent.CATEGORY_BROWSABLE))
        assertEquals(null, intent.`package`)
        assertEquals(null, intent.component)
    }
}

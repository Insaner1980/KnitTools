package com.finnvek.knittools.ui.screens.pattern

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class PatternViewerSourceTest {
    @Test
    fun `renderer errors are not displayed from raw exception messages`() {
        val source =
            String(
                Files.readAllBytes(
                    Paths.get("src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"),
                ),
            )

        assertFalse(source.contains("rendererError = error.message"))
        assertTrue(source.contains("rendererError = patternOpenFailed"))
    }
}

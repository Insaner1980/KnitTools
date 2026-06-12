package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryAccountHeaderSourceTest {
    @Test
    fun `ravelry account header keeps disconnect out of primary actions`() {
        val header = ProjectSourceFiles.read(RAVELRY_HEADER)
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val detailScreen = ProjectSourceFiles.read(RAVELRY_DETAIL_SCREEN)

        assertTrue(header.contains("internal fun RavelryAccountHeader("))
        assertTrue(header.contains("onBrowseRavelry: () -> Unit"))
        assertTrue(header.contains("RavelryAuthState.Connected"))
        assertTrue(header.contains("R.string.ravelry_browse"))
        assertTrue(header.contains("Icons.Filled.MoreVert"))
        assertTrue(header.contains("DropdownMenu("))
        assertTrue(header.contains("DropdownMenuItem("))
        assertTrue(header.contains("Text(stringResource(R.string.ravelry_disconnect))"))
        assertFalse(header.contains("OutlinedButton(onClick = onDisconnect"))

        assertTrue(searchScreen.contains("RavelryAccountHeader("))
        assertTrue(detailScreen.contains("RavelryAccountHeader("))
        assertFalse(searchScreen.contains("RavelrySignInPrompt("))
        assertFalse(detailScreen.contains("RavelrySignInPrompt("))
    }

    private companion object {
        private const val RAVELRY_HEADER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryAccountHeader.kt"
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val RAVELRY_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryDetailScreen.kt"
    }
}

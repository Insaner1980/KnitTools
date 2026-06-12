package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryBrowseCustomTabsSourceTest {
    @Test
    fun `main activity opens ravelry pattern search in custom tab with browser share enabled`() {
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)

        assertTrue(mainActivity.contains("fun launchRavelryBrowse()"))
        assertTrue(
            mainActivity.contains(
                "private const val RAVELRY_PATTERN_SEARCH_URL = \"https://www.ravelry.com/patterns/search\"",
            ),
        )
        assertTrue(mainActivity.contains("CustomTabsIntent.SHARE_STATE_ON"))
        assertTrue(mainActivity.contains(".setShareState(CustomTabsIntent.SHARE_STATE_ON)"))
        assertTrue(mainActivity.contains(".launchUrl(this, Uri.parse(RAVELRY_PATTERN_SEARCH_URL))"))
        assertFalse(mainActivity.contains("setActionButton("))
        assertFalse(mainActivity.contains("addMenuItem("))
    }

    @Test
    fun `browse ravelry callback is wired through ravelry routes`() {
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(mainActivity.contains("onBrowseRavelry = ::launchRavelryBrowse"))
        assertTrue(navGraph.contains("onBrowseRavelry: () -> Unit = {}"))
        assertTrue(navGraph.contains("onBrowseRavelry = onBrowseRavelry"))
        assertTrue(navGraph.contains("RavelrySearchRoute("))
        assertTrue(navGraph.contains("RavelryDetailScreen("))
    }

    private companion object {
        private const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
    }
}

package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelrySearchTabSourceTest {
    @Test
    fun `search tab keeps search visible but disabled until connected`() {
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val strings = ProjectSourceFiles.read(BASE_STRINGS)

        assertTrue(searchScreen.contains("import com.finnvek.knittools.auth.RavelryAuthState"))
        assertTrue(searchScreen.contains("canSearch = authState is RavelryAuthState.Connected"))
        assertTrue(searchScreen.contains("val canSearch: Boolean,"))
        assertTrue(searchScreen.contains("enabled = state.canSearch"))
        assertTrue(searchScreen.contains("R.string.ravelry_search_requires_sign_in"))
        assertTrue(
            strings.contains(
                "<string name=\"ravelry_search_requires_sign_in\">Sign in with Ravelry to search patterns.</string>",
            ),
        )
    }

    @Test
    fun `search actions and pagination are gated while saved patterns tab stays available`() {
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)
        val tabRow = searchScreen.substringAfter("PrimaryTabRow(").substringBefore("when (selectedTab)")

        assertTrue(searchScreen.contains("internal fun shouldRequestRavelryLoadMore("))
        assertTrue(searchScreen.contains("shouldLoadMore &&\n        canSearch &&"))
        assertTrue(searchScreen.contains("canSearch = state.canSearch"))
        assertTrue(
            Regex("""if \(state\.canSearch\) \{\s+onSearch\(\)\s+}""")
                .containsMatchIn(searchScreen),
        )
        assertTrue(
            searchScreen.contains(
                "actionLabel = if (state.canSearch) retryLabel else null",
            ),
        )
        assertTrue(searchScreen.contains("onAction = if (state.canSearch) onSearch else null"))
        assertTrue(searchScreen.contains("onAction = if (state.canSearch) onLoadMore else null"))

        assertTrue(tabRow.contains("R.string.ravelry_saved_patterns"))
        assertFalse(tabRow.contains("enabled = false"))
        assertFalse(tabRow.contains("authState is RavelryAuthState.Connected"))
    }

    private companion object {
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val BASE_STRINGS =
            "app/src/main/res/values/strings.xml"
    }
}

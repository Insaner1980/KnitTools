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
        assertTrue(searchScreen.contains("enabled = canSearch"))
        // Tilikortti kertoo kirjautumisesta; samaa viestiä ei toisteta kentän alla.
        assertFalse(searchScreen.contains("ravelry_search_requires_sign_in"))
        assertFalse(strings.contains("ravelry_search_requires_sign_in"))
        // Käytöstä poistettu kenttä näyttää käytöstä poistetulta, ja vihje kattaa myös virkkauksen.
        assertTrue(
            ProjectSourceFiles
                .read("app/src/main/java/com/finnvek/knittools/ui/components/TextFieldColors.kt")
                .contains("disabledContainerColor = container.copy(alpha = DISABLED_FIELD_CONTAINER_ALPHA)"),
        )
        assertTrue(strings.contains("<string name=\"search_hint\">Search patterns…</string>"))
    }

    @Test
    fun `search actions and pagination are gated and saved patterns live only in the Library`() {
        val searchScreen = ProjectSourceFiles.read(RAVELRY_SEARCH_SCREEN)

        assertTrue(searchScreen.contains("internal fun shouldRequestRavelryLoadMore("))
        assertTrue(searchScreen.contains("shouldLoadMore &&\n        canSearch &&"))
        assertTrue(searchScreen.contains("canSearch = state.canSearch"))
        assertTrue(
            Regex("""if \(canSearch\) \{\s+onSearch\(\)\s+}""")
                .containsMatchIn(searchScreen),
        )
        assertTrue(
            searchScreen.contains(
                "actionLabel = if (canSearch) retryLabel else null",
            ),
        )
        assertTrue(searchScreen.contains("hasResults -> onLoadMore"))
        assertTrue(searchScreen.contains("else -> onSearch"))

        // Tallennetut kaavat ovat vain Libraryssa; Ravelry-näkymä linkittää sinne ilman omaa välilehteä.
        assertFalse(searchScreen.contains("PrimaryTabRow("))
        assertFalse(searchScreen.contains("private fun SavedTab("))
        assertTrue(searchScreen.contains("title = stringResource(R.string.ravelry_saved_patterns)"))
        assertTrue(searchScreen.contains("onClick = actions.onOpenSavedPatterns"))
    }

    private companion object {
        private const val RAVELRY_SEARCH_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt"
        private const val BASE_STRINGS =
            "app/src/main/res/values/strings.xml"
    }
}

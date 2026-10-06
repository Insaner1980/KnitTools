package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Lukitsee yhtenäisen ulkoasun: näkymät eivät määrittele omia korttivärejä, osio-otsikoita,
 * tekstikenttävärejä tai valintakorostuksia, vaan käyttävät yhteisiä komponentteja ja
 * `knitToolsColors`-tokeneita. Sallitut poikkeukset on lueteltu tarkalla määrällä, jotta uusi
 * paikallinen kopio kaatuu testiin eikä huku vanhojen sekaan.
 */
class UiConsistencySourceTest {
    @Test
    fun `screens use card tokens instead of raw surface roles`() {
        // Laskurin omat ohjaimet (askelpainikkeet, toistolaskuri, edistymisraita) eivät ole kortteja.
        assertOnlyAllowed(
            Regex("""colorScheme\.(surfaceVariant|surfaceContainer\w*|secondaryContainer)\b"""),
            mapOf(
                "counter/CounterWorkspaceSections.kt" to 3,
                "counter/MultiCounterComponents.kt" to 1,
            ),
            "Käytä knitToolsColors.cardContainer / cardContainerColor(selected) / tableHeaderContainer",
        )
    }

    @Test
    fun `screens use SectionLabel instead of private uppercase headers`() {
        // Sallitut: laskurin projektinimi, toistolaskurin nimike ja Insightsin tilastosarakkeen nimike.
        assertOnlyAllowed(
            Regex("""\.localizedUppercase\(\)"""),
            mapOf(
                "counter/CounterScreen.kt" to 1,
                "counter/CounterWorkspaceSections.kt" to 1,
                "insights/InsightsSections.kt" to 1,
            ),
            "Käytä ui/components/SectionLabel-komponenttia",
        )
    }

    @Test
    fun `screens use shared text field colors`() {
        // Muistiinpanoeditori on koko näytön kirjoituspinta ilman kenttää.
        assertOnlyAllowed(
            Regex("""\b(Outlined)?TextFieldDefaults\.colors\("""),
            mapOf("notes/NotesEditorScreen.kt" to 1),
            "Käytä highContainerTextFieldColors(), dialogTextFieldColors() tai cardTextFieldColors()",
        )
    }

    @Test
    fun `screens use selected card token instead of primary tints`() {
        // Sallittu: PDF-lukijan lukuviivan kaista. Langan In Use -merkki käyttää actionContaineria.
        assertOnlyAllowed(
            Regex("""colorScheme\.primary\.copy\(alpha"""),
            mapOf("pattern/PatternViewerScreen.kt" to 1),
            "Käytä cardContainerColor(selected) tai knitToolsColors.selectedCardContainer / actionContainer",
        )
    }

    @Test
    fun `retired card tokens and section header stay removed`() {
        val sources = screenSources() + componentSources()
        listOf(
            """\bprojectCardContainer\b""",
            """\bprimaryTintContainer\b""",
            """\bprojectActionsSectionHeader\b""",
            // Vanha ikonillinen otsikko; OverviewSectionHeader ja InsightsSectionHeader käyttävät SectionLabelia.
            """(?<![A-Za-z])SectionHeader\(""",
        ).map(::Regex).forEach { retired ->
            val offenders = sources.filter { (_, text) -> retired.containsMatchIn(text) }.keys
            assertEquals("${retired.pattern} palasi: $offenders", emptySet<String>(), offenders)
        }
    }

    private fun assertOnlyAllowed(
        pattern: Regex,
        allowed: Map<String, Int>,
        hint: String,
    ) {
        val actual =
            screenSources()
                .mapValues { (_, text) -> pattern.findAll(text).count() }
                .filterValues { it > 0 }
        assertEquals(hint, allowed, actual)
    }

    private fun screenSources(): Map<String, String> = sourcesUnder(SCREENS)

    private fun componentSources(): Map<String, String> = sourcesUnder(COMPONENTS)

    private fun sourcesUnder(relativeRoot: String): Map<String, String> {
        val root = ProjectSourceFiles.file(relativeRoot)
        return Files.walk(root).use { paths ->
            paths
                .filter { it.toString().endsWith(".kt") }
                .toList()
                .associate { path: Path ->
                    root.relativize(path).toString().replace('\\', '/') to ProjectSourceFiles.read(path)
                }
        }
    }

    private companion object {
        const val SCREENS = "app/src/main/java/com/finnvek/knittools/ui/screens"
        const val COMPONENTS = "app/src/main/java/com/finnvek/knittools/ui/components"
    }
}

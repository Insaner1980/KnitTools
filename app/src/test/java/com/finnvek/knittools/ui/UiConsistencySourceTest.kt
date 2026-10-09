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
        // Sallitut: toistolaskurin nimike ja Insightsin tilastosarakkeen nimike. Projektin nimeä ei muuteta
        // versaaleiksi missään, myös laskurin yläpalkki näyttää sen kuten projektinäkymä.
        assertOnlyAllowed(
            Regex("""\.localizedUppercase\(\)"""),
            mapOf(
                "counter/CounterWorkspaceSections.kt" to 1,
                "insights/InsightsSections.kt" to 1,
            ),
            "Käytä ui/components/SectionLabel-komponenttia",
        )
    }

    @Test
    fun `project names keep their casing in section labels`() {
        // SectionLabel muuttaa tekstin versaaleiksi; projektin nimi kulkee ProjectNameLabelin kautta.
        assertOnlyAllowed(
            Regex("""SectionLabel\(\s*(text\s*=\s*)?[\w.]*(projectName|project\.name)\b"""),
            emptyMap(),
            "Käytä ProjectNameLabel-komponenttia: projektin nimeä ei muuteta versaaleiksi",
        )
    }

    @Test
    fun `sheets use SheetTitle and the shared surface`() {
        // Projektin toimintosheetin otsikkona toimii projektikortti (ProjectOverviewLink).
        val titleExceptions = setOf("counter/ProjectActionsBottomSheet.kt")
        val sheetCall = Regex("""(?<![\w.])ModalBottomSheet\(""")
        // Kommentit ja merkkijonot pois: kommentoitu "SheetTitle(" ei saa hyväksyä otsikotonta sheetiä.
        val sources = (screenSources() + componentSources()).mapValues { (_, text) -> text.codeOnly() }
        // Otsikko tarkistetaan jokaisen sheetin omasta sisällöstä: yksi otsikko ei saa kelvata
        // saman tiedoston kaikille sheeteille. Sisältö voi olla saman tiedoston apufunktiossa.
        val withoutTitle =
            sources
                .filterKeys { it !in titleExceptions }
                .mapValues { (_, text) ->
                    sheetCall.findAll(text).count { match ->
                        val argumentsEnd = match.range.last + callArguments(text, match.range.last).length
                        !hasSheetTitle(text, trailingBlock(text, argumentsEnd))
                    }
                }.filterValues { it > 0 }
        assertEquals("Käytä SheetTitlea jokaisen sheetin otsikkona", emptyMap<String, Int>(), withoutTitle)

        val withoutSurface =
            sources
                .mapValues { (_, text) ->
                    sheetCall.findAll(text).count { match ->
                        "containerColor = MaterialTheme.colorScheme.surface" !in callArguments(text, match.range.last)
                    }
                }.filterValues { it > 0 }
        assertEquals(
            "Sheetin pohja on colorScheme.surface kuten muissa sheeteissä",
            emptyMap<String, Int>(),
            withoutSurface,
        )
    }

    @Test
    fun `choices use SegmentedToggle instead of FilterChip`() {
        // Sallittu: PDF-merkintöjen vieritettävä työkalupaletti, jossa tilat ja toiminnot ovat rinnakkain.
        // Myös jaetut komponentit, jottei sirurivi palaa komponentin kautta.
        assertOnlyAllowed(
            Regex("""(?<![\w.])FilterChip\("""),
            mapOf("pattern/PatternAnnotationToolbar.kt" to 1),
            "Käytä SegmentedTogglea tai ProjectFilterPilliä",
            sources = screenSources() + componentSources(),
        )
    }

    @Test
    fun `simple confirmations use ConfirmationDialog`() {
        // Sallitut: lomakedialogit (nimeäminen, laskurin lisäys, tavoite, silmukat, sivulle siirto, kansio),
        // monen vaihtoehdon istuntodialogit sekä verkko-ohjeen korvaus- ja jakovalinnat tilakytkimineen.
        assertOnlyAllowed(
            Regex("""(?<![\w.])AlertDialog\("""),
            mapOf(
                "counter/CounterScreen.kt" to 6,
                "counter/MultiCounterComponents.kt" to 2,
                "counter/PhotoGalleryScreen.kt" to 1,
                "counter/TargetRowsDialog.kt" to 1,
                "library/WebPatternEditorScreen.kt" to 2,
                "pattern/PatternViewerScreen.kt" to 2,
                "project/ProjectFolderComponents.kt" to 2,
            ),
            "Käytä ConfirmationDialogia tavallisiin vahvistuksiin (isDestructive poistoille)",
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
        sources: Map<String, String> = screenSources(),
    ) {
        val actual =
            sources
                .mapValues { (_, text) -> pattern.findAll(text).count() }
                .filterValues { it > 0 }
        assertEquals(hint, allowed, actual)
    }

    /** Kutsua seuraava sisältölohko `{ … }`, eli sheetin sisältö. */
    private fun trailingBlock(
        text: String,
        fromIndex: Int,
    ): String {
        val open = text.indexOf('{', fromIndex)
        if (open < 0) return ""
        var depth = 0
        for (index in open until text.length) {
            when (text[index]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return text.substring(open, index)
            }
        }
        return text.substring(open)
    }

    /** Otsikko suoraan sisällössä tai saman tiedoston apufunktiossa, jota sisältö kutsuu. */
    private fun hasSheetTitle(
        fileText: String,
        block: String,
    ): Boolean {
        if (block.containsTitle()) return true
        return Regex("""\b([A-Z]\w+)\(""").findAll(block).any { call ->
            val declaration = Regex("""fun ${call.groupValues[1]}\(""").find(fileText) ?: return@any false
            val bodyStart = declaration.range.last + callArguments(fileText, declaration.range.last).length
            trailingBlock(fileText, bodyStart).containsTitle()
        }
    }

    private fun String.containsTitle(): Boolean = titleCall.containsMatchIn(this)

    /** Lähdekoodi ilman kommentteja ja merkkijonoja; merkkijonot jäävät tyhjiksi, jotta rakenne säilyy. */
    private fun String.codeOnly(): String =
        commentOrString.replace(this) { match ->
            if (match.value.startsWith("\"")) "\"\"" else ""
        }

    /** Kutsun argumentit avaavasta sulkeesta vastaavaan sulkevaan asti. */
    private fun callArguments(
        text: String,
        openParenIndex: Int,
    ): String {
        var depth = 0
        for (index in openParenIndex until text.length) {
            when (text[index]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return text.substring(openParenIndex, index)
            }
        }
        return text.substring(openParenIndex)
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

        /** Varsinainen kutsu, ei esim. `SheetTitleText(` tai `.SheetTitle(`. */
        val titleCall = Regex("""(?<![\w.])(SheetTitle|FormSheet)\(""")

        // Raakamerkkijono, tavallinen merkkijono, rivikommentti tai lohkokommentti.
        val commentOrString = Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|//[^\\n]*|/\\*[\\s\\S]*?\\*/")
    }
}

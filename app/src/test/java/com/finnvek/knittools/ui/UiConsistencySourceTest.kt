package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `main step uses shared face and form without modifying image buttons`() {
        val workspace = screenSources().getValue("counter/CounterWorkspaceSections.kt")
        val sheet = screenSources().getValue("counter/MainCounterStepSheet.kt")
        assertTrue(workspace.contains("CounterStepButtonFace("))
        assertFalse(workspace.contains("SegmentedToggle("))
        assertFalse(workspace.contains("drawCircle("))
        assertTrue(sheet.contains("FormSheet("))
        assertTrue(sheet.contains("SegmentedToggle("))
        assertTrue(sheet.contains("NumberInputField("))
        assertEquals(3, Regex("CounterImageButton\\(").findAll(workspace).count())
    }

    @Test
    fun `page-level notes use the shared InfoNote`() {
        val pages = screenSources()
        val notes =
            mapOf(
                "chartsymbols/ChartSymbolScreen.kt" to "R.string.chart_symbols_disclaimer",
                "yarn/YarnEstimatorScreen.kt" to "R.string.extra_skein_note",
                "needles/NeedleSizeScreen.kt" to "R.string.needle_size_disclaimer",
                "gauge/GaugeScreen.kt" to "task.warningResource()",
            )
        notes.forEach { (page, body) ->
            val source = pages.getValue(page)
            assertTrue(page, source.contains(Regex("InfoNote\\(\\s*text = stringResource\\(" + Regex.escape(body))))
        }
    }

    @Test
    fun `screens use card tokens instead of raw surface roles`() {
        // Laskurin omat ohjaimet (askelpainikkeet, toistolaskuri, edistymisraita) eivät ole kortteja.
        assertOnlyAllowed(
            Regex("""colorScheme\.(surfaceVariant|surfaceContainer\w*|secondaryContainer)\b"""),
            mapOf(
                "counter/CounterWorkspaceSections.kt" to 2,
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
    fun `title check ignores titles in nested comments and strings`() {
        val untitled =
            listOf(
                "ModalBottomSheet(onDismissRequest = {}) {",
                "    /* ulompi /* sisempi */ SheetTitle(text = otsikko) */",
                "    Text(\"SheetTitle(\")",
                "    // SheetTitle(text = otsikko)",
                "}",
            ).joinToString("\n")

        assertEquals(false, titleCall.containsMatchIn(untitled.codeOnly()))
        assertEquals(true, titleCall.containsMatchIn("SheetTitle(text = otsikko)".codeOnly()))
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
        assertOnlyAllowed(
            Regex("""(?<![\w.])AlertDialog\("""),
            emptyMap(),
            "Käytä ConfirmationDialogia tavallisiin vahvistuksiin (isDestructive poistoille)",
        )
    }

    @Test
    fun `screens use shared text fields`() {
        // The notes screen and its in-counter editor use a continuous writing surface.
        assertOnlyAllowed(
            Regex("""\b(?:TextField|OutlinedTextField)\s*\("""),
            mapOf(
                "counter/CounterScreen.kt" to 1,
                "notes/NotesEditorScreen.kt" to 1,
            ),
            "Use LabeledTextField, NumberInputField, or SearchTextField",
            sources = screenSources().mapValues { (_, text) -> text.codeOnly() },
        )
    }

    @Test
    fun `form labels are outside text fields including shared PDF import`() {
        val sources = screenSources().mapValues { (_, source) -> source.codeOnly() }
        val fields = Regex("""\b(?:TextField|OutlinedTextField)\s*\(""")
        val inlineLabel = Regex("""\blabel\s*=\s*\{""")
        val violations =
            sources
                .filterValues { source ->
                    fields.findAll(source).any { field ->
                        inlineLabel.containsMatchIn(callArguments(source, field.range.last))
                    }
                }.keys
        assertEquals(emptySet<String>(), violations)
        assertTrue(sources.getValue("library/IncomingPdfSheet.kt").contains("LabeledTextField("))
    }

    @Test
    fun `delete and remove menu actions use the destructive color`() {
        val menuCalls = Regex("""\b(DropdownMenuItem|RowMenuAction|FolderMenuItem)\s*\(""")
        val destructiveLabel = Regex("""\bR\.string\.(?:delete\w*|remove\w*|\w+_(?:delete|remove)(?:_action)?)\b""")
        val sources = (screenSources() + componentSources()).mapValues { (_, source) -> source.codeOnly() }
        val violations =
            sources
                .mapValues { (_, source) ->
                    menuCalls
                        .findAll(source)
                        .mapNotNull { menu ->
                            val arguments = callArguments(source, menu.range.last)
                            val label = destructiveLabel.find(arguments)?.value ?: return@mapNotNull null
                            val destructive =
                                if (menu.groupValues[1] == "DropdownMenuItem") {
                                    "MaterialTheme.colorScheme.error" in arguments
                                } else {
                                    "isDestructive = true" in arguments
                                }
                            label.takeUnless { destructive }
                        }.toList()
                }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap<String, List<String>>(), violations)
    }

    @Test
    fun `screens use shared text field colors`() {
        // Muistiinpanoeditori on koko näytön kirjoituspinta ilman kenttää.
        assertOnlyAllowed(
            Regex("""\b(Outlined)?TextFieldDefaults\.colors\("""),
            mapOf("notes/NotesEditorScreen.kt" to 1),
            "Use the shared text field components",
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

    @Test
    fun `pro markers use the local resolver and the retired badge stays removed`() {
        val sources = (screenSources() + componentSources()).mapValues { (_, text) -> text.codeOnly() }
        val retired = Regex("""\b(ProBadge|ProBadgeState|proBadgeDescription)\b""")
        assertFalse(componentSources().containsKey("ProBadge.kt"))
        assertEquals(emptySet<String>(), sources.filterValues(retired::containsMatchIn).keys)
        val violations =
            sources
                .mapValues { (_, code) ->
                    markerResolverViolations(code)
                }.filterValues { it.isNotEmpty() }
        assertEquals(
            "ProMarker saa proActionAccess-kutsun tai saman funktion immutable resolver-arvon",
            emptyMap<String, List<String>>(),
            violations,
        )
        assertTrue(sources.values.any { markerCall.containsMatchIn(it) })
    }

    @Test
    fun `marker resolver check rejects literals mutable aliases unrelated functions and ad hoc pro checks`() {
        val valid =
            listOf(
                "fun Row() { ProMarker(access = proActionAccess(state, ready, feature)) }",
                "fun Row() { val itemAccess = proActionAccess(state, ready, feature); Box { ProMarker(itemAccess) } }",
                "fun Row() { val itemAccess: ProActionAccess = proActionAccess(state, ready, feature); " +
                    "ProMarker(itemAccess) }",
                "fun ProMarker(access: ProActionAccess) { Text(label) }",
            )
        valid.forEach { assertTrue(it, markerResolverViolations(it).isEmpty()) }
        val invalid =
            listOf(
                "fun Row() { ProMarker(ProActionAccess.Locked) }",
                "fun Row() { var itemAccess = proActionAccess(state, ready, feature); ProMarker(itemAccess) }",
                "fun Row(access: ProActionAccess) { ProMarker(access) }",
                "fun Other() { val access = proActionAccess(state, ready, feature) }; fun Row() { ProMarker(access) }",
                "fun Row() { val access = if (state.isPro) ProActionAccess.Allowed else ProActionAccess.Locked; " +
                    "ProMarker(access) }",
                "fun Row() { if (state.isPro) { ProMarker(proActionAccess(state, ready, feature)) } }",
                "fun Row() { ProMarker(proActionAccess(state, ready, feature).let { ProActionAccess.Locked }) }",
                "fun Row() { val access = proActionAccess(state, ready, feature).let { ProActionAccess.Locked }; " +
                    "ProMarker(access) }",
                "fun Row() { /* val access = proActionAccess(state, ready, feature) */ ProMarker(access) }",
            )
        invalid.forEach { assertEquals(it, 1, markerResolverViolations(it).size) }
    }

    private fun markerResolverViolations(source: String): List<String> {
        val code = source.codeOnly()
        return markerCall
            .findAll(code)
            .filterNot { call ->
                val prefix = enclosingFunctionPrefix(code, call.range.first) ?: return@filterNot false
                val arguments =
                    callArguments(code, call.range.last)
                        .removePrefix("(")
                        .replaceFirst(Regex("""^\s*access\s*=\s*"""), "")
                        .trimStart()
                if (Regex("""\.\s*isPro\b""").containsMatchIn(prefix)) return@filterNot false
                if (isResolverExpression(arguments)) return@filterNot true
                val name = Regex("""^(\w+)\s*(?:,|$)""").find(arguments)?.groupValues?.get(1) ?: return@filterNot false
                val assignment =
                    Regex("""\b(val|var)\s+${Regex.escape(name)}(?:\s*:\s*ProActionAccess)?\s*=\s*""")
                        .findAll(prefix)
                        .lastOrNull() ?: return@filterNot false
                assignment.groupValues[1] == "val" &&
                    isResolverExpression(
                        prefix.substring(assignment.range.last + 1),
                        allowFollowingStatements = true,
                    )
            }.map { call -> callArguments(code, call.range.last).removePrefix("(").trim() }
            .toList()
    }

    /** Vain sama funktio kelpaa: toisen rivin resolver-arvo ei saa hyväksyä paikallista oikotietä. */
    private fun enclosingFunctionPrefix(
        code: String,
        callIndex: Int,
    ): String? =
        Regex("""\bfun\s+[\w.]+\s*\(""")
            .findAll(code)
            .mapNotNull { declaration ->
                val parametersEnd = declaration.range.last + callArguments(code, declaration.range.last).length
                val bodyStart = code.indexOf('{', parametersEnd)
                if (bodyStart < 0 || bodyStart > callIndex) return@mapNotNull null
                if (Regex(
                        """\bfun\b""",
                    ).containsMatchIn(code.substring(parametersEnd, bodyStart))
                ) {
                    return@mapNotNull null
                }
                val body = trailingBlock(code, parametersEnd)
                if (callIndex >= bodyStart + body.length) return@mapNotNull null
                bodyStart to code.substring(bodyStart, callIndex)
            }.maxByOrNull { it.first }
            ?.second

    private fun isResolverExpression(
        arguments: String,
        allowFollowingStatements: Boolean = false,
    ): Boolean {
        if (!arguments.startsWith("proActionAccess(")) return false
        val open = arguments.indexOf('(')
        val end = open + callArguments(arguments, open).length + 1
        val remaining = arguments.substring(end.coerceAtMost(arguments.length)).trimStart()
        if (remaining.isEmpty() || remaining.startsWith(',')) return true
        if (!allowFollowingStatements || remaining.startsWith('.') || remaining.startsWith('?')) return false
        return remaining.startsWith(';') || arguments.substring(end).contains('\n')
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

    /**
     * Lähdekoodi ilman kommentteja ja merkkijonoja; merkkijonot jäävät tyhjiksi, jotta rakenne säilyy.
     * Lohkokommentit lasketaan sisäkkäin kuten Kotlinissa: säännöllinen lauseke pysähtyi ensimmäiseen
     * sulkeutumiseen ja jätti ulomman kommentin loppuosan koodiksi.
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun String.codeOnly(): String {
        val code = StringBuilder(length)
        var index = 0
        while (index < length) {
            index =
                when {
                    startsWith(RAW_QUOTE, index) -> {
                        code.append("\"\"")
                        indexOf(RAW_QUOTE, index + RAW_QUOTE.length).let { end ->
                            if (end < 0) length else end + RAW_QUOTE.length
                        }
                    }

                    this[index] == '"' || this[index] == '\'' -> {
                        val quote = this[index]
                        var end = index + 1
                        while (end < length && this[end] != quote && this[end] != '\n') {
                            if (this[end] == '\\') end++
                            end++
                        }
                        code.append(quote).append(quote)
                        minOf(end + 1, length)
                    }

                    startsWith("//", index) -> {
                        indexOf('\n', index).let { end -> if (end < 0) length else end }
                    }

                    startsWith("/*", index) -> {
                        var depth = 0
                        var end = index
                        while (end < length) {
                            when {
                                startsWith("/*", end) -> {
                                    depth++
                                    end += 2
                                }

                                startsWith("*/", end) -> {
                                    depth--
                                    end += 2
                                    if (depth == 0) break
                                }

                                else -> {
                                    end++
                                }
                            }
                        }
                        end
                    }

                    else -> {
                        code.append(this[index])
                        index + 1
                    }
                }
        }
        return code.toString()
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

        val markerCall = Regex("""(?<![\w.])(?<!fun )ProMarker\(""")

        const val RAW_QUOTE = "\"\"\""
    }
}

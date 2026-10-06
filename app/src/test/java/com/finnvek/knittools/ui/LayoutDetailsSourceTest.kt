package com.finnvek.knittools.ui

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Asettelun yksityiskohdat, jotka korjattiin kuvakaappauskatselmoinnissa. */
class LayoutDetailsSourceTest {
    @Test
    fun `projects title is the folder selector like the Insights range title`() {
        val list = ProjectSourceFiles.read("$SCREENS/project/ProjectListScreen.kt")
        val folders = ProjectSourceFiles.read("$SCREENS/project/ProjectFolderComponents.kt")
        val insights = ProjectSourceFiles.read("$SCREENS/insights/InsightsScreen.kt")

        assertTrue(list.contains("folderTitle = {"))
        assertTrue(list.contains("folderTitle()"))
        assertFalse(list.contains("R.string.project_list_title"))
        assertTrue(folders.contains("ScreenTitleSelector("))
        assertTrue(insights.contains("ScreenTitleSelector("))
        // Fokus palautetaan valitsimeen vasta kun se on taas näkyvissä.
        assertTrue(list.contains("!showFoldersSheet && !isMultiSelectMode"))
    }

    @Test
    fun `segmented toggles span the full width and info tips sit next to labels`() {
        val toggle = ProjectSourceFiles.read("$COMPONENTS/SegmentedToggle.kt")
        val increase = ProjectSourceFiles.read("$SCREENS/increase/IncreaseDecreaseScreen.kt")
        val castOn = ProjectSourceFiles.read("$SCREENS/caston/CastOnScreen.kt")

        assertFalse(toggle.contains("fraction"))
        // Info nimikkeen vieressä: kontrollin päässä se kavensi valitsinta ja kenttiä.
        assertTrue(increase.contains("LabelWithInfo("))
        assertFalse(increase.contains("InfoTip("))
        assertFalse(castOn.contains("InfoTip("))
        assertEquals(2, Regex("""info =\s+InfoTipText\(""").findAll(castOn).count())
    }

    @Test
    fun `calculator inputs always sit on a shared tool card`() {
        listOf(
            "increase/IncreaseDecreaseScreen.kt",
            "caston/CastOnScreen.kt",
            "yarn/YarnEstimatorScreen.kt",
            "gauge/GaugeScreen.kt",
        ).forEach { file ->
            val source = ProjectSourceFiles.read("$SCREENS/$file")
            assertTrue(file, source.contains("ToolInputCard"))
            assertFalse(file, source.contains("RoundedCornerShape(18.dp)"))
        }
        assertFalse(ProjectSourceFiles.read("$SCREENS/gauge/GaugeComponents.kt").contains("fun GaugeHeading("))
    }

    @Test
    fun `dialog cancel actions are neutral so the confirm action leads`() {
        val offenders =
            Regex("""TextButton\([^)]*\)\s*\{\s*Text\(stringResource\(R\.string\.cancel\)\)""")
        listOf(
            "$COMPONENTS/ConfirmationDialog.kt",
            "$COMPONENTS/FormSheet.kt",
            "$SCREENS/counter/CounterScreen.kt",
        ).forEach { file ->
            val source = ProjectSourceFiles.read(file)
            assertTrue(file, source.contains("CancelButton("))
            assertFalse(file, offenders.containsMatchIn(source))
        }
    }

    @Test
    fun `calculator shows example values and where the result will appear`() {
        val increase = ProjectSourceFiles.read("$SCREENS/increase/IncreaseDecreaseScreen.kt")
        val field = ProjectSourceFiles.read("$COMPONENTS/NumberInputField.kt")

        assertTrue(field.contains("val placeholder: String? = null,"))
        assertTrue(field.contains("MaterialTheme.knitToolsColors.fieldPlaceholderText"))
        assertTrue(increase.contains("placeholder = EXAMPLE_CURRENT_STITCHES,"))
        assertTrue(increase.contains("placeholder = EXAMPLE_CHANGE_BY,"))
        assertTrue(increase.contains("ResultPlaceholder(text = stringResource(mode.resultPlaceholderRes()))"))
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)
            assertTrue(file.toString(), text.contains("""name="increase_result_placeholder""""))
            assertTrue(file.toString(), text.contains("""name="decrease_result_placeholder""""))
        }
    }

    @Test
    fun `add actions use the shared 3D plus pill everywhere`() {
        listOf(
            "library/SavedPatternsScreen.kt",
            "library/MyYarnScreen.kt",
            "counter/PhotoGalleryScreen.kt",
        ).forEach { file ->
            val source = ProjectSourceFiles.read("$SCREENS/$file")
            assertTrue(file, source.contains("LabeledCounterImageButton("))
            assertFalse(file, source.contains("FloatingActionButton("))
        }
    }

    @Test
    fun `selections, headings and settings follow the shared components`() {
        val dialog = ProjectSourceFiles.read("$COMPONENTS/ProjectDetailsSheet.kt")
        val chartSymbols = ProjectSourceFiles.read("$SCREENS/chartsymbols/ChartSymbolScreen.kt")
        val settings = ProjectSourceFiles.read("$SCREENS/settings/SettingsScreen.kt")

        assertFalse(dialog.contains("FilterChip("))
        assertEquals(2, Regex("""SegmentedToggle\(""").findAll(dialog).count())
        assertTrue(chartSymbols.contains("SectionLabel("))
        assertFalse(chartSymbols.contains("typography.titleMedium"))
        assertTrue(settings.contains("R.string.settings_section_general"))
        assertTrue(settings.contains("private fun ThemeSelector("))
        assertFalse(settings.contains("RadioButton(selected"))
    }

    @Test
    fun `every calculator shows where its result will appear`() {
        mapOf(
            "caston/CastOnScreen.kt" to "R.string.cast_on_result_placeholder",
            "yarn/YarnEstimatorScreen.kt" to "R.string.yarn_result_placeholder",
            "gauge/GaugeComponents.kt" to "R.string.tool_result_placeholder",
        ).forEach { (file, placeholder) ->
            val source = ProjectSourceFiles.read("$SCREENS/$file")
            assertTrue(file, source.contains("ResultPlaceholder(text = stringResource($placeholder)"))
        }
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)
            listOf(
                "cast_on_result_placeholder",
                "yarn_result_placeholder",
                "tool_result_placeholder",
                "settings_theme",
            ).forEach {
                assertTrue("$file $it", text.contains("""name="$it""""))
            }
        }
    }

    @Test
    fun `every add and edit form uses the shared form sheet`() {
        listOf(
            "$COMPONENTS/ProjectDetailsSheet.kt",
            "$SCREENS/library/MyYarnScreen.kt",
            "$SCREENS/library/WebPatternEditorScreen.kt",
        ).forEach { file ->
            val source = ProjectSourceFiles.read(file)
            assertTrue(file, source.contains("FormSheet("))
            assertFalse(file, source.contains("OutlinedTextField("))
            assertFalse(file, source.contains("ScrollableFormDialog("))
        }
    }

    @Test
    fun `reference lists and session history use hairline rows instead of cards`() {
        val abbreviations = ProjectSourceFiles.read("$SCREENS/abbreviations/AbbreviationsScreen.kt")
        val session = ProjectSourceFiles.read("$COMPONENTS/SessionItem.kt")

        assertFalse(abbreviations.contains("Card("))
        assertTrue(abbreviations.contains("HorizontalDivider("))
        assertFalse(session.contains("Surface("))
        // Poisto rivin valikossa, ei punaisena roskakorina jokaisella rivillä.
        assertFalse(session.contains("colorScheme.error"))
        assertTrue(session.contains("private fun SessionItemMenu("))
    }

    @Test
    fun `pattern sheets share the sheet title and keep Pro actions from looking primary`() {
        val picker = ProjectSourceFiles.read("$SCREENS/pattern/PatternPickerSheet.kt")
        val viewer = ProjectSourceFiles.read("$SCREENS/pattern/PatternViewerScreen.kt")
        val card = ProjectSourceFiles.read("$SCREENS/ravelry/PatternCard.kt")

        assertTrue(picker.contains("SheetTitle("))
        assertTrue(picker.contains("SectionLabel(text = stringResource(R.string.pattern_picker_saved_patterns))"))
        assertFalse(picker.contains("        Button("))
        assertTrue(viewer.contains("SheetTitle(text = stringResource(R.string.project_documents_title)"))
        assertTrue(viewer.contains("OverviewEmptyText(R.string.project_documents_empty)"))
        assertTrue(card.contains("if (availability != PatternAvailability.Unknown) {"))
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            assertTrue(file.toString(), ProjectSourceFiles.read(file).contains("""name="web_pattern_edit_title""""))
        }
    }

    private companion object {
        const val SCREENS = "app/src/main/java/com/finnvek/knittools/ui/screens"
        const val COMPONENTS = "app/src/main/java/com/finnvek/knittools/ui/components"
    }
}

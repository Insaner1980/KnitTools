package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CounterWorkspaceSourceTest {
    @Test
    fun `counter content is implemented as one workspace lazy column`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspaceFile = ProjectSourceFiles.file(COUNTER_WORKSPACE_SECTIONS)

        assertTrue("CounterWorkspaceSections.kt is missing", Files.exists(workspaceFile))

        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        assertTrue(counterScreen.contains("CounterWorkspace("))
        assertFalse(counterScreen.contains("private fun CounterScreenContent("))
        assertTrue(workspace.contains("LazyColumn("))
        assertTrue(workspace.contains("contentPadding ="))
        assertTrue(workspace.contains("PaddingValues("))
        assertTrue(workspace.contains(".padding(scaffoldPadding)"))
        assertFalse(workspace.contains("""key = "project-header""""))
        assertTrue(workspace.contains("""key = "counter-hero""""))
        assertTrue(workspace.contains("modifier = Modifier.fillParentMaxHeight()"))
        assertTrue(workspace.indexOf("""key = "counter-hero"""") < workspace.indexOf("ProjectContentCards("))
        assertTrue(workspace.indexOf("ProjectContentCards(") < workspace.indexOf("""key = "extra-counters-title""""))
        assertTrue(workspace.contains("verticalArrangement = Arrangement.spacedBy"))
        assertTrue(workspace.contains("key = { counter -> counter.id }"))
    }

    @Test
    fun `top bar only keeps navigation and more while daily project actions move into content cards`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val contentCardsFile = ProjectSourceFiles.file(COUNTER_PROJECT_CONTENT_CARDS)

        assertTrue("CounterProjectContentCards.kt is missing", Files.exists(contentCardsFile))

        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertFalse(counterScreen.contains("canUseProgressPhotos ="))
        assertFalse(counterScreen.contains("showPatternIcon ="))
        assertFalse(counterScreen.contains("Icons.Filled.CameraAlt"))
        assertFalse(counterScreen.contains("Icons.Filled.Description"))
        assertFalse(workspace.contains("CounterQuickActions("))
        assertFalse(workspace.contains("ProjectInfoSection("))
        assertTrue(workspace.contains("ProjectContentCards("))

        listOf(
            "project_content_add_pattern",
            "project_content_yarn",
            "project_content_notes",
            "project_content_photos",
            "reminders",
        ).forEach { key ->
            assertTrue("Project content string missing: $key", strings.contains("""<string name="$key">"""))
            assertTrue("Project content source does not reference: $key", contentCards.contains("R.string.$key"))
        }
        assertTrue(contentCards.contains("R.string.saved_pattern_detail_open_pattern"))
        assertFalse(contentCards.contains("R.string.project_content_open_pattern"))
        assertFalse(contentCards.contains("R.string.project_content_attach_pattern"))
    }

    @Test
    fun `pattern content card title follows attachment state`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertTrue(workspace.contains("hasPattern = state.patternUri != null || state.linkedPattern != null"))
        assertTrue(contentCards.contains("hasPattern: Boolean"))
        assertTrue(contentCards.contains("titleRes = patternContentTitleRes(hasPattern)"))
        assertTrue(
            contentCards.contains(
                "if (hasPattern) R.string.saved_pattern_detail_open_pattern else R.string.project_content_add_pattern",
            ),
        )
        assertTrue(strings.contains("""<string name="project_content_add_pattern">Add Pattern</string>"""))
        assertFalse(contentCards.contains("R.string.project_content_attach_pattern"))
    }

    @Test
    fun `counter header keeps pattern details out of the first viewport`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        assertTrue(counterScreen.contains("TopAppBar("))
        assertTrue(counterScreen.contains("state.projectName.ifEmpty"))
        assertFalse(workspace.contains("project_content_title"))
        assertFalse(workspace.contains("PatternHeaderRow"))
        assertFalse(workspace.contains("project_header_pattern_attached"))
        assertFalse(workspace.contains("text = attachedPatternName,"))
        assertFalse(contentCards.contains("two_sleeves_one_promise.pdf"))
        assertFalse(contentCards.contains("Ravelry"))
        assertFalse(contentCards.contains(" · "))
    }

    @Test
    fun `counter hero keeps project hidden and places undo below primary controls`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val source = counterSourceFiles(workspace)

        assertHeroRevealOrdering(workspace)
        assertHeroControlStructure(source)
        assertHeroClickAndVisualSemantics(workspace, source)
        assertHeroDimensions(dimens)
        assertFalse(workspace.contains("CounterUndoButton("))
        assertFalse(workspace.contains("Icons.AutoMirrored.Filled.Undo"))
    }

    @Test
    fun `counter hero number measures available width before rolling digits`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        assertTrue(workspace.contains("import androidx.compose.foundation.layout.BoxWithConstraints"))
        assertTrue(workspace.contains("import androidx.compose.ui.text.rememberTextMeasurer"))
        assertTrue(workspace.contains("val countText = state.counter.count.toString()"))
        assertTrue(workspace.contains("BoxWithConstraints("))
        assertTrue(workspace.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(workspace.contains("contentAlignment = Alignment.Center"))
        assertTrue(workspace.contains("val textMeasurer = rememberTextMeasurer()"))
        assertTrue(workspace.contains("counterMainNumberFittedFontSize("))
        assertTrue(workspace.contains("maxWidthPx = constraints.maxWidth"))
        assertTrue(workspace.contains("measureWidth = { candidateFontSize ->"))
        assertTrue(workspace.contains("baseTextStyle.copy(fontSize = candidateFontSize)"))
        assertTrue(workspace.contains("fontSize = fittedFontSize"))
        assertTrue(workspace.contains("if (measureWidth(maxFontSize) <= maxWidthPx)"))
        assertTrue(workspace.contains("repeat(CounterDimens.CounterMainNumberFitIterations)"))
        assertTrue(workspace.contains("if (measureWidth(candidateFontSize) <= maxWidthPx)"))
        assertTrue(dimens.contains("CounterMainNumberMinimumFontSize = 48.sp"))
        assertTrue(dimens.contains("CounterMainNumberFitIterations = 8"))
    }

    @Test
    fun `counter hero uses provided undo image below optically adjusted plus and minus buttons`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val source = counterSourceFiles(workspace)

        assertTrue(Files.exists(ProjectSourceFiles.file(COUNTER_UNDO_BUTTON_ASSET)))
        assertTrue(source.workspace.contains("onUndo: () -> Unit"))
        assertTrue(source.workspace.contains("imageRes = R.drawable.counter_undo_button"))
        assertTrue(
            source.workspace.contains(
                "contentDescription = stringResource(R.string.counter_undo_last_change)",
            ),
        )
        assertTrue(source.workspace.contains("visualSize = CounterDimens.CounterUndoVisualSize"))
        assertTrue(source.workspace.contains(".size(CounterDimens.CounterUndoTouchSize)"))
        assertTrue(source.workspace.contains(".align(Alignment.BottomCenter)"))
        assertTrue(
            source.workspace.indexOf("imageRes = R.drawable.counter_minus_button") <
                source.workspace.indexOf("imageRes = R.drawable.counter_undo_button"),
        )
        assertTrue(
            source.workspace.indexOf("imageRes = R.drawable.counter_plus_button") <
                source.workspace.indexOf("imageRes = R.drawable.counter_undo_button"),
        )
        assertTrue(source.workspace.contains("visualSize = CounterDimens.CounterMinusVisualSize"))
        assertTrue(source.workspace.contains("visualOffsetY = CounterDimens.CounterMinusOpticalOffsetY"))
        assertTrue(source.workspace.contains("visualSize = CounterDimens.CounterPrimaryVisualSize"))
        assertTrue(dimens.contains("CounterMinusVisualSize = 123.dp"))
        assertTrue(dimens.contains("CounterMinusOpticalOffsetY = 1.dp"))
        assertTrue(dimens.contains("CounterUndoVisualSize = 92.dp"))
        assertTrue(dimens.contains("CounterUndoTouchSize = CounterUndoVisualSize"))
        assertTrue(dimens.contains("CounterUndoVerticalSpacing = 16.dp"))
        assertTrue(dimens.contains("CounterControlsHeight ="))
        assertTrue(dimens.contains("CounterPrimaryTouchSize + CounterUndoVerticalSpacing + CounterUndoTouchSize"))
    }

    @Test
    fun `counter hero omits target progress line and lifts primary buttons`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        assertFalse(workspace.contains("CounterTargetProgressBar("))
        assertTrue(workspace.contains("CounterTargetHelperLabel("))
        assertFalse(workspace.contains("CounterProgressHeight"))
        assertFalse(workspace.contains("CounterProgressCornerRadius"))
        assertTrue(dimens.contains("HeroButtonSpacing = 64.dp"))
        assertTrue(dimens.contains("HeroButtonCompactSpacing = 8.dp"))
    }

    @Test
    fun `counter card model does not expose preview fields or nested lazy grids`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)

        listOf(
            "bodyText",
            "bodyRes",
            "photoCount",
            "reminderRow",
            "reminderMessage",
            "project_content_open_pattern",
            "project_content_attach_pattern",
            "project_content_next_reminder",
            "project_content_reminder_body",
            "KeyboardArrowRight",
            "LazyVerticalGrid",
        ).forEach { forbidden ->
            assertFalse("Counter project cards should not contain $forbidden", contentCards.contains(forbidden))
        }
        assertTrue(contentCards.contains("aspectRatio(1f)"))
        assertTrue(contentCards.contains("take(4).chunked(2)"))
        assertFalse(contentCards.contains("if (rowCards.size == 1)"))
        assertTrue(contentCards.contains("Arrangement.Center"))
        assertTrue(contentCards.contains("onClickLabel = title"))
        assertTrue(contentCards.contains("role = Role.Button"))
        assertFalse(workspace.contains("""key = "counter-buttons""""))
        assertTrue(
            workspace.indexOf("ProjectContentCards(") <
                workspace.indexOf("""key = "extra-counters-title""""),
        )
        assertFalse(workspace.contains("""key = "stitch-tracker""""))
    }

    @Test
    fun `pattern card opens pdf viewer or saved pattern detail without preview UI`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(workspace.contains("state.patternUri != null -> onOpenPattern()"))
        assertTrue(workspace.contains("state.linkedPattern != null -> onOpenSavedPatternDetail()"))
        assertFalse(workspace.contains("onShowPatternInfo"))
        assertTrue(counterScreen.contains("onSavedPatternDetail: (Long) -> Unit = {}"))
        assertTrue(
            counterScreen.contains(
                "onOpenSavedPatternDetail = { " +
                    "state.linkedPattern?.id?.let(onSavedPatternDetail) }",
            ),
        )
        assertFalse(counterScreen.contains("PatternInfoSheet("))
        assertTrue(navGraph.contains("onSavedPatternDetail = { savedPatternId ->"))
        assertTrue(navGraph.contains("navController.navigateToTopLevel(TopLevelDestination.Library)"))
        assertTrue(
            navGraph.contains(
                "navController.navigateSingleTopTo(" +
                    "Screen.SavedPatternDetail(savedPatternId).route)",
            ),
        )
        assertFalse(contentCards.contains("patternName"))
        assertFalse(contentCards.contains("linkedPattern"))
    }

    @Test
    fun `counter route keeps bottom navigation visible`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        val hiddenRoutesBlock =
            navGraph
                .substringAfter("private val HIDE_BOTTOM_BAR_ROUTES =")
                .substringBefore(")")
        assertFalse(hiddenRoutesBlock.contains("Screen.Counter.route"))
    }

    @Test
    fun `localized strings no longer carry counter preview card copy`() {
        STRING_FILES.forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)
            assertTrue(
                "Add-pattern card label missing in $stringsFile",
                strings.contains("""<string name="project_content_add_pattern">"""),
            )
            assertFalse(strings.contains("""<string name="project_content_pattern">"""))
            assertFalse(strings.contains("""<string name="project_content_open_pattern">"""))
            assertFalse(strings.contains("""<string name="project_content_attach_pattern">"""))
            assertFalse(strings.contains("""<string name="project_content_attach_pattern_body">"""))
            assertFalse(strings.contains("""<string name="project_content_add_yarn_body">"""))
            assertFalse(strings.contains("""<string name="project_content_add_note_body">"""))
            assertFalse(strings.contains("""<string name="project_content_add_photo_body">"""))
            assertFalse(strings.contains("""<string name="project_content_next_reminder">"""))
            assertFalse(strings.contains("""<string name="project_content_reminder_body">"""))
        }
    }

    private fun counterSourceFiles(workspace: String): CounterSourceFiles =
        CounterSourceFiles(
            workspace = workspace,
            imageButton = ProjectSourceFiles.read(COUNTER_IMAGE_BUTTON),
        )

    private fun assertHeroRevealOrdering(workspace: String) {
        assertTrue(workspace.contains("""key = "counter-hero-reveal-gap""""))
        assertTrue(
            workspace.indexOf("""key = "counter-hero"""") <
                workspace.indexOf("""key = "counter-hero-reveal-gap""""),
        )
        assertTrue(
            workspace.indexOf("""key = "counter-hero-reveal-gap"""") <
                workspace.indexOf("ProjectContentCards("),
        )
    }

    private fun assertHeroControlStructure(source: CounterSourceFiles) {
        assertTrue(source.workspace.contains("CounterRepeatToRowSpacing"))
        assertTrue(source.workspace.indexOf("CompactPatternRepeatRow(") < source.workspace.indexOf("CounterRowLabel("))
        assertTrue(source.workspace.contains("onUndo = actions.onUndo"))
        assertTrue(source.workspace.contains("onUndo: () -> Unit"))
        assertFalse(source.workspace.contains("CounterUndoButton("))
        assertTrue(source.workspace.contains("onClick = onUndo"))
        assertTrue(source.workspace.contains("CounterControlsMaxWidth"))
        assertTrue(source.workspace.contains("CounterControlsHeight"))
        assertTrue(source.workspace.contains(".height(CounterDimens.CounterControlsHeight)"))
        assertTrue(source.workspace.contains(".align(Alignment.TopStart)"))
        assertTrue(source.workspace.contains(".align(Alignment.TopEnd)"))
        assertTrue(
            source.workspace.indexOf(".fillMaxWidth()") <
                source.workspace.indexOf(".widthIn(max = CounterDimens.CounterControlsMaxWidth)"),
        )
    }

    private fun assertHeroClickAndVisualSemantics(
        workspace: String,
        source: CounterSourceFiles,
    ) {
        assertTrue(workspace.contains("counterClickWithoutIndication("))
        assertTrue(source.workspace.contains(".counterClickWithoutIndication(actions.onSurfaceIncrement)"))
        assertFalse(source.workspace.contains(".clickable(onClick = actions.onSurfaceIncrement)"))
        assertFalse(source.workspace.contains(".clickable(onClick = onDecrement)"))
        assertFalse(source.workspace.contains(".clickable(onClick = onIncrement)"))
        assertFalse(source.workspace.contains(".clickable(onClick = onUndo)"))
        assertTrue(source.workspace.contains("CounterImageButton("))
        assertTrue(Regex("CounterImageButton\\(").findAll(source.workspace).count() == 3)
        assertTrue(source.workspace.contains("imageRes = R.drawable.counter_minus_button"))
        assertTrue(source.workspace.contains("imageRes = R.drawable.counter_plus_button"))
        assertTrue(source.workspace.contains("imageRes = R.drawable.counter_undo_button"))
        assertFalse(source.workspace.contains("CounterCraftButton("))
        assertFalse(source.workspace.contains("CounterCraftButtonSymbol"))
        assertFalse(source.workspace.contains("CounterCraftButtonTone"))
        assertFalse(source.workspace.contains("minusContentColor"))
        assertFalse(source.workspace.contains("LightCounterMinusIcon"))
        listOf(
            "collectIsPressedAsState()",
            "enabled: Boolean = true",
            "Role.Button",
            "indication = null",
            "painterResource(id = imageRes)",
            "contentDescription = contentDescription",
        ).forEach { required -> assertTrue(source.imageButton.contains(required)) }
        assertTrue(
            source.workspace
                .contains(".counterClickWithoutIndication(onClick = onClick, enabled = enabled)"),
        )
        assertFalse(source.workspace.contains(".clickable(onClick = onClick)"))
        assertTrue(workspace.contains("indication = null"))
        listOf(
            "CroppedWood",
            "R.drawable.plus_button",
            "R.drawable.minus_button",
            "ImageBitmap.imageResource",
            "drawImage(",
        ).forEach { forbidden -> assertFalse(workspace.contains(forbidden)) }
    }

    private fun assertHeroDimensions(dimens: String) {
        assertTrue(dimens.contains("CounterProjectRevealGap = 48.dp"))
        assertTrue(dimens.contains("HeroButtonSpacing = 64.dp"))
        assertTrue(dimens.contains("CounterControlsMaxWidth = 360.dp"))
        assertTrue(dimens.contains("CounterControlsHeight ="))
        assertTrue(dimens.contains("CounterPrimaryTouchSize + CounterUndoVerticalSpacing + CounterUndoTouchSize"))
        assertTrue(dimens.contains("CounterPrimaryTouchSize = 144.dp"))
        assertTrue(dimens.contains("CounterPrimaryVisualSize = 125.dp"))
        assertFalse(dimens.contains("CounterPrimaryIconSize"))
        assertFalse(dimens.contains("CounterPrimaryShadowElevation"))
        assertFalse(dimens.contains("CounterActionButton"))
        assertTrue(dimens.contains("CounterMinusTouchSize = 144.dp"))
        assertTrue(dimens.contains("CounterMinusVisualSize = 123.dp"))
        assertTrue(dimens.contains("CounterMinusOpticalOffsetY = 1.dp"))
        assertFalse(dimens.contains("CounterMinusIconSize"))
        assertFalse(dimens.contains("CounterMinusShadowElevation"))
        assertTrue(dimens.contains("CounterUndoTouchSize = CounterUndoVisualSize"))
        assertTrue(dimens.contains("CounterUndoVisualSize = 92.dp"))
        assertTrue(dimens.contains("CounterControlsToStitchTrackerSpacing = 72.dp"))
    }

    private data class CounterSourceFiles(
        val workspace: String,
        val imageButton: String,
    )

    private companion object {
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val COUNTER_PROJECT_CONTENT_CARDS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterProjectContentCards.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val COUNTER_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt"
        private const val COUNTER_IMAGE_BUTTON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CounterImageButton.kt"
        private const val COUNTER_UNDO_BUTTON_ASSET =
            "app/src/main/res/drawable-nodpi/counter_undo_button.webp"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
        private val STRING_FILES =
            listOf(
                STRINGS,
                "app/src/main/res/values-da/strings.xml",
                "app/src/main/res/values-de/strings.xml",
                "app/src/main/res/values-es/strings.xml",
                "app/src/main/res/values-fi/strings.xml",
                "app/src/main/res/values-fr/strings.xml",
                "app/src/main/res/values-it/strings.xml",
                "app/src/main/res/values-nb/strings.xml",
                "app/src/main/res/values-nl/strings.xml",
                "app/src/main/res/values-pt/strings.xml",
                "app/src/main/res/values-sv/strings.xml",
            )
    }
}

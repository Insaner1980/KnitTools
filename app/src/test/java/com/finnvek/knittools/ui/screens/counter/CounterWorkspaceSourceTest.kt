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
            "project_content_pattern",
            "project_content_yarn",
            "project_content_notes",
            "project_content_photos",
            "reminders",
        ).forEach { key ->
            assertTrue("Project content string missing: $key", strings.contains("""<string name="$key">"""))
            assertTrue("Project content source does not reference: $key", contentCards.contains("R.string.$key"))
        }
        assertFalse(contentCards.contains("R.string.project_content_open_pattern"))
        assertFalse(contentCards.contains("R.string.project_content_attach_pattern"))
    }

    @Test
    fun `counter header keeps pattern details out of the first viewport`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val contentCards = ProjectSourceFiles.read(COUNTER_PROJECT_CONTENT_CARDS)
        val hero = workspace.blockBetween("private fun CounterHero", "@Composable\nprivate fun CounterStitchTracker")

        assertTrue(counterScreen.contains("TopAppBar("))
        assertTrue(counterScreen.contains("state.projectName.ifEmpty"))
        assertFalse(hero.contains("ReminderAlertCard("))
        assertFalse(hero.contains("project_content_title"))
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
        val heroSource = heroSourceBlocks(workspace)

        assertHeroRevealOrdering(workspace)
        assertHeroControlStructure(heroSource)
        assertHeroClickAndVisualSemantics(workspace, heroSource)
        assertHeroDimensions(dimens)
        assertFalse(workspace.contains("CounterUndoButton("))
        assertFalse(workspace.contains("Icons.AutoMirrored.Filled.Undo"))
    }

    @Test
    fun `counter hero uses provided undo image below optically adjusted plus and minus buttons`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val heroSource = heroSourceBlocks(workspace)

        assertTrue(Files.exists(ProjectSourceFiles.file(COUNTER_UNDO_BUTTON_ASSET)))
        assertTrue(heroSource.controls.contains("onUndo: () -> Unit"))
        assertTrue(heroSource.controls.contains("imageRes = R.drawable.counter_undo_button"))
        assertTrue(heroSource.controls.contains("contentDescription = stringResource(R.string.counter_undo_last_change)"))
        assertTrue(heroSource.controls.contains("visualSize = CounterDimens.CounterUndoVisualSize"))
        assertTrue(heroSource.controls.contains(".size(CounterDimens.CounterUndoTouchSize)"))
        assertTrue(heroSource.controls.contains(".align(Alignment.BottomCenter)"))
        assertTrue(
            heroSource.controls.indexOf("imageRes = R.drawable.counter_minus_button") <
                heroSource.controls.indexOf("imageRes = R.drawable.counter_undo_button"),
        )
        assertTrue(
            heroSource.controls.indexOf("imageRes = R.drawable.counter_plus_button") <
                heroSource.controls.indexOf("imageRes = R.drawable.counter_undo_button"),
        )
        assertTrue(heroSource.controls.contains("visualSize = CounterDimens.CounterMinusVisualSize"))
        assertTrue(heroSource.controls.contains("visualOffsetY = CounterDimens.CounterMinusOpticalOffsetY"))
        assertTrue(heroSource.controls.contains("visualSize = CounterDimens.CounterPrimaryVisualSize"))
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
        val hero = workspace.blockBetween("private fun CounterHero", "@Composable\nprivate fun CounterStitchTracker")

        assertFalse(hero.contains("CounterTargetProgressBar("))
        assertTrue(hero.contains("CounterTargetHelperLabel("))
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
                "Pattern card label missing in $stringsFile",
                strings.contains("""<string name="project_content_pattern">"""),
            )
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

    private fun heroSourceBlocks(workspace: String): HeroSourceBlocks =
        HeroSourceBlocks(
            hero =
                workspace.blockBetween(
                    "private fun CounterHero",
                    "@Composable\nprivate fun CounterStitchTracker",
                ),
            controls =
                workspace.blockBetween(
                    "private fun CounterButtons",
                    "private fun CounterUiState.toMainCounterProject",
                ),
            imageButton = ProjectSourceFiles.read(COUNTER_IMAGE_BUTTON),
            repeatButton =
                workspace.blockBetween(
                    "private fun PatternRepeatButton",
                    "@Composable\nprivate fun WorkspaceSectionTitle",
                ),
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

    private fun assertHeroControlStructure(heroSource: HeroSourceBlocks) {
        assertTrue(heroSource.hero.contains("CounterRepeatToRowSpacing"))
        assertTrue(heroSource.hero.indexOf("CompactPatternRepeatRow(") < heroSource.hero.indexOf("CounterRowLabel("))
        assertTrue(heroSource.hero.contains("onUndo = actions.onUndo"))
        assertTrue(heroSource.controls.contains("onUndo: () -> Unit"))
        assertFalse(heroSource.controls.contains("CounterUndoButton("))
        assertTrue(heroSource.controls.contains("onClick = onUndo"))
        assertTrue(heroSource.controls.contains("CounterControlsMaxWidth"))
        assertTrue(heroSource.controls.contains("CounterControlsHeight"))
        assertTrue(heroSource.controls.contains(".height(CounterDimens.CounterControlsHeight)"))
        assertTrue(heroSource.controls.contains(".align(Alignment.TopStart)"))
        assertTrue(heroSource.controls.contains(".align(Alignment.TopEnd)"))
        assertFalse(heroSource.controls.contains("horizontalArrangement = Arrangement.SpaceBetween"))
        assertTrue(
            heroSource.controls.indexOf(".fillMaxWidth()") <
                heroSource.controls.indexOf(".widthIn(max = CounterDimens.CounterControlsMaxWidth)"),
        )
    }

    private fun assertHeroClickAndVisualSemantics(
        workspace: String,
        heroSource: HeroSourceBlocks,
    ) {
        assertTrue(workspace.contains("counterClickWithoutIndication("))
        assertTrue(heroSource.hero.contains(".counterClickWithoutIndication(actions.onSurfaceIncrement)"))
        assertFalse(heroSource.hero.contains(".clickable(onClick = actions.onSurfaceIncrement)"))
        assertFalse(heroSource.controls.contains(".clickable(onClick = onDecrement)"))
        assertFalse(heroSource.controls.contains(".clickable(onClick = onIncrement)"))
        assertFalse(heroSource.controls.contains(".clickable(onClick = onUndo)"))
        assertTrue(heroSource.controls.contains("CounterImageButton("))
        assertTrue(Regex("CounterImageButton\\(").findAll(heroSource.controls).count() == 3)
        assertTrue(heroSource.controls.contains("imageRes = R.drawable.counter_minus_button"))
        assertTrue(heroSource.controls.contains("imageRes = R.drawable.counter_plus_button"))
        assertTrue(heroSource.controls.contains("imageRes = R.drawable.counter_undo_button"))
        assertFalse(heroSource.controls.contains("CounterCraftButton("))
        assertFalse(heroSource.controls.contains("CounterCraftButtonSymbol"))
        assertFalse(heroSource.controls.contains("CounterCraftButtonTone"))
        assertFalse(heroSource.controls.contains("minusContentColor"))
        assertFalse(heroSource.controls.contains("LightCounterMinusIcon"))
        listOf(
            "collectIsPressedAsState()",
            "enabled: Boolean = true",
            "Role.Button",
            "indication = null",
            "painterResource(id = imageRes)",
            "contentDescription = contentDescription",
        ).forEach { required -> assertTrue(heroSource.imageButton.contains(required)) }
        assertTrue(
            heroSource.repeatButton
                .contains(".counterClickWithoutIndication(onClick = onClick, enabled = enabled)"),
        )
        assertFalse(heroSource.repeatButton.contains(".clickable(onClick = onClick)"))
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

    private data class HeroSourceBlocks(
        val hero: String,
        val controls: String,
        val imageButton: String,
        val repeatButton: String,
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

private fun String.blockBetween(
    start: String,
    end: String,
): String = substringAfter(start).substringBefore(end)

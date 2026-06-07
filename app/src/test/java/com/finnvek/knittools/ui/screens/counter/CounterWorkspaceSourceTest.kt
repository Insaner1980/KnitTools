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
    fun `counter hero keeps project hidden and groups primary controls with undo`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val heroSource = heroSourceBlocks(workspace)

        assertHeroRevealOrdering(workspace)
        assertHeroControlStructure(heroSource)
        assertHeroClickAndVisualSemantics(workspace, heroSource)
        assertHeroDimensions(dimens)
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
                    "@Composable\nprivate fun CounterUndoButton",
                ),
            undoButton =
                workspace.blockBetween(
                    "private fun CounterUndoButton",
                    "@Composable\nprivate fun CounterHeroActionButton",
                ),
            actionButton =
                workspace.blockBetween(
                    "private fun CounterHeroActionButton",
                    "@Composable\nprivate fun CompactPatternRepeatRow",
                ),
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
        assertTrue(heroSource.controls.contains("CounterUndoButton("))
        assertTrue(heroSource.controls.contains("onUndo = onUndo"))
        assertTrue(heroSource.controls.contains("CounterControlsMaxWidth"))
        assertTrue(heroSource.controls.contains("CounterControlsHeight"))
        assertTrue(heroSource.controls.contains(".height(CounterDimens.CounterControlsHeight)"))
        assertTrue(heroSource.controls.contains(".align(Alignment.CenterStart)"))
        assertTrue(heroSource.controls.contains(".align(Alignment.CenterEnd)"))
        assertTrue(heroSource.controls.contains("CounterUndoHorizontalOffset"))
        assertTrue(heroSource.controls.contains("CounterUndoVerticalOffset"))
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
        assertTrue(heroSource.controls.contains(".counterClickWithoutIndication(onDecrement)"))
        assertTrue(heroSource.controls.contains(".counterClickWithoutIndication(onIncrement)"))
        assertFalse(heroSource.controls.contains(".clickable(onClick = onDecrement)"))
        assertFalse(heroSource.controls.contains(".clickable(onClick = onIncrement)"))
        assertTrue(heroSource.undoButton.contains(".counterClickWithoutIndication(onUndo)"))
        assertFalse(heroSource.undoButton.contains(".clickable(onClick = onUndo)"))
        assertTrue(heroSource.controls.contains("CounterHeroActionButton("))
        assertTrue(Regex("CounterHeroActionButton\\(").findAll(heroSource.controls).count() == 2)
        assertTrue(heroSource.controls.contains("icon = Icons.Filled.Remove"))
        assertTrue(heroSource.controls.contains("icon = Icons.Filled.Add"))
        assertTrue(heroSource.controls.contains("minusContentColor"))
        assertTrue(heroSource.controls.contains("LightCounterMinusIcon"))
        assertTrue(heroSource.controls.contains("contentColor = minusContentColor"))
        assertTrue(heroSource.controls.contains("contentColor = MaterialTheme.colorScheme.primary"))
        assertTrue(heroSource.actionButton.contains("MaterialTheme.colorScheme.background.luminance() > 0.5f"))
        assertTrue(heroSource.actionButton.contains("MaterialTheme.colorScheme.surfaceContainerHighest"))
        assertTrue(heroSource.actionButton.contains("MaterialTheme.colorScheme.surfaceVariant"))
        assertTrue(heroSource.actionButton.contains(".background(containerColor)"))
        assertTrue(heroSource.actionButton.contains(".clip(CircleShape)"))
        assertFalse(heroSource.actionButton.contains(".shadow("))
        assertFalse(heroSource.actionButton.contains("elevation: Dp"))
        assertTrue(heroSource.actionButton.contains("contentDescription = contentDescription"))
        assertTrue(heroSource.actionButton.contains("Icon("))
        assertTrue(heroSource.actionButton.contains("tint = contentColor"))
        assertTrue(heroSource.actionButton.contains("contentDescription = null"))
        assertTrue(heroSource.repeatButton.contains(".counterClickWithoutIndication(onClick)"))
        assertFalse(heroSource.repeatButton.contains(".clickable(onClick = onClick)"))
        assertTrue(workspace.contains("indication = null"))
        listOf(
            "CroppedWood",
            "R.drawable.plus_button",
            "R.drawable.minus_button",
            "counter_minus_button",
            "counter_plus_button",
            "ImageBitmap.imageResource",
            "drawImage(",
            "painterResource",
            "drawWithCache",
            "Brush.radialGradient",
        ).forEach { forbidden -> assertFalse(workspace.contains(forbidden)) }
    }

    private fun assertHeroDimensions(dimens: String) {
        assertTrue(dimens.contains("CounterProjectRevealGap = 48.dp"))
        assertTrue(dimens.contains("HeroButtonSpacing = 88.dp"))
        assertTrue(dimens.contains("CounterControlsMaxWidth = 360.dp"))
        assertTrue(dimens.contains("CounterControlsHeight = 160.dp"))
        assertTrue(dimens.contains("CounterPrimaryTouchSize = 144.dp"))
        assertTrue(dimens.contains("CounterPrimaryVisualSize = 125.dp"))
        assertTrue(dimens.contains("CounterPrimaryIconSize = 48.dp"))
        assertFalse(dimens.contains("CounterPrimaryShadowElevation"))
        assertFalse(dimens.contains("CounterActionButton"))
        assertTrue(dimens.contains("CounterMinusTouchSize = 144.dp"))
        assertTrue(dimens.contains("CounterMinusVisualSize = 125.dp"))
        assertTrue(dimens.contains("CounterMinusIconSize = 48.dp"))
        assertFalse(dimens.contains("CounterMinusShadowElevation"))
        assertTrue(dimens.contains("CounterUndoTouchSize = 48.dp"))
        assertTrue(dimens.contains("CounterUndoHorizontalOffset = (-8).dp"))
        assertTrue(dimens.contains("CounterUndoVerticalOffset = 172.dp"))
        assertTrue(dimens.contains("CounterControlsToStitchTrackerSpacing = 72.dp"))
    }

    private data class HeroSourceBlocks(
        val hero: String,
        val controls: String,
        val undoButton: String,
        val actionButton: String,
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

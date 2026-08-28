package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ProjectListWorkspaceSourceTest {
    @Test
    fun `continue knitting model carries only cheap project context`() {
        val viewModel = ProjectSourceFiles.read(PROJECT_LIST_VIEW_MODEL)

        assertTrue(viewModel.contains("val sectionName: String?"))
        assertTrue(viewModel.contains("val targetRows: Int?"))
        assertTrue(viewModel.contains("sectionName = candidate.sectionName"))
        assertTrue(viewModel.contains("targetRows = candidate.targetRows"))
        assertFalse(viewModel.contains("val totalMinutes: Int"))
        assertFalse(viewModel.contains("getTotalMinutesForProject"))
    }

    @Test
    fun `continue knitting hero uses target progress and image action without time or border`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val hero =
            sourceBetween(
                screen,
                "private fun ContinueKnittingCard(",
                "internal fun normalizedContinueKnittingSectionName(",
            )
        val continueAction = sourceFrom(hero, "CounterImageButton(")

        assertTrue(hero.contains("mainCounterTargetStatus(mainCounterDisplay.targetLine)"))
        assertTrue(hero.contains("mainCounterTargetFraction(mainCounterDisplay.targetLine)"))
        assertTrue(hero.contains("if (progressFraction != null)"))
        assertTrue(hero.contains(".height(ProjectListDimens.ProgressTrackHeight)"))
        assertTrue(hero.contains(".fillMaxWidth(progressFraction)"))
        assertTokensInOrder(
            continueAction,
            "imageRes = R.drawable.counter_continue_button",
            "contentDescription =",
            "R.string.project_continue_content_description",
            "state.projectName",
            "visualSize = ProjectListDimens.HeroActionVisualSize",
            "onClick = onClick",
            "modifier =",
            "ProjectListDimens.HeroActionTouchSize",
        )
        assertTrue(
            "counter_continue_button.webp is missing",
            Files.exists(ProjectSourceFiles.file(COUNTER_CONTINUE_BUTTON_ASSET)),
        )
        assertFalse(screen.contains("DurationDisplayFormatter"))
        assertFalse(screen.contains("formatMinutes"))
        assertFalse(screen.contains("R.string.project_metadata_format"))
        assertFalse(screen.contains("BorderStroke"))
        assertFalse(screen.contains("Brush.linearGradient"))
        assertFalse(screen.contains("imageVector = Icons.Filled.PlayArrow"))
    }

    @Test
    fun `new project action uses the physical plus button outside multi select mode`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val screenFunction =
            sourceBetween(screen, "fun ProjectListScreen(", "private fun ProjectListDialogs(")
        val content =
            sourceBetween(
                screen,
                "private fun ProjectListContent(",
                "data class ActiveProjectItemState(",
            )
        val createAction = sourceFrom(screenFunction, "CounterImageButton(")
        val dimens = ProjectSourceFiles.read(PROJECT_LIST_DIMENS)

        assertTrue(normalizeWhitespace(screenFunction).contains("if (!isMultiSelectMode) { CounterImageButton("))
        assertTokensInOrder(
            createAction,
            "imageRes = R.drawable.counter_plus_button",
            "contentDescription = stringResource(R.string.new_project)",
            "visualSize = ProjectListDimens.CreateButtonVisualSize",
            "onClick = {",
            "creationFolderId = (selectedFolderFilter as? ProjectFolderFilter.Folder)?.folderId",
            "creationFolderName = folders.firstOrNull { it.id == creationFolderId }?.name",
            "viewModel.requestProjectCreation()",
            "modifier =",
            ".align(Alignment.BottomEnd)",
            ".padding(16.dp)",
            ".size(ProjectListDimens.CreateButtonTouchSize)",
        )
        assertTokensInOrder(
            content,
            "LazyColumn(",
            "contentPadding =",
            "PaddingValues(",
            "start = ProjectListDimens.ScreenHorizontalPadding",
            "top = ProjectListDimens.ListTopPadding",
            "end = ProjectListDimens.ScreenHorizontalPadding",
            "bottom = ProjectListDimens.ListBottomPadding",
        )
        assertTrue(dimens.contains("val CreateButtonTouchSize = 72.dp"))
        assertTrue(dimens.contains("val ListBottomPadding = 112.dp"))
        assertTrue(
            "counter_plus_button.webp is missing",
            Files.exists(ProjectSourceFiles.file(COUNTER_PLUS_BUTTON_ASSET)),
        )
        assertFalse(screen.contains("FloatingActionButton"))
        assertFalse(screen.contains("ProBadge"))
        assertFalse(screen.contains("viewModel.proState.collectAsStateWithLifecycle()"))
        assertFalse(screen.contains("viewModel.projectCount.collectAsStateWithLifecycle()"))
    }

    @Test
    fun `continue knitting section name is normalized before localized formatting`() {
        assertEquals("Sleeve", normalizedContinueKnittingSectionName(" Sleeve "))
        assertEquals(null, normalizedContinueKnittingSectionName(" "))
    }

    @Test
    fun `project list items open project photos from their photo action`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(screen.contains("onPhotoGallery: (Long) -> Unit = {}"))
        assertTrue(screen.contains("CollectWithLifecycleEffect({ viewModel.navigateToPhotoGallery }) { projectId ->"))
        assertTrue(screen.contains("onPhotoGallery(projectId)"))
        assertTrue(screen.contains("onPhotoGallery = viewModel::openPhotoGallery"))
        assertTrue(screen.contains("onPhotosClick = { actions.onPhotoGallery(project.id) }"))
        assertTrue(navGraph.contains("onPhotoGallery = { projectId ->"))
        assertTrue(navGraph.contains("counterViewModel.selectProjectByIdForLaunch(projectId) { loaded ->"))
        assertTrue(navGraph.contains("if (loaded) {"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.PhotoGallery.route)"))
    }

    @Test
    fun `project list items open only attached local patterns from their pattern action`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(screen.contains("onPatternViewer: (Long) -> Unit = {}"))
        assertTrue(screen.contains("onPatternViewer = onPatternViewer"))
        assertTrue(screen.contains("hasPatternAttachment = project.id in state.projectIdsWithAvailablePrimary"))
        assertTrue(screen.contains("onPatternClick = { actions.onPatternViewer(project.id) }"))
        assertTrue(navGraph.contains("onPatternViewer = { projectId ->"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.PatternViewer(projectId).route)"))
    }

    @Test
    fun `project list items open first linked yarn card from their yarn row`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(screen.contains("onYarnCard: (Long) -> Unit = {}"))
        assertTrue(screen.contains("onYarnCard = onYarnCard"))
        assertTrue(screen.contains("val yarnCardIds: Map<Long, Long>"))
        assertTrue(screen.contains("firstYarnCardId = state.yarnCardIds[project.id]"))
        assertTrue(screen.contains("onYarnClick ="))
        assertTrue(screen.contains("state.firstYarnCardId?.let { yarnCardId ->"))
        assertTrue(screen.contains("{ actions.onYarnCard(yarnCardId) }"))
        assertFalse(screen.contains("parseYarnCardIds(project.yarnCardIds).firstOrNull()"))
        assertTrue(navGraph.contains("onYarnCard = { cardId ->"))
        assertTrue(navGraph.contains("navController.navigateToTopLevel(TopLevelDestination.Library)"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.YarnCardDetail(cardId).route)"))
    }

    @Test
    fun `project list items do not derive a display color from linked yarn ids`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val item = ProjectSourceFiles.read(PROJECT_LIST_ITEM)

        assertFalse(screen.contains("yarnColorSeed"))
        assertFalse(item.contains("YarnColors"))
        assertTrue(item.contains("text = yarnName"))
    }

    @Test
    fun `active and completed rows use cardless list items with dividers only between rows`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertEquals(2, "ProjectListItem\\(".toRegex().findAll(screen).count())
        assertTrue(screen.contains("if (index < visibleActiveProjects.lastIndex)"))
        assertTrue(screen.contains("if (index < state.completed.lastIndex)"))
        assertTrue(screen.contains("HorizontalDivider("))
        assertTrue(screen.contains("thickness = ProjectListDimens.DividerThickness"))
        assertTrue(screen.contains("copy(alpha = ProjectListDimens.DividerAlpha)"))
        assertFalse(screen.contains('\u00b7'))
    }

    @Test
    fun `section counts follow normal multi select hero only empty and completed states`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val content =
            sourceBetween(
                screen,
                "private fun ProjectListContent(",
                "data class ActiveProjectItemState(",
            )
        val sectionLabel =
            sourceBetween(screen, "private fun SectionLabel(", "private fun ProjectListDivider(")
        val visibleActiveFilter = sourceBetween(content, "val visibleActiveProjects =", "LazyColumn(")
        val normalizedContent = normalizeWhitespace(content)

        assertTrue(content.contains("val isHeroVisible = !state.isMultiSelectMode && state.continueKnitting != null"))
        assertTokensInOrder(
            visibleActiveFilter,
            "if (isHeroVisible)",
            "state.active.filterNot",
            "it.id == heroProjectId",
            "else",
            "state.active",
        )
        assertTrue(normalizedContent.contains("if (!state.isMultiSelectMode) { state.continueKnitting?.let"))
        assertTrue(content.contains("if (visibleActiveProjects.isNotEmpty() || !isHeroVisible)"))
        assertTrue(content.contains("count = visibleActiveProjects.size"))
        assertTrue(content.contains("items = visibleActiveProjects"))
        assertTrue(content.contains("if (visibleActiveProjects.isEmpty())"))
        assertTrue(content.contains("text = stringResource(R.string.no_active_projects)"))
        assertTrue(content.contains("if (state.showCompleted)"))
        assertTrue(content.contains("count = state.completed.size"))
        assertTokensInOrder(
            sectionLabel,
            "R.string.project_section_count_format",
            "text.localizedUppercase()",
            "count",
        )
    }

    @Test
    fun `completed rows use loaded data for context final count and completion time`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val content =
            sourceBetween(
                screen,
                "private fun ProjectListContent(",
                "data class ActiveProjectItemState(",
            )

        assertTrue(content.contains("project = project.copy(count = project.totalRows ?: project.count)"))
        assertTrue(content.contains("lastUpdated = project.completedAt ?: project.updatedAt"))
        assertTrue(content.contains("patternName = project.patternName"))
        assertFalse(content.contains("completedYarn"))
        assertFalse(content.contains("completedAttachment"))
    }

    @Test
    fun `default project list count and completed formats match the workspace contract`() {
        val strings = ProjectSourceFiles.read(DEFAULT_STRINGS)

        assertTrue(strings.contains("<string name=\"project_completed_format\">Completed %1\$s</string>"))
        assertTrue(strings.contains("<string name=\"project_section_count_format\">%1\$s %2\$d</string>"))
    }

    private fun sourceBetween(
        source: String,
        start: String,
        end: String,
    ): String {
        val startIndex = source.indexOf(start)
        check(startIndex >= 0) { "Missing source marker: $start" }
        val endIndex = source.indexOf(end, startIndex + start.length)
        check(endIndex > startIndex) { "Missing source marker after $start: $end" }
        return source.substring(startIndex, endIndex)
    }

    private fun sourceFrom(
        source: String,
        start: String,
    ): String {
        val startIndex = source.indexOf(start)
        check(startIndex >= 0) { "Missing source marker: $start" }
        return source.substring(startIndex)
    }

    private fun normalizeWhitespace(source: String): String = source.replace(WHITESPACE, " ").trim()

    private fun assertTokensInOrder(
        source: String,
        vararg tokens: String,
    ) {
        var searchFrom = 0
        tokens.forEach { token ->
            val position = source.indexOf(token, searchFrom)
            assertTrue("Missing or out-of-order source token: $token", position >= 0)
            searchFrom = position + token.length
        }
    }

    private companion object {
        private const val PROJECT_LIST_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
        private const val PROJECT_LIST_ITEM =
            "app/src/main/java/com/finnvek/knittools/ui/components/ProjectListItem.kt"
        private const val PROJECT_LIST_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/ProjectListDimens.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val DEFAULT_STRINGS = "app/src/main/res/values/strings.xml"
        private const val COUNTER_CONTINUE_BUTTON_ASSET =
            "app/src/main/res/drawable-nodpi/counter_continue_button.webp"
        private const val COUNTER_PLUS_BUTTON_ASSET =
            "app/src/main/res/drawable-nodpi/counter_plus_button.webp"
        private val WHITESPACE = "\\s+".toRegex()
    }
}

package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.ui.components.normalizedContinueKnittingSectionName
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
    fun `continue card is shared by the hero and the overview with one physical continue button`() {
        val card = ProjectSourceFiles.read(CONTINUE_CARD)
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val overview = ProjectSourceFiles.read(PROJECT_OVERVIEW_SCREEN)
        val continueAction = sourceFrom(card, "CounterImageButton(")

        assertTrue(card.contains("mainCounterTargetStatus(display.targetLine)"))
        assertTrue(card.contains("mainCounterTargetFraction(display.targetLine)"))
        assertTrue(card.contains("if (progressFraction != null)"))
        assertTrue(card.contains("ProjectProgressBar(progressFraction)"))
        assertTrue(card.contains("MaterialTheme.knitToolsColors.primaryReadable"))
        // Ylärivi seuraa käsityötyyppiä, eikä virkkausprojektille sanota "Continue knitting".
        assertTrue(card.contains("project.craftType == CraftType.CROCHET -> R.string.continue_crocheting"))
        assertTokensInOrder(
            continueAction,
            "imageRes = R.drawable.counter_continue_button",
            "contentDescription = stringResource(R.string.project_continue_content_description, project.name)",
            "visualSize = ProjectListDimens.HeroActionVisualSize",
            "onClick = it",
            "modifier = Modifier.size(ProjectListDimens.HeroActionTouchSize)",
        )
        // Iso lukema on samaa primary-oranssia kuin palkki ja painike.
        assertTrue(card.contains("style = MaterialTheme.typography.projectHeroCount"))
        assertTrue(card.contains("color = MaterialTheme.colorScheme.primary"))
        // Listalla heron runko avaa projektinäkymän ja nappi laskurin; projektinäkymässä molemmat laskurin.
        assertTokensInOrder(
            screen,
            "ContinueProjectCard(",
            "onClick = { actions.onOpenOverview(ck.projectId) }",
            "onClickLabel = stringResource(R.string.project_card_open_overview_action)",
            "onOpenCounter = { actions.onOpenCounter(ck.projectId) }",
        )
        assertTokensInOrder(
            overview,
            "ContinueProjectCard(",
            "onClick = onOpenCounter.takeUnless { state.isCompleted }",
            "onOpenCounter = onOpenCounter.takeUnless { state.isCompleted }",
            "showName = false",
        )
        assertFalse(overview.contains("R.string.project_overview_open_counter"))
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
        val createAction = sourceFrom(screenFunction, "LabeledCounterImageButton(")
        val dimens = ProjectSourceFiles.read(PROJECT_LIST_DIMENS)

        // Plus-napin vieressä on teksti: sama plus tarkoittaa laskurissa rivin lisäämistä.
        assertTrue(
            normalizeWhitespace(screenFunction).contains("if (!isMultiSelectMode) { LabeledCounterImageButton("),
        )
        assertTokensInOrder(
            createAction,
            "imageRes = R.drawable.counter_plus_button",
            "label = stringResource(R.string.new_project)",
            "visualSize = ProjectListDimens.CreateButtonVisualSize",
            "onClick = {",
            "creationFolderId = (selectedFolderFilter as? ProjectFolderFilter.Folder)?.folderId",
            "creationFolderName = folders.firstOrNull { it.id == creationFolderId }?.name",
            "viewModel.requestProjectCreation()",
            "modifier =",
            ".align(Alignment.BottomEnd)",
            ".padding(ProjectListDimens.CreateButtonMargin)",
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
        assertTrue(dimens.contains("val CreateButtonVisualSize = 64.dp"))
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
    fun `cards open overview and only the continue hero opens counter`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val nav = ProjectSourceFiles.read(NAV_GRAPH)
        // ContinueProjectCard ei kuulu laskuun: haetaan vain itsenäinen ProjectCard-kutsu.
        assertEquals(2, "(?<![A-Za-z])ProjectCard\\(".toRegex().findAll(screen).count())
        assertTrue(screen.contains("actions.onOpenOverview(project.id)"))
        assertFalse(screen.contains("actions.onOpenCounter(project.id)"))
        assertTrue(screen.contains("actions.onOpenCounter(ck.projectId)"))
        assertTrue(screen.contains("actions.onOpenOverview(ck.projectId)"))
        assertTrue(screen.contains("onOpenCounter(projectId)"))
        assertTrue(nav.contains("Screen.ProjectOverview(projectId).route"))
        assertTrue(nav.contains("counterViewModel.selectProjectByIdForLaunch(projectId)"))
        assertTrue(screen.contains("ProjectListDimens.CardSpacing"))
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
            sourceBetween(screen, "private fun ProjectSectionLabel(", "private fun DeleteProjectDialog(")
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
        // Aktiivisten määrä sisältää heron projektin, vaikka sille ei piirretä omaa korttia.
        assertTrue(content.contains("count = state.active.size"))
        assertFalse(content.contains("count = visibleActiveProjects.size"))
        assertTrue(content.contains("items = visibleActiveProjects"))
        assertTrue(content.contains("if (visibleActiveProjects.isEmpty())"))
        assertTrue(content.contains("text = stringResource(R.string.no_active_projects)"))
        assertTrue(content.contains("if (state.showCompleted)"))
        assertTrue(content.contains("count = state.completed.size"))
        assertTokensInOrder(
            sectionLabel,
            "SectionLabel(",
            "R.string.project_section_count_format",
            "text",
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
        assertTrue(content.contains("photoUri = state.latestPhotoUris[project.id]"))
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

    // CPD-OFF: Lahdekooditestin rajausapu pidetaan tarkistuksen yhteydessa.
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
    // CPD-ON

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
        private const val CONTINUE_CARD =
            "app/src/main/java/com/finnvek/knittools/ui/components/ContinueProjectCard.kt"
        private const val PROJECT_OVERVIEW_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewScreen.kt"
        private const val PROJECT_LIST_ITEM =
            "app/src/main/java/com/finnvek/knittools/ui/components/ProjectCard.kt"
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

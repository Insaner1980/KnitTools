package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListWorkspaceSourceTest {
    @Test
    fun `continue knitting model carries only cheap project context`() {
        val viewModel = ProjectSourceFiles.read(PROJECT_LIST_VIEW_MODEL)

        assertTrue(viewModel.contains("val sectionName: String?"))
        assertTrue(viewModel.contains("val targetRows: Int?"))
        assertTrue(viewModel.contains("sectionName = candidate.sectionName"))
        assertTrue(viewModel.contains("targetRows = candidate.targetRows"))
    }

    @Test
    fun `continue knitting section name is normalized before localized formatting`() {
        assertEquals("Sleeve", normalizedContinueKnittingSectionName(" Sleeve "))
        assertEquals(null, normalizedContinueKnittingSectionName(" "))
    }

    @Test
    fun `project cards open project photos from their photo indicator`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(screen.contains("onPhotoGallery: (Long) -> Unit = {}"))
        assertTrue(screen.contains("CollectWithLifecycleEffect(viewModel.navigateToPhotoGallery) { projectId ->"))
        assertTrue(screen.contains("onPhotoGallery(projectId)"))
        assertTrue(screen.contains("onPhotoGallery = viewModel::openPhotoGallery"))
        assertTrue(screen.contains("onPhotosClick = { actions.onPhotoGallery(project.id) }"))
        assertTrue(navGraph.contains("onPhotoGallery = { projectId ->"))
        assertTrue(navGraph.contains("counterViewModel.selectProjectByIdForLaunch(projectId) { loaded ->"))
        assertTrue(navGraph.contains("if (loaded) {"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.PhotoGallery.route)"))
    }

    @Test
    fun `project cards open only attached local patterns from their pattern indicator`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(screen.contains("onPatternViewer: (Long) -> Unit = {}"))
        assertTrue(screen.contains("onPatternViewer = onPatternViewer"))
        assertTrue(screen.contains("hasPatternAttachment = !project.patternUri.isNullOrBlank()"))
        assertTrue(screen.contains("onPatternClick = { actions.onPatternViewer(project.id) }"))
        assertTrue(navGraph.contains("onPatternViewer = { projectId ->"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.PatternViewer(projectId).route)"))
    }

    @Test
    fun `project cards open first linked yarn card from their yarn row`() {
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
    fun `project card yarn color uses the linked yarn card id`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertEquals(2, "yarnColorSeed = state.firstYarnCardId".toRegex().findAll(screen).count())
        assertFalse(screen.contains("yarnColorSeed = project.id"))
    }

    private companion object {
        private const val PROJECT_LIST_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
    }
}

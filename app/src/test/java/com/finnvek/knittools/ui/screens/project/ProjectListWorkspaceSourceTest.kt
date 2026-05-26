package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectListWorkspaceSourceTest {
    @Test
    fun `continue knitting model carries only cheap project context`() {
        val viewModel = ProjectSourceFiles.read(PROJECT_LIST_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)

        assertTrue(viewModel.contains("val sectionName: String?"))
        assertTrue(viewModel.contains("val targetRows: Int?"))
        assertTrue(viewModel.contains("sectionName = candidate.sectionName"))
        assertTrue(viewModel.contains("targetRows = candidate.targetRows"))
        assertTrue(screen.contains("private fun continueKnittingContextLine("))
    }

    @Test
    fun `continue knitting context includes section before row progress`() {
        assertEquals(
            "Sleeve · Row 12 / 40",
            continueKnittingContextLineReflective(
                sectionName = "Sleeve",
                rowCount = 12,
                targetRows = 40,
                fallback = "Row 12 / 40",
            ),
        )
        assertEquals(
            "Row 12 / 40",
            continueKnittingContextLineReflective(
                sectionName = " ",
                rowCount = 12,
                targetRows = 40,
                fallback = "Row 12 / 40",
            ),
        )
    }

    @Test
    fun `project cards open project photos from their photo indicator`() {
        val screen = ProjectSourceFiles.read(PROJECT_LIST_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(screen.contains("onPhotoGallery: (Long) -> Unit = {}"))
        assertTrue(screen.contains("onPhotoGallery = onPhotoGallery"))
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

        assertTrue(screen.contains("import com.finnvek.knittools.domain.model.parseYarnCardIds"))
        assertTrue(screen.contains("onYarnCard: (Long) -> Unit = {}"))
        assertTrue(screen.contains("onYarnCard = onYarnCard"))
        assertTrue(screen.contains("val firstYarnCardId = parseYarnCardIds(project.yarnCardIds).firstOrNull()"))
        assertTrue(screen.contains("onYarnClick ="))
        assertTrue(screen.contains("firstYarnCardId?.let { yarnCardId ->"))
        assertTrue(screen.contains("{ actions.onYarnCard(yarnCardId) }"))
        assertTrue(navGraph.contains("onYarnCard = { cardId ->"))
        assertTrue(navGraph.contains("navController.navigateToTopLevel(TopLevelDestination.Library)"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.YarnCardDetail(cardId).route)"))
    }

    private fun continueKnittingContextLineReflective(
        sectionName: String?,
        rowCount: Int,
        targetRows: Int?,
        fallback: String,
    ): String =
        projectListScreenKt()
            .getDeclaredMethod(
                "continueKnittingContextLine",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaObjectType,
                String::class.java,
            ).apply { isAccessible = true }
            .invoke(null, sectionName, rowCount, targetRows, fallback) as String

    private fun projectListScreenKt(): Class<*> =
        Class.forName("com.finnvek.knittools.ui.screens.project.ProjectListScreenKt")

    private companion object {
        private const val PROJECT_LIST_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt"
        private const val PROJECT_LIST_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListScreen.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
    }
}

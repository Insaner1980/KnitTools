package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationRouteSourceTest {
    @Test
    fun `project and library viewers receive route scoped annotation view models`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val projectRoute =
            navGraph.substringAfter("Screen.PatternViewer.ROUTE").substringBefore("Screen.SessionHistory.ROUTE")
        val libraryRoute =
            navGraph
                .substringAfter("private fun NavGraphBuilder.libraryPatternViewerRoute")
                .substringBefore("private fun NavGraphBuilder.libraryRavelryDetailRoute")

        listOf(projectRoute, libraryRoute).forEach { route ->
            assertTrue(route.contains("PatternAnnotationViewModel = hiltViewModel(backStackEntry)"))
            assertTrue(route.contains("annotationViewModel = annotationViewModel"))
        }
    }

    @Test
    fun `both viewers forward page changes to annotation state`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER)
        val projectViewer =
            viewer
                .substringAfter(
                    "fun PatternViewerScreen(",
                ).substringBefore("private fun TrackReadingLineForCurrentRow")
        val libraryViewer =
            viewer
                .substringAfter("fun LibraryPatternViewerScreen(")
                .substringBefore("private fun rememberPatternRenderState")

        listOf(projectViewer, libraryViewer).forEach { route ->
            assertTrue(route.contains("LaunchedEffect(currentPage)"))
            assertTrue(route.contains("annotationViewModel.setCurrentPage(currentPage)"))
        }
    }

    @Test
    fun `project layer panel marks master read only while library shows only master`() {
        val panel = ProjectSourceFiles.read(PATTERN_LAYER_PANEL)
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER)

        assertTrue(panel.contains("val projectViewer = state.owner is PatternAnnotationOwner.Project"))
        assertTrue(panel.contains("readOnly = projectViewer"))
        assertTrue(panel.contains("if (projectViewer)"))
        assertTrue(viewer.contains("PatternAnnotationOverlay("))
        assertTrue(viewer.contains("onMasterLayerVisibilityChange = annotationViewModel::setMasterLayerVisible"))
        assertTrue(viewer.contains("onProjectLayerVisibilityChange = annotationViewModel::setProjectLayerVisible"))
    }

    private companion object {
        const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        const val PATTERN_VIEWER =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
        const val PATTERN_LAYER_PANEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationLayerPanel.kt"
    }
}

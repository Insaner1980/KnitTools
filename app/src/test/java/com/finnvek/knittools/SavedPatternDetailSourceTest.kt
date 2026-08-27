package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SavedPatternDetailSourceTest {
    @Test
    fun `saved pattern detail route loads saved pattern by id`() {
        val screen = ProjectSourceFiles.read(SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val viewModel = ProjectSourceFiles.read(LIBRARY_VIEW_MODEL)

        assertTrue(screen.contains("data class SavedPatternDetail"))
        assertTrue(screen.contains("const val ROUTE = \"saved_pattern_detail/{savedPatternId}\""))
        assertTrue(navGraph.contains("savedPatternDetailRoute(navController)"))
        assertTrue(navGraph.contains("Screen.SavedPatternDetail.ROUTE"))
        assertTrue(navGraph.contains("SavedPatternDetailScreen("))
        assertTrue(navGraph.contains("libraryViewModel.loadSavedPattern(savedPatternId)"))
        assertTrue(navGraph.contains("Screen.LibraryPatternViewer(savedPatternId).route"))
        assertTrue(navGraph.contains("counterViewModel.attachSavedPattern(pattern)"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.Counter.route)"))
        assertTrue(navGraph.contains("libraryViewModel.deleteSavedPattern(savedPatternId)"))
        val detailRoute =
            navGraph
                .substringAfter("private fun NavGraphBuilder.savedPatternDetailRoute")
                .substringBefore("private fun NavGraphBuilder.libraryPatternViewerRoute")
        assertTrue(detailRoute.contains("libraryViewModel.patternDeleteErrorId.collectAsStateWithLifecycle()"))
        assertTrue(detailRoute.contains("deleteErrorId = patternDeleteErrorId"))
        assertTrue(viewModel.contains("fun deleteSavedPattern("))
        assertTrue(viewModel.contains("savedPatternRepository.deleteById(id)"))
    }

    @Test
    fun `saved pattern detail screen exposes metadata availability and actions`() {
        val detailPath = ProjectSourceFiles.file(SAVED_PATTERN_DETAIL_SCREEN)
        val strings = ProjectSourceFiles.read(BASE_STRINGS)

        assertTrue(Files.exists(detailPath))
        val detail = ProjectSourceFiles.read(detailPath)

        assertTrue(detail.contains("fun SavedPatternDetailScreen("))
        assertTrue(detail.contains("RemotePatternImage("))
        assertTrue(detail.contains("pattern.name"))
        assertTrue(detail.contains("pattern.designerName"))
        assertTrue(detail.contains("SavedPatternAvailabilityChip("))
        assertTrue(detail.contains("PatternAvailabilityBadge(availability = pattern.availability)"))
        assertTrue(detail.contains("pattern.hasAttachedPdf"))
        assertTrue(detail.contains("pattern.isAvailableOffline"))
        assertTrue(detail.contains("pattern.requiresRavelryAccess"))
        assertTrue(detail.contains("openRavelryUrl("))
        assertTrue(detail.contains("onOpenPattern"))
        assertTrue(detail.contains("onAttachToProject"))
        assertTrue(detail.contains("onRemove"))
        val detailSignature =
            detail
                .substringAfter("fun SavedPatternDetailScreen(")
                .substringBefore(") {")
        assertTrue(detailSignature.contains("modifier: Modifier = Modifier"))
        assertTrue(
            "modifier must be the first optional parameter",
            detailSignature.indexOf("modifier: Modifier = Modifier") <
                detailSignature.indexOf("deleteErrorId: Long = 0L"),
        )
        assertTrue(detail.contains("deleteErrorId: Long = 0L"))
        assertTrue(detail.contains("SnackbarHostState()"))
        assertTrue(detail.contains("LaunchedEffect(deleteErrorId)"))
        assertTrue(detail.contains("SnackbarHost(hostState = snackbarHostState)"))

        DETAIL_STRINGS.forEach { stringName ->
            assertTrue(strings.contains("name=\"$stringName\""))
        }
    }

    private companion object {
        private val DETAIL_STRINGS =
            listOf(
                "saved_pattern_detail_pdf_attached",
                "saved_pattern_detail_available_offline",
                "saved_pattern_detail_open_on_ravelry",
                "saved_pattern_detail_requires_ravelry",
                "saved_pattern_detail_open_pattern",
                "saved_pattern_detail_attach_to_project",
                "saved_pattern_detail_remove_confirm",
            )
        private const val SCREEN = "app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt"
        private const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val LIBRARY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryViewModel.kt"
        private const val SAVED_PATTERN_DETAIL_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternDetailScreen.kt"
        private const val BASE_STRINGS = "app/src/main/res/values/strings.xml"
    }
}

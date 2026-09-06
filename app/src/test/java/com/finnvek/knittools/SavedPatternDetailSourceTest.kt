package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class SavedPatternDetailSourceTest {
    @Test
    fun `saved pattern deletion messages can scroll without changing other confirmations`() {
        val dialog =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/components/ConfirmationDialog.kt",
            )
        assertTrue(dialog.contains("scrollableMessage: Boolean = false"))
        assertTrue(
            dialog.contains("if (scrollableMessage) Modifier.verticalScroll(rememberScrollState()) else Modifier"),
        )
        listOf(
            SAVED_PATTERN_DETAIL_SCREEN,
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/SavedPatternsScreen.kt",
            "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelrySearchScreen.kt",
        ).forEach { path ->
            assertTrue(
                "$path must allow the complete deletion warning to be read",
                ProjectSourceFiles.read(path).contains("scrollableMessage = true"),
            )
        }
    }

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
    fun `library pattern viewer requires a local pdf uri`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val viewerRoute =
            navGraph
                .substringAfter("private fun NavGraphBuilder.libraryPatternViewerRoute")
                .substringBefore("private fun NavGraphBuilder.libraryMyYarnRoute")

        assertTrue(viewerRoute.contains("pattern?.localPdfUri?.takeIf { it.isNotBlank() }"))
        assertFalse(viewerRoute.contains("pattern.patternUrl"))
        assertTrue(
            viewerRoute.contains(
                "navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)",
            ),
        )
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
        assertTrue(detail.contains("deleteErrorId > lastHandledDeleteErrorId"))
        assertTrue(detail.contains("SnackbarHost(hostState = snackbarHostState)"))

        DETAIL_STRINGS.forEach { stringName ->
            assertTrue(strings.contains("name=\"$stringName\""))
        }
    }

    @Test
    fun `saved pattern deletion warnings cover pdf scope in every supported locale`() {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val locales =
            builder
                .parse(ProjectSourceFiles.file("app/src/main/res/xml/locales_config.xml").toFile())
                .getElementsByTagName("locale")
        val expectedDirectories =
            (0 until locales.length)
                .map { index ->
                    val language = (locales.item(index) as Element).getAttribute("android:name")
                    if (language == "en") "values" else "values-$language"
                }.toSet()
        val files = ProjectSourceFiles.localizedStringFiles()
        assertEquals(expectedDirectories, files.map { it.parent.fileName.toString() }.toSet())
        files.forEach { file ->
            assertDeletionWarnings(file, builder)
        }
    }

    private fun assertDeletionWarnings(
        file: Path,
        builder: DocumentBuilder,
    ) {
        val document = builder.parse(file.toFile())
        val resources = document.documentElement.childNodes
        val messages = mutableListOf<Pair<String, List<String>>>()
        val quantities = mutableSetOf<String>()
        (0 until resources.length).forEach resourceLoop@{ index ->
            val resource = resources.item(index) as? Element ?: return@resourceLoop
            when (resource.getAttribute("name")) {
                "saved_pattern_detail_remove_confirm" -> messages.add(resource.textContent to emptyList())
                "web_pattern_delete_confirm_message" -> messages.add(resource.textContent to listOf("%1\$s"))
                "delete_patterns_confirm" -> {
                    val items = resource.getElementsByTagName("item")
                    (0 until items.length).forEach { itemIndex ->
                        val item = items.item(itemIndex) as Element
                        quantities.add(item.getAttribute("quantity"))
                        messages.add(item.textContent to listOf("%1\$d"))
                    }
                }
            }
        }
        val expectedQuantities =
            if (file.parent.fileName.toString() in setOf("values-es", "values-fr", "values-it", "values-pt")) {
                setOf("one", "many", "other")
            } else {
                setOf("one", "other")
            }
        assertEquals("$file plural forms", expectedQuantities, quantities)
        assertEquals("$file confirmation messages", quantities.size + 2, messages.size)
        val placeholders = Regex("""%(?:\d+\$)?[a-zA-Z]""")
        messages.forEach { (message, expectedPlaceholders) ->
            assertEquals(
                "$file placeholders",
                expectedPlaceholders,
                placeholders.findAll(message).map { it.value }.toList(),
            )
            val paragraphs = message.split("\\n\\n")
            assertEquals("$file must explain deletion before confirmation", 2, paragraphs.size)
            assertTrue(
                "$file must distinguish PDF annotations and attachments",
                Regex("PDF").findAll(paragraphs[1]).count() >= 3,
            )
            if (file.parent.fileName.toString() == "values") {
                assertTrue(message.contains("Any PDF annotations made in the Library"))
                assertTrue(message.contains("will no longer appear in projects"))
                assertTrue(
                    message.contains(
                        "PDFs already attached to projects and PDF annotations made within projects will be kept.",
                    ),
                )
            } else {
                assertFalse("$file must translate the warning", message.contains("Any PDF annotations"))
            }
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

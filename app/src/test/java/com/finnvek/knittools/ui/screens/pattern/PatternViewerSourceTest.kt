package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternViewerSourceTest {
    @Test
    fun `renderer errors are not displayed from raw exception messages`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertFalse(source.contains("rendererError = error.message"))
        assertTrue(source.contains("rendererError = patternOpenFailed"))
    }

    @Test
    fun `project pattern viewer uses persisted reading line state`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("readingLineEnabled = selectedDocument?.readingLineEnabled == true"))
        assertTrue(source.contains("readingLineYFraction = readingLinePreviewYFraction"))
        assertTrue(source.contains("onReadingLineToggle = counterViewModel::setReadingLineEnabled"))
        assertTrue(source.contains("counterViewModel.commitManualReadingLinePosition(sanitizedYFraction)"))
    }

    @Test
    fun `library pattern viewer keeps reading line state session local`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("var readingLineEnabled by rememberSaveable(patternUri)"))
        assertTrue(source.contains("var readingLineYFraction by rememberSaveable(patternUri)"))
        assertTrue(source.contains("readingLineEnabled = readingLineEnabled"))
        assertTrue(source.contains("onReadingLineToggle = { readingLineEnabled = it }"))
    }

    @Test
    fun `library pattern viewer does not receive or write project state`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val libraryViewer =
            source.blockBetween(
                "fun LibraryPatternViewerScreen(",
                "@Composable\nprivate fun rememberPatternRenderState",
            )

        assertFalse(libraryViewer.contains("CounterViewModel"))
        assertFalse(libraryViewer.contains("counterViewModel"))
        assertFalse(libraryViewer.contains("projectId"))
        assertFalse(libraryViewer.contains("PatternAnnotationRepository"))
    }

    @Test
    fun `reading line is toggled from pattern viewer overflow and drawn in transformed pdf layer`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val viewport = ProjectSourceFiles.read(PATTERN_DOCUMENT_VIEWPORT)

        assertTrue(source.contains("R.string.pattern_show_reading_line"))
        assertTrue(source.contains("R.string.pattern_hide_reading_line"))
        assertTrue(viewport.contains(".transformable(state = transformableState)"))
        assertTrue(source.contains("ReadingLineOverlay("))
        assertTrue(source.contains("dragAmount / scale"))
        assertTrue(source.contains("READING_LINE_MIN_Y_FRACTION"))
        assertTrue(source.contains("READING_LINE_MAX_Y_FRACTION"))
    }

    @Test
    fun `reading line drag has separate live update and drag end commit callbacks`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("val onReadingLineYFractionCommit: (Float) -> Unit"))
        assertTrue(source.contains("onYFractionChange = actions.onReadingLineYFractionChange"))
        assertTrue(source.contains("onYFractionCommit = actions.onReadingLineYFractionCommit"))
        assertTrue(source.contains("onDragEnd = { actions.onYFractionCommit(lastYFraction) }"))
        assertTrue(source.contains("onDragCancel = {"))
        assertTrue(source.contains("onVerticalDrag = { change, dragAmount ->"))
        assertTrue(source.contains("dragAmount / scale.coerceAtLeast(1f)"))
    }

    @Test
    fun `reading line label uses current row and localized theme styling`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val documentBlock =
            source.blockBetween(
                "PatternDocumentViewport(",
                "@Composable\nprivate fun ReadingLineOverlay",
            )
        val overlayBlock =
            source.blockBetween(
                "internal fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertTrue(documentBlock.contains("currentRow = state.currentRow"))
        assertTrue(overlayBlock.contains("currentRow: Int?"))
        assertTrue(overlayBlock.contains("currentRow?.let { currentRow ->"))
        assertTrue(overlayBlock.contains("ReadingLineRowLabel("))
        assertTrue(overlayBlock.contains("stringResource(R.string.current_row_short, currentRow)"))
        assertTrue(overlayBlock.contains("MaterialTheme.colorScheme.primaryContainer"))
        assertTrue(overlayBlock.contains("MaterialTheme.colorScheme.onPrimaryContainer"))
    }

    @Test
    fun `reading line label does not hardcode visible row text`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val overlayBlock =
            source.blockBetween(
                "internal fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertFalse(overlayBlock.contains("\"Row "))
        assertFalse(overlayBlock.contains("\"Kerros "))
        assertFalse(overlayBlock.contains("Text(\""))
        assertTrue(overlayBlock.contains("stringResource(R.string.current_row_short, currentRow)"))
    }

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `project reading line drag commit pauses follow without creating a row marker`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )
        val libraryViewer =
            source.blockBetween(
                "fun LibraryPatternViewerScreen(",
                "@Composable\nprivate fun rememberPatternRenderState",
            )

        assertTrue(projectViewer.contains("onReadingLineYFractionCommit = { yFraction ->"))
        // CPD-ON
        assertTrue(projectViewer.contains("val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)"))
        assertTrue(projectViewer.contains("counterViewModel.commitManualReadingLinePosition(sanitizedYFraction)"))
        assertFalse(
            projectViewer
                .blockBetween(
                    "onReadingLineYFractionCommit = { yFraction ->",
                    "onReadingLineDragCancel = {",
                ).contains("upsertPatternRowMarker"),
        )
        assertFalse(libraryViewer.contains("upsertPatternRowMarker"))
    }

    @Test
    fun `project reading line drag live updates preview state without persistence write`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )
        val contentActions =
            projectViewer.blockBetween(
                "actions =\n                    PatternViewerContentActions(",
                "                modifier =\n",
            )

        assertTrue(projectViewer.contains("var readingLinePreviewYFraction by remember(selectedDocument?.id)"))
        assertTrue(projectViewer.contains("var isReadingLineDragging by remember(selectedDocument?.id)"))
        assertTrue(
            projectViewer.contains(
                "LaunchedEffect(selectedDocument?.id, selectedDocument?.readingLineYFraction, isReadingLineDragging)",
            ),
        )
        assertTrue(projectViewer.contains("if (!isReadingLineDragging)"))
        assertTrue(projectViewer.contains("selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION"))
        assertTrue(projectViewer.contains("readingLineYFraction = readingLinePreviewYFraction"))
        assertTrue(projectViewer.contains("onReadingLineYFractionChange = { yFraction ->"))
        assertTrue(projectViewer.contains("isReadingLineDragging = true"))
        assertTrue(projectViewer.contains("readingLinePreviewYFraction = sanitizeReadingLineYFraction(yFraction)"))
        assertFalse(
            contentActions.contains("onReadingLineYFractionChange = counterViewModel::updateReadingLineYFraction"),
        )
    }

    @Test
    fun `project reading line drag cancel restores committed preview without persistence write`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )
        val overlayBlock =
            source.blockBetween(
                "internal fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertTrue(projectViewer.contains("onReadingLineDragCancel = {"))
        assertTrue(projectViewer.contains("isReadingLineDragging = false"))
        assertTrue(projectViewer.contains("selectedDocument?.readingLineYFraction ?: DEFAULT_READING_LINE_Y_FRACTION"))
        assertFalse(projectViewer.contains("onReadingLineDragCancel = counterViewModel::updateReadingLineYFraction"))
        assertTrue(overlayBlock.contains("onDragCancel = { actions.onDragCancel() }"))
    }

    @Test
    fun `reading line overlay exposes drag start and cancel callbacks`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val overlayBlock =
            source.blockBetween(
                "internal fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertTrue(overlayBlock.contains("actions: ReadingLineOverlayActions"))
        assertTrue(source.contains("internal data class ReadingLineOverlayActions("))
        assertTrue(source.contains("val onDragStart: () -> Unit"))
        assertTrue(source.contains("val onDragCancel: () -> Unit"))
        assertTrue(overlayBlock.contains("onDragStart = {"))
        assertTrue(overlayBlock.contains("actions.onDragStart()"))
        assertTrue(overlayBlock.contains("onDragCancel = { actions.onDragCancel() }"))
    }

    @Test
    fun `bookmark jump focuses the persisted bookmark page`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val jumpBlock =
            source.blockBetween(
                "is PatternViewerEvent.BookmarkJumped -> {",
                "accessibilityAnnouncement =",
            )

        assertTrue(jumpBlock.contains("pageIndex = event.bookmark.pageIndex"))
    }

    @Test
    fun `pattern row marker upsert replaces same row page pair`() {
        val source = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val upsertBlock =
            source.blockBetween(
                "fun upsertPatternRowMarker(",
                "fun mergePatternRowMarkers(",
            )

        assertTrue(upsertBlock.contains("markers.indexOfFirst { it.row == row && it.page == page }"))
        assertTrue(upsertBlock.contains("markers[index] = marker"))
        assertTrue(upsertBlock.contains("markers += marker"))
    }

    @Test
    fun `project pattern viewer observes repository-owned reading line location`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)

        assertFalse(viewer.contains("TrackReadingLineForCurrentRow("))
        assertTrue(viewer.contains("val currentPage = selectedDocument?.currentPage ?: 0"))
        assertTrue(repository.contains("resolveReadingLineLocation("))
        assertTrue(repository.contains("updateViewerStateInTransaction("))
    }

    @Test
    fun `external document add returns duplicate feedback to the documents sheet`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("patternViewerViewModel.handleDocumentAddResult(result)"))
        assertTrue(source.contains("if (result !is ProjectDocumentMutationResult.Added)"))
        assertTrue(source.contains("showDocumentSheet = true"))
    }

    @Test
    fun `repository parses row mapping before page-aware resolution`() {
        val repository = ProjectSourceFiles.read(COUNTER_REPOSITORY)
        val followBlock =
            repository.blockBetween(
                "private suspend fun applyReadingLineFollow(",
                "private suspend fun applyLinkedCounterDelta(",
            )

        assertTrue(followBlock.contains("parseMapping(document.rowMapping)"))
        assertTrue(followBlock.contains("resolveReadingLineLocation("))
    }

    // CPD-OFF: Lahdekooditesti toistaa tarkoituksella saman projektin ja kirjaston rajauksen.
    @Test
    fun `project pattern viewer exposes row marker controls only from project overflow`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )
        val libraryViewer =
            source.blockBetween(
                "fun LibraryPatternViewerScreen(",
                "@Composable\nprivate fun rememberPatternRenderState",
            )

        assertTrue(projectViewer.contains("currentRow = counterState.counter.count"))
        assertTrue(projectViewer.contains("onSaveReadingLineAsCurrentRow = {"))
        assertTrue(projectViewer.contains("counterViewModel.upsertPatternRowMarker("))
        assertTrue(projectViewer.contains("onClearReadingLineRowMarker = {"))
        assertTrue(projectViewer.contains("counterViewModel.removePatternRowMarker("))
        assertTrue(projectViewer.contains("row = counterState.counter.count"))
        assertTrue(projectViewer.contains("page = currentPage"))
        assertTrue(projectViewer.contains("onClearReadingLinePageMarkers = {"))
        assertTrue(projectViewer.contains("counterViewModel.removePatternRowMarkersForPage(currentPage)"))
        assertFalse(libraryViewer.contains("removePatternRowMarker"))
        assertFalse(libraryViewer.contains("removePatternRowMarkersForPage"))
        assertFalse(libraryViewer.contains("pattern_save_line_as_row"))
    }
    // CPD-ON

    @Test
    fun `project pattern viewer exposes two point calibration and merges accepted markers`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )
        val libraryViewer =
            source.blockBetween(
                "fun LibraryPatternViewerScreen(",
                "@Composable\nprivate fun rememberPatternRenderState",
            )

        assertTrue(projectViewer.contains("var rowCalibrationState by remember"))
        assertTrue(projectViewer.contains("onStartRowCalibration = {"))
        assertTrue(projectViewer.contains("counterViewModel.setReadingLineEnabled(true)"))
        assertTrue(projectViewer.contains("RowCalibrationPanel("))
        assertTrue(projectViewer.contains("createCalibrationRowMarkers("))
        assertTrue(projectViewer.contains("counterViewModel.mergePatternRowMarkers(markers)"))
        assertTrue(projectViewer.contains("rowCalibrationState = null"))
        assertFalse(libraryViewer.contains("RowCalibrationPanel("))
        assertFalse(libraryViewer.contains("mergePatternRowMarkers"))
    }

    @Test
    fun `row calibration keeps invalid input local and shows localized error`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val calibrationBlock =
            source.blockBetween(
                "private data class RowCalibrationState(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )

        assertTrue(calibrationBlock.contains("showInvalidRowError: Boolean"))
        assertTrue(calibrationBlock.contains("markers == null"))
        assertTrue(calibrationBlock.contains("pattern_calibration_invalid_row"))
        assertTrue(calibrationBlock.contains("rowInput.toIntOrNull()"))
        assertFalse(calibrationBlock.contains("mergePatternRowMarkers(markers) } ?:"))
    }

    @Test
    fun `pattern viewer top bar renders row marker menu items from string resources`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val overflowMenu =
            source.blockBetween(
                "private fun PatternViewerOverflowMenu(",
                "@Composable\nprivate fun PatternPageJumpDialog",
            )

        assertTrue(overflowMenu.contains("R.string.pattern_set_row_marker_here"))
        assertTrue(overflowMenu.contains("R.string.pattern_clear_row_mark"))
        assertTrue(overflowMenu.contains("R.string.pattern_clear_page_marks"))
        assertTrue(overflowMenu.contains("R.string.pattern_calibrate_rows"))
        assertTrue(overflowMenu.contains("val currentRow = state.currentRow ?: return"))
        assertTrue(overflowMenu.contains("actions.onSaveReadingLineAsCurrentRow()"))
        assertTrue(overflowMenu.contains("actions.onClearReadingLineRowMarker()"))
        assertTrue(overflowMenu.contains("actions.onClearReadingLinePageMarkers()"))
        assertTrue(overflowMenu.contains("actions.onStartRowCalibration()"))
    }

    @Test
    fun `project pattern viewer guards row marker overflow when no pdf is attached`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )
        val overflowMenu =
            source.blockBetween(
                "private fun PatternViewerOverflowMenu(",
                "@Composable\nprivate fun PatternPageJumpDialog",
            )

        assertTrue(projectViewer.contains("val rowMarkers = remember(selectedDocument?.rowMapping)"))
        assertTrue(projectViewer.contains("currentRow = counterState.counter.count.takeIf { patternUri != null }"))
        assertTrue(projectViewer.contains("hasCurrentRowMarker ="))
        assertTrue(projectViewer.contains("hasPageRowMarkers ="))
        assertTrue(overflowMenu.contains("if (state.hasCurrentRowMarker)"))
        assertTrue(overflowMenu.contains("if (state.hasPageRowMarkers)"))
    }

    @Test
    fun `counter view model can remove row markers by row page and by page`() {
        val source = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val removeRowBlock =
            source.blockBetween(
                "fun removePatternRowMarker(",
                "fun removePatternRowMarkersForPage(",
            )
        val removePageBlock =
            source.blockBetween(
                "fun removePatternRowMarkersForPage(",
                "fun mergePatternRowMarkers(",
            )

        assertTrue(removeRowBlock.contains("val currentMarkers = parseMapping(state.patternRowMapping)"))
        assertTrue(removeRowBlock.contains("filterNot { it.row == row && it.page == page }"))
        assertTrue(removeRowBlock.contains("if (markers.size == currentMarkers.size) return"))
        assertTrue(removeRowBlock.contains("updatePatternRowMapping(serializeMapping(markers))"))
        assertTrue(removePageBlock.contains("val currentMarkers = parseMapping(state.patternRowMapping)"))
        assertTrue(removePageBlock.contains("filterNot { it.page == page }"))
        assertTrue(removePageBlock.contains("if (markers.size == currentMarkers.size) return"))
        assertTrue(removePageBlock.contains("updatePatternRowMapping(serializeMapping(markers))"))
    }

    @Test
    fun `counter view model skips row marker writes without pdf or matching markers`() {
        val source = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val upsertBlock =
            source.blockBetween(
                "fun upsertPatternRowMarker(",
                "fun removePatternRowMarker(",
            )
        val removeRowBlock =
            source.blockBetween(
                "fun removePatternRowMarker(",
                "fun removePatternRowMarkersForPage(",
            )
        val removePageBlock =
            source.blockBetween(
                "fun removePatternRowMarkersForPage(",
                "fun mergePatternRowMarkers(",
            )
        val mergeBlock =
            source.blockBetween(
                "fun mergePatternRowMarkers(",
                "private fun persistCount(",
            )

        listOf(upsertBlock, removeRowBlock, removePageBlock, mergeBlock).forEach { block ->
            assertTrue(block.contains("val state = _uiState.value"))
            assertTrue(block.contains("if (state.patternUri == null) return"))
        }
        assertTrue(removeRowBlock.contains("val currentMarkers = parseMapping(state.patternRowMapping)"))
        assertTrue(removeRowBlock.contains("if (markers.size == currentMarkers.size) return"))
        assertTrue(removePageBlock.contains("val currentMarkers = parseMapping(state.patternRowMapping)"))
        assertTrue(removePageBlock.contains("if (markers.size == currentMarkers.size) return"))
    }

    @Test
    fun `localized pattern viewer strings include row marker controls`() {
        ProjectSourceFiles.localizedStringFiles().forEach { stringsFile ->
            val strings = ProjectSourceFiles.read(stringsFile)

            listOf(
                "pattern_clear_row_mark",
                "pattern_clear_page_marks",
                "pattern_calibrate_rows",
                "pattern_calibration_first_row",
                "pattern_calibration_last_row",
                "pattern_calibration_save_first",
                "pattern_calibration_save_last",
                "pattern_calibration_invalid_row",
            ).forEach { key ->
                assertTrue("Missing $key in $stringsFile", strings.hasStringResource(key))
            }
        }
    }

    @Test
    fun `reading line controls stay out of counter screen`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertFalse(counterScreen.contains("pattern_show_reading_line"))
        assertFalse(counterScreen.contains("pattern_hide_reading_line"))
        assertFalse(counterScreen.contains("ReadingLineOverlay"))
    }

    @Test
    fun `pdf viewport owns zoom pan reset scroll and coordinate transform`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val viewport = ProjectSourceFiles.read(PATTERN_DOCUMENT_VIEWPORT)

        assertTrue(viewer.contains("PatternDocumentViewport("))
        assertFalse(viewer.contains("rememberTransformableState"))
        assertTrue(viewport.contains("rememberTransformableState"))
        assertTrue(viewport.contains("viewportState.reset()"))
        assertTrue(viewport.contains("val scrollState = rememberScrollState()"))
        assertTrue(viewport.contains(".verticalScroll(scrollState)"))
        assertTrue(viewport.contains("toPageCoordinateTransform(pageSize)"))
    }

    @Test
    fun `hidden editable annotation layer disables pointer input`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val interactionOverlay = viewer.blockBetween("interactionOverlay =", "PatternAnnotationInputOverlay(")
        val normalizedGuard = interactionOverlay.replace(Regex("\\s+"), " ")

        assertTrue(
            normalizedGuard.contains(
                "if (editableLayerVisible && state.annotationState.activeTool != PatternAnnotationTool.BROWSE)",
            ),
        )
    }

    @Test
    fun `page render clears the previous bitmap before rendering the next page`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val renderProducer = viewer.blockBetween("val renderedBitmap by produceState", "return PatternRenderState(")
        val resetIndex = renderProducer.indexOf("value = null")
        val rendererLookupIndex = renderProducer.indexOf("val activeRenderer")

        assertTrue(resetIndex >= 0)
        assertTrue(resetIndex < rendererLookupIndex)
    }

    @Test
    fun `annotated export filename comes from string resources`() {
        val viewer = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(viewer.contains("R.string.pattern_annotation_export_default_name"))
        assertTrue(viewer.contains("R.string.pattern_annotation_export_filename"))
        assertFalse(viewer.contains("?: \"pattern\""))
        assertFalse(viewer.contains("-annotated.pdf"))
    }

    private companion object {
        const val PATTERN_VIEWER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
        const val PATTERN_DOCUMENT_VIEWPORT =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternDocumentViewport.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
    }
}

private fun String.blockBetween(
    start: String,
    end: String,
): String = substringAfter(start).substringBefore(end)

private fun String.hasStringResource(key: String): Boolean =
    Regex("""<string\s+name="$key"(\s|>)""").containsMatchIn(this)

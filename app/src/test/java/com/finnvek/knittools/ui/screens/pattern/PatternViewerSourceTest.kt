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

        assertTrue(source.contains("readingLineEnabled = counterState.readingLineEnabled"))
        assertTrue(source.contains("readingLineYFraction = counterState.readingLineYFraction"))
        assertTrue(source.contains("onReadingLineToggle = counterViewModel::setReadingLineEnabled"))
        assertTrue(source.contains("onReadingLineYFractionChange = counterViewModel::updateReadingLineYFraction"))
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
    fun `reading line is toggled from pattern viewer overflow and drawn in transformed pdf layer`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("R.string.pattern_show_reading_line"))
        assertTrue(source.contains("R.string.pattern_hide_reading_line"))
        assertTrue(source.contains(".transformable(state = transformableState)"))
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
                "private fun PatternViewerDocument(",
                "@Composable\nprivate fun ReadingLineOverlay",
            )
        val overlayBlock =
            source.blockBetween(
                "private fun ReadingLineOverlay(",
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
                "private fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertFalse(overlayBlock.contains("\"Row "))
        assertFalse(overlayBlock.contains("\"Kerros "))
        assertFalse(overlayBlock.contains("Text(\""))
        assertTrue(overlayBlock.contains("stringResource(R.string.current_row_short, currentRow)"))
    }

    @Test
    fun `project reading line drag commit stores current row page anchor`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
            )
        val libraryViewer =
            source.blockBetween(
                "fun LibraryPatternViewerScreen(",
                "@Composable\nprivate fun rememberPatternRenderState",
            )

        assertTrue(projectViewer.contains("onReadingLineYFractionCommit = { yFraction ->"))
        assertTrue(projectViewer.contains("val sanitizedYFraction = sanitizeReadingLineYFraction(yFraction)"))
        assertTrue(projectViewer.contains("counterViewModel.updateReadingLineYFraction(sanitizedYFraction)"))
        assertTrue(projectViewer.contains("counterViewModel.upsertPatternRowMarker("))
        assertTrue(projectViewer.contains("row = counterState.counter.count"))
        assertTrue(projectViewer.contains("page = currentPage"))
        assertTrue(projectViewer.contains("yPosition = sanitizedYFraction"))
        assertFalse(libraryViewer.contains("upsertPatternRowMarker"))
    }

    @Test
    fun `project reading line drag live updates preview state without persistence write`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
            )
        val contentActions =
            projectViewer.blockBetween(
                "actions =\n                    PatternViewerContentActions(",
                "                modifier =\n",
            )

        assertTrue(projectViewer.contains("var readingLinePreviewYFraction by remember(patternUri)"))
        assertTrue(projectViewer.contains("var isReadingLineDragging by remember(patternUri)"))
        assertTrue(
            projectViewer.contains(
                "LaunchedEffect(patternUri, counterState.readingLineYFraction, isReadingLineDragging)",
            ),
        )
        assertTrue(projectViewer.contains("if (!isReadingLineDragging)"))
        assertTrue(projectViewer.contains("readingLinePreviewYFraction = counterState.readingLineYFraction"))
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
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
            )
        val overlayBlock =
            source.blockBetween(
                "private fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertTrue(projectViewer.contains("onReadingLineDragCancel = {"))
        assertTrue(projectViewer.contains("isReadingLineDragging = false"))
        assertTrue(projectViewer.contains("readingLinePreviewYFraction = counterState.readingLineYFraction"))
        assertFalse(projectViewer.contains("onReadingLineDragCancel = counterViewModel::updateReadingLineYFraction"))
        assertTrue(overlayBlock.contains("onDragCancel = { actions.onDragCancel() }"))
    }

    @Test
    fun `reading line overlay exposes drag start and cancel callbacks`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val overlayBlock =
            source.blockBetween(
                "private fun ReadingLineOverlay(",
                "// Tilan ja toimintojen ryhmittely PatternViewerBottomBarille",
            )

        assertTrue(overlayBlock.contains("actions: ReadingLineOverlayActions"))
        assertTrue(source.contains("private data class ReadingLineOverlayActions("))
        assertTrue(source.contains("val onDragStart: () -> Unit"))
        assertTrue(source.contains("val onDragCancel: () -> Unit"))
        assertTrue(overlayBlock.contains("onDragStart = {"))
        assertTrue(overlayBlock.contains("actions.onDragStart()"))
        assertTrue(overlayBlock.contains("onDragCancel = { actions.onDragCancel() }"))
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
    fun `project pattern viewer resolves reading line when current row changes`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)

        assertTrue(source.contains("TrackReadingLineForCurrentRow("))
        assertTrue(source.contains("currentRow = counterState.counter.count"))
        assertTrue(source.contains("patternRowMapping = counterState.patternRowMapping"))
        assertTrue(source.contains("resolveReadingLineYFraction("))
        assertTrue(source.contains("LaunchedEffect(currentRow, currentPage, patternRowMapping, readingLineEnabled)"))
    }

    @Test
    fun `project pattern viewer parses row mapping only when mapping changes`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val tracker =
            source.blockBetween(
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
                "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun LibraryPatternViewerScreen",
            )

        assertTrue(tracker.contains("val rowMarkers = remember(patternRowMapping) { parseMapping(patternRowMapping) }"))
        assertTrue(tracker.contains("markers = rowMarkers"))
        assertFalse(tracker.contains("markers = parseMapping(patternRowMapping)"))
    }

    @Test
    fun `project pattern viewer exposes row marker controls only from project overflow`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
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

    @Test
    fun `project pattern viewer exposes two point calibration and merges accepted markers`() {
        val source = ProjectSourceFiles.read(PATTERN_VIEWER_SCREEN)
        val projectViewer =
            source.blockBetween(
                "fun PatternViewerScreen(",
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
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
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
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

        assertTrue(overflowMenu.contains("R.string.pattern_save_line_as_row"))
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
                "@Composable\nprivate fun TrackReadingLineForCurrentRow",
            )
        val overflowMenu =
            source.blockBetween(
                "private fun PatternViewerOverflowMenu(",
                "@Composable\nprivate fun PatternPageJumpDialog",
            )

        assertTrue(projectViewer.contains("val rowMarkers = remember(counterState.patternRowMapping)"))
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
                "pattern_save_line_as_row",
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

    private companion object {
        const val PATTERN_VIEWER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternViewerScreen.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
    }
}

private fun String.blockBetween(
    start: String,
    end: String,
): String = substringAfter(start).substringBefore(end)

private fun String.hasStringResource(key: String): Boolean =
    Regex("""<string\s+name="$key"(\s|>)""").containsMatchIn(this)

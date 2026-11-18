package com.finnvek.knittools.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.ui.screens.counterhistory.CounterHistoryContent
import com.finnvek.knittools.ui.screens.counterhistory.CounterHistoryUiState
import com.finnvek.knittools.ui.screens.project.ProjectFoldersSheet
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Explicit transient layout states; route/SQLite interactions are covered by the native matrix. */
class AuditPart08LoadingUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun folderLoadingAndReadErrorKeepCreateAndRetryReachable() {
        val loading = mutableStateOf(true)
        var retries = 0
        var creates = 0
        compose.setContent {
            KnitToolsTheme {
                ProjectFoldersSheet(
                    folders = emptyList(),
                    selectedFilter = ProjectFolderFilter.AllProjects,
                    isLoading = loading.value,
                    errorMessage = if (loading.value) null else context.getString(R.string.folder_load_error),
                    isMutating = false,
                    onSelectFilter = {},
                    onCreateFolder = { creates++ },
                    onRenameFolder = {},
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onDeleteFolder = {},
                    onRetry = { retries++ },
                    onDismiss = {},
                )
            }
        }
        readable(R.string.folder_loading)
        compose.onNodeWithText(context.getString(R.string.folder_create)).performScrollTo().performClick()
        assertEquals(1, creates)
        compose.runOnIdle { loading.value = false }
        readable(R.string.folder_load_error)
        compose.onNodeWithText(context.getString(R.string.retry)).performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun historyLoadingDoesNotHideBackOrLeakEmptyMessage() {
        var backs = 0
        compose.setContent {
            KnitToolsTheme {
                CounterHistoryContent(CounterHistoryUiState(loading = true), { backs++ })
            }
        }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.counter_history_empty_title)).assertDoesNotExist()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).assertIsDisplayed().performClick()
        assertEquals(1, backs)
    }

    private fun readable(id: Int) {
        val node = compose.onNodeWithText(context.getString(id)).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val detail =
            layouts.joinToString {
                "size=${it.size}, paragraph=${it.multiParagraph.width} x ${it.multiParagraph.height}, constraints=${it.layoutInput.constraints}, lines=${it.lineCount}, overflow=${it.hasVisualOverflow}"
            }
        val case = InstrumentationRegistry.getArguments().getString("auditCase", "initial")
        val folder = java.io.File(context.getExternalFilesDir(null), "audit18/$case/loading-states").apply { mkdirs() }
        java.io.File(folder, "$id.txt").writeText(detail)
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(folder, "$id.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        assertTrue(detail, layouts.isNotEmpty())
        val size = node.fetchSemanticsNode().size
        layouts.forEach { layout ->
            // Compose's semantics paragraph can retain maxWidth even when the actual text node is narrower.
            // Check the rendered line extents, as GaugeScreenTest does, rather than paragraph-box overflow.
            val width = minOf(size.width, layout.size.width)
            repeat(layout.lineCount) { line ->
                assertTrue(detail, layout.getLineLeft(line) >= -1f && layout.getLineRight(line) <= width + 1f)
                assertTrue(detail, layout.getLineBottom(line) <= size.height + 1f)
                assertTrue(detail, !layout.isLineEllipsized(line))
            }
        }
    }
}

package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationWriteFeedbackSourceTest {
    @Test
    fun `both viewers use shared feedback outside the markup toolbar with explicit dismissal`() {
        val viewer = ProjectSourceFiles.read("$PATTERN/PatternViewerScreen.kt")
        val sharedActions = Regex("annotationActions = annotationViewModel.patternContentAnnotationActions\\(\\)")
        assertEquals(2, sharedActions.findAll(viewer).count())
        val content = viewer.substringAfter("private fun PatternViewerContent(")
        val feedback =
            content
                .substringAfter("if (state.annotationState.writeError")
                .substringBefore("if (state.annotationState.isExporting)")
        assertTrue(feedback.contains("PatternAnnotationWriteError.WRITE_FAILED"))
        assertTrue(feedback.contains("(!state.annotationMode || state.annotationState.textEdit == null)"))
        assertTrue(feedback.contains("TextButton(onClick = annotationActions.onDismissWriteError)"))
        assertTrue(feedback.contains("R.string.pattern_annotation_command_failed"))
        assertTrue(feedback.contains("R.string.dismiss"))
        assertTrue(viewer.contains("onDismissWriteError = ::dismissWriteError"))
        assertFalse(feedback.contains("LaunchedEffect"))
    }

    @Test
    fun `existing text editor retains saveable draft and separate conflict message`() {
        val toolbar = ProjectSourceFiles.read("$PATTERN/PatternAnnotationToolbar.kt")
        val edit = toolbar.substringAfter("state.textEdit?.let").substringBefore("if (showCalloutEditor)")
        assertTrue(edit.contains("writeFailed = state.writeError == PatternAnnotationWriteError.WRITE_FAILED"))
        assertTrue(edit.contains("textConflict = state.writeError == PatternAnnotationWriteError.TEXT_CONFLICT"))
        val dialog =
            toolbar
                .substringAfter("private fun PatternTextEditorDialog(")
                .substringBefore("private fun PatternCalloutEditorDialog(")
        assertTrue(dialog.contains("var text by rememberSaveable { mutableStateOf(initialText) }"))
        assertTrue(dialog.contains("R.string.pattern_annotation_write_failed"))
        assertTrue(dialog.contains("enabled = text.isNotBlank() && !isSaving && !textConflict"))
    }

    @Test
    fun `command failure copy and dismissal exist in every configured locale separately from text conflict`() {
        for (directory in ProjectSourceFiles.configuredResourceDirectories()) {
            val resources = ProjectSourceFiles.textResources(directory)
            val message = resources.getValue("pattern_annotation_command_failed").textContent
            assertTrue(directory, message.isNotBlank())
            assertFalse(directory, message == resources.getValue("pattern_annotation_write_failed").textContent)
            assertTrue(directory, resources.getValue("dismiss").textContent.isNotBlank())
        }
    }

    private companion object {
        const val PATTERN = "app/src/main/java/com/finnvek/knittools/ui/screens/pattern"
    }
}

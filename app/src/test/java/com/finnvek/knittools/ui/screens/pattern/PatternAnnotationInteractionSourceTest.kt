package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationInteractionSourceTest {
    @Test
    fun `pointer move never writes repository and commit owns the insert`() {
        val viewModel = ProjectSourceFiles.read(VIEW_MODEL)
        val appendBlock = viewModel.substringAfter("fun appendStrokePoint(").substringBefore("fun commitStroke(")
        val commitBlock = viewModel.substringAfter("fun commitStroke(").substringBefore("fun cancelStroke(")

        assertFalse(appendBlock.contains("annotationRepository"))
        assertTrue(commitBlock.contains("executeCommand(PatternAnnotationCommand.Insert(annotation))"))
    }

    @Test
    fun `pointer overlay separates stylus palm and two finger transform gestures`() {
        val overlay = ProjectSourceFiles.read(OVERLAY)

        assertTrue(overlay.contains("annotationPointers.size >= 2"))
        assertTrue(overlay.contains("pressedChanges.filter { it.type != PointerType.Touch }"))
        assertTrue(overlay.contains("PointerType.Eraser -> PatternInputPointerType.ERASER"))
        assertTrue(overlay.contains("actions.onCancelStroke()"))
        assertTrue(overlay.contains("actions.onCommitStroke(simplificationTolerance)"))
    }

    private companion object {
        const val VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationViewModel.kt"
        const val OVERLAY =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternAnnotationOverlay.kt"
    }
}

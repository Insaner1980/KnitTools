package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterUndoSourceTest {
    @Test
    fun `undo uses persisted main counter history instead of transient previous count`() {
        val source = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val undoBlock =
            source
                .substringAfter("fun undo()")
                .substringBefore("fun reset()")

        assertTrue(
            undoBlock.contains("repository.applyMainCounterChange(projectId, MainCounterChange.Undo)"),
        )
        assertFalse(undoBlock.contains("state.counter.previousCount ?: return"))
        assertFalse(undoBlock.contains("CounterLogic.undo(state.counter)"))
    }

    private companion object {
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
    }
}

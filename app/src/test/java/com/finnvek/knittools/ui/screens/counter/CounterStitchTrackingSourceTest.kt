package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterStitchTrackingSourceTest {
    @Test
    fun `counter screen renders stitch tracker and exposes stitch count setup`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val projectActions = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)

        assertTrue(counterScreen.contains("import com.finnvek.knittools.ui.components.StitchCounter"))
        assertTrue(counterScreen.contains("StitchCounter("))
        assertTrue(counterScreen.contains("showStitchDialog = true"))
        assertTrue(projectActions.contains("val stitchCount: Int?"))
        assertTrue(projectActions.contains("label = stringResource(R.string.stitches_per_row)"))
    }

    @Test
    fun `voice reset action delegates to row reset`() {
        val counterViewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)

        assertTrue(counterViewModel.contains("is AiVoiceAction.Reset -> {\n                    reset()"))
    }

    @Test
    fun `widget row actions reset persisted stitch position inside repository transaction`() {
        val widgetActions = ProjectSourceFiles.read(COUNTER_WIDGET_ACTIONS)
        val counterRepository = ProjectSourceFiles.read(COUNTER_REPOSITORY)

        assertTrue(widgetActions.contains("repository.applyWidgetCountChange"))
        assertTrue(counterRepository.contains("transactionRunner.run"))
        assertTrue(counterRepository.contains("dao.updateCurrentStitch(id, 0, updatedAt)"))
    }

    private companion object {
        private const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        private const val PROJECT_ACTIONS_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        private const val COUNTER_WIDGET_ACTIONS =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
    }
}

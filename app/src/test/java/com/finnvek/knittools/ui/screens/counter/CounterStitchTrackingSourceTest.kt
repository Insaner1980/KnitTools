package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterStitchTrackingSourceTest {
    @Test
    fun `counter screen renders stitch tracker and exposes stitch count setup`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val projectActions = ProjectSourceFiles.read(PROJECT_ACTIONS_BOTTOM_SHEET)

        assertTrue(workspace.contains("import com.finnvek.knittools.ui.components.StitchCounter"))
        assertTrue(workspace.contains("StitchCounter("))
        assertTrue(counterScreen.contains("showStitchDialog = true"))
        assertTrue(projectActions.contains("val stitchCount: Int?"))
        assertTrue(projectActions.contains("label = stringResource(R.string.stitches_per_row)"))
    }

    @Test
    fun `stitch reset is not coupled to removed voice commands`() {
        val counterScreen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertFalse(counterScreen.contains("VoiceCommand.Reset -> {"))
        assertFalse(counterScreen.contains("VoiceCommand"))
        assertTrue(counterScreen.contains("viewModel.reset()"))
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
        private const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        private const val PROJECT_ACTIONS_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
        private const val COUNTER_WIDGET_ACTIONS =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
    }
}

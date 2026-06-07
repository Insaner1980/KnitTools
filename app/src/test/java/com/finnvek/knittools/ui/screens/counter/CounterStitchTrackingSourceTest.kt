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
    fun `stitch tracker stays visually related to hero as secondary plus minus stepper`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val stitchCounter = ProjectSourceFiles.read(STITCH_COUNTER)
        val stepperButton = ProjectSourceFiles.read(COUNTER_STEPPER_BUTTON)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val strings = ProjectSourceFiles.read(STRINGS)
        val hero =
            workspace
                .substringAfter("private fun CounterHero")
                .substringBefore("@Composable\nprivate fun CounterRowLabel")

        assertFalse(workspace.contains("""item(key = "stitch-tracker")"""))
        assertTrue(hero.contains("CounterControlsToStitchTrackerSpacing"))
        assertTrue(hero.indexOf("CounterButtons(") < hero.indexOf("CounterStitchTracker("))
        assertTrue(stitchCounter.contains("Icons.Filled.Remove"))
        assertTrue(stitchCounter.contains("Icons.Filled.Add"))
        assertTrue(stitchCounter.contains("CounterStepperButton("))
        assertTrue(stitchCounter.contains("isIncrement = false"))
        assertTrue(stitchCounter.contains("isIncrement = true"))
        assertFalse(stitchCounter.contains("private fun StitchCounterButton"))
        assertTrue(stitchCounter.contains("MaterialTheme.colorScheme.surfaceContainerHighest.copy"))
        assertTrue(stitchCounter.contains("CounterDimens.StitchTrackerContainerAlpha"))
        assertTrue(stitchCounter.contains("stringResource(R.string.stitch_counter_compact_format"))
        assertFalse(stitchCounter.contains("R.string.stitch_previous"))
        assertFalse(stitchCounter.contains("R.string.stitch_next"))
        assertFalse(strings.contains("""<string name="stitch_previous">"""))
        assertFalse(strings.contains("""<string name="stitch_next">"""))
        assertTrue(dimens.contains("CounterControlsToStitchTrackerSpacing = 72.dp"))
        assertTrue(dimens.contains("StitchTrackerContainerAlpha = 0.42f"))
        assertTrue(stepperButton.contains("CounterDimens.ExtraCounterStepperTouchSize"))
        assertTrue(stepperButton.contains("CounterDimens.ExtraCounterStepperVisualSize"))
        assertTrue(stepperButton.contains("CounterDimens.ExtraCounterStepperIconSize"))
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
        private const val STITCH_COUNTER =
            "app/src/main/java/com/finnvek/knittools/ui/components/StitchCounter.kt"
        private const val COUNTER_STEPPER_BUTTON =
            "app/src/main/java/com/finnvek/knittools/ui/components/CounterStepperButton.kt"
        private const val COUNTER_DIMENS =
            "app/src/main/java/com/finnvek/knittools/ui/theme/CounterDimens.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
        private const val PROJECT_ACTIONS_BOTTOM_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/ProjectActionsBottomSheet.kt"
        private const val COUNTER_WIDGET_ACTIONS =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt"
        private const val COUNTER_REPOSITORY =
            "app/src/main/java/com/finnvek/knittools/repository/CounterRepository.kt"
    }
}

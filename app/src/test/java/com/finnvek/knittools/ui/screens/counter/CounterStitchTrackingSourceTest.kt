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
        assertTrue(stitchCounter.contains("symbol = CounterStepSymbol.Minus"))
        assertTrue(stitchCounter.contains("symbol = CounterStepSymbol.Plus"))
        assertFalse(stitchCounter.contains("Icons.Filled.Remove"))
        assertFalse(stitchCounter.contains("Icons.Filled.Add"))
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
    fun `repeat hero keeps stitch tracker visible after undo`() {
        val workspace = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val dimens = ProjectSourceFiles.read(COUNTER_DIMENS)
        val hero =
            workspace
                .substringAfter("private fun CounterHero")
                .substringBefore("@Composable\nprivate fun CounterStitchTracker")

        assertTrue(dimens.contains("HeroButtonCompactSpacing = 8.dp"))
        assertTrue(hero.contains("val heroButtonSpacing ="))
        assertTrue(hero.contains("val controlsToStitchTrackerSpacing ="))
        assertTrue(hero.contains("state.canUseSecondaryCounter && state.visibleStitchTotal != null"))
        assertTrue(hero.contains("CounterDimens.HeroButtonCompactSpacing"))
        assertTrue(hero.contains("CounterDimens.CounterControlsToStitchTrackerCompactSpacing"))
        assertTrue(hero.contains("Spacer(modifier = Modifier.height(heroButtonSpacing))"))
        assertTrue(hero.contains("Spacer(modifier = Modifier.height(controlsToStitchTrackerSpacing))"))
        assertTrue(dimens.contains("CounterRepeatToRowSpacing = 32.dp"))
        assertTrue(dimens.contains("CounterUndoTouchSize = CounterUndoVisualSize"))
        assertTrue(dimens.contains("CounterUndoVerticalSpacing = 16.dp"))
        assertTrue(dimens.contains("CounterControlsHeight ="))
        assertTrue(dimens.contains("CounterPrimaryTouchSize + CounterUndoVerticalSpacing + CounterUndoTouchSize"))
        assertTrue(dimens.contains("CounterControlsToStitchTrackerCompactSpacing = 8.dp"))
        assertTrue(dimens.contains("StitchTrackerMinHeight = 68.dp"))
        assertTrue(
            ProjectSourceFiles
                .read(STITCH_COUNTER)
                .contains("heightIn(min = CounterDimens.StitchTrackerMinHeight)"),
        )
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

    @Test
    fun `row counter actions leave persisted stitch reset inside repository transaction`() {
        val counterViewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val counterRepository = ProjectSourceFiles.read(COUNTER_REPOSITORY)

        assertFalse(counterViewModel.contains("persistCurrentStitchIfNeeded(resetStitch)"))
        assertFalse(counterViewModel.contains("private fun persistCurrentStitchIfNeeded"))
        assertTrue(
            counterViewModel.contains(
                "repository.applyMainCounterChange(projectId, action.toMainCounterChange())",
            ),
        )
        assertTrue(counterRepository.contains("transactionRunner.run"))
        assertTrue(counterRepository.contains("dao.updateCurrentStitch(id, 0, updatedAt)"))
    }

    private companion object {
        private const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
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

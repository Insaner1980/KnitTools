package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateRaceSourceTest {
    @Test
    fun `pattern camera scan rechecks feature gate at launch and capture boundaries`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER_SHEET)

        assertTrue(picker.contains("internal fun canStartPatternCameraScan("))
        assertTrue(picker.contains("enabled = canStartPatternCameraScan(projectId, canUseCameraScan)"))
        assertTrue(picker.contains("if (!canStartPatternCameraScan(pendingProjectId, currentCanUseCameraScan))"))
        assertTrue(picker.contains("canUseCameraScan = currentCanUseCameraScan"))
    }

    @Test
    fun `classic voice start rechecks voice command feature before starting listener`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertTrue(viewModel.contains("fun canStartClassicVoice(): Boolean"))
        assertTrue(viewModel.contains("proManager.hasFeature(ProFeature.VOICE_COMMANDS)"))
        assertTrue(screen.contains("viewModel.canStartClassicVoice()"))
        assertTrue(screen.contains("if (hasAudioPermission(context) && viewModel.canStartClassicVoice())"))
    }

    @Test
    fun `summary generation rechecks AI feature before calling generator`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val gate = viewModel.indexOf("if (!proManager.hasFeature(ProFeature.AI_FEATURES)) return@launch")
        val generatorCall = viewModel.indexOf("counterSummaryGenerator.generate(state)")

        assertTrue(gate >= 0)
        assertTrue(generatorCall > gate)
    }

    private companion object {
        const val PATTERN_PICKER_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pattern/PatternPickerSheet.kt"
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
    }
}

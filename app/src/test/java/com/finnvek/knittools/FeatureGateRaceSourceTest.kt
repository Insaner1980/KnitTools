package com.finnvek.knittools

import org.junit.Assert.assertFalse
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
    fun `classic voice start path is removed from counter`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertFalse(viewModel.contains("fun canStartClassicVoice(): Boolean"))
        assertFalse(viewModel.contains("ProFeature.VOICE_COMMANDS"))
        assertFalse(screen.contains("viewModel.canStartClassicVoice()"))
        assertFalse(screen.contains("hasAudioPermission(context)"))
        assertFalse(screen.contains("Manifest.permission.RECORD_AUDIO"))
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

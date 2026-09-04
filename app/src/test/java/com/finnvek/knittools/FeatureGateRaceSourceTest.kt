package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateRaceSourceTest {
    @Test
    fun `pattern camera scan persists bounded authorization across the camera result`() {
        val picker = ProjectSourceFiles.read(PATTERN_PICKER_SHEET)

        assertTrue(picker.contains("internal fun canStartPatternCameraScan("))
        assertTrue(picker.contains("var pendingCaptureAuthorized by rememberSaveable"))
        assertTrue(picker.contains("currentCanUseCameraScan || pendingCaptureAuthorized"))
        assertTrue(picker.contains("imageImportViewModel.authorizeCameraCapture"))
        assertTrue(picker.contains("imageImportViewModel.createCameraCaptureTarget"))
        assertTrue(picker.contains("imageImportViewModel.discardCameraCapture(uri, file)"))
        assertTrue(picker.contains("catch (_: ActivityNotFoundException)"))
        assertTrue(picker.contains("catch (_: SecurityException)"))
        assertTrue(picker.contains("pendingCaptureAuthorized = false"))
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

    @Test
    fun `counter history pruning is independent of pro and billing state`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)

        assertTrue(viewModel.contains("pruneHistory(project.id)"))
        assertTrue(viewModel.contains("private suspend fun pruneHistory(projectId: Long)"))
        assertFalse(viewModel.contains("pruneHistoryForFree"))
        assertFalse(viewModel.contains("initialStateReady && !proState.isPro"))
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

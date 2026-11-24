package com.finnvek.knittools.ui.screens.backup

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCancellationFeedbackSourceTest {
    private val screen =
        ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/screens/backup/BackupScreen.kt")

    @Test fun cancellationOutcomeUsesTheObservedLocalizedVisibleError() {
        val route = screen.substringBefore("internal fun BackupContent(")
        assertTrue(route.contains("val state by viewModel.state.collectAsStateWithLifecycle()"))
        assertTrue(route.contains("state = state"))
        assertTrue(route.contains("onCancel = viewModel::cancel"))
        val feedback = screen.substringAfter("state.error?.let { error ->").substringBefore("state.preview?.let")
        assertTrue(feedback.contains("Text(stringResource(errorMessage(error))"))
        assertTrue(feedback.contains("liveRegion = LiveRegionMode.Assertive"))
        assertTrue(screen.contains("BackupError.WRITE -> R.string.backup_error_write"))
        assertFalse(feedback.contains("backup_exported"))
    }

    @Test fun busyBackCancelsInPlaceAndIdleBackNavigatesWhileRestoringHidesCancel() {
        assertTrue(screen.contains("BackHandler(state.busy || state.preview != null) { onCancel() }"))
        val toolbar = screen.substringAfter("onBack = {").substringBefore("wrapTitle = true")
        assertTrue(Regex("""state\.busy -> \{\s*onCancel\(\)\s*}""").containsMatchIn(toolbar))
        assertTrue(Regex("else -> \\{\\s+onCancel\\(\\)\\s+onBack\\(\\)").containsMatchIn(toolbar))
        val progress = screen.substringAfter("private fun BackupProgress(").substringBefore("private fun phaseMessage")
        assertTrue(progress.contains("if (phase != BackupPhase.RESTORING)"))
        assertTrue(progress.contains("CancelButton(onCancel"))
    }

    @Test fun incompleteDestinationGuidanceExistsInEveryConfiguredLocale() {
        for (directory in ProjectSourceFiles.configuredResourceDirectories()) {
            val resources = ProjectSourceFiles.textResources(directory)
            val message = resources.getValue("backup_error_write").textContent
            assertTrue(directory, message.isNotBlank())
            assertFalse(directory, message == resources.getValue("backup_exported").textContent)
        }
        val english = ProjectSourceFiles.textResources("values").getValue("backup_error_write").textContent
        val finnish = ProjectSourceFiles.textResources("values-fi").getValue("backup_error_write").textContent
        assertTrue(english.contains("Check the destination"))
        assertTrue(english.contains("Delete any incomplete destination file."))
        assertTrue(finnish.contains("Tarkista kohde"))
        assertTrue(finnish.contains("Poista mahdollinen keskeneräinen kohdetiedosto."))
    }
}

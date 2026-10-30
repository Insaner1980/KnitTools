package com.finnvek.knittools.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<BackupRepository>(relaxed = true)
    private val onboarding = mockk<OnboardingStateRepository>(relaxed = true)
    private val uri = mockk<Uri>()
    private val store = ViewModelStore()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun cleanup() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun exportCancellationWaitsForCleanupBeforeReturningToIdle() = cancellation(BackupPhase.EXPORTING)

    @Test fun validationCancellationWaitsForCleanupBeforeReturningToIdle() = cancellation(BackupPhase.VALIDATING)

    @Test fun completedExportSuppressesBackupTip() =
        runTest(dispatcher) {
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            viewModel.export(uri)
            runCurrent()
            assertEquals(BackupPhase.EXPORTED, viewModel.state.value.phase)
            coVerify(exactly = 1) { onboarding.markBackupCreated() }
            coVerify(exactly = 0) { onboarding.markBasicTipsSeen() }
        }

    @Test fun onboardingFailureDoesNotTurnCompletedExportIntoFailure() =
        runTest(dispatcher) {
            coEvery { onboarding.markBackupCreated() } throws IllegalStateException("Unavailable")
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            viewModel.export(uri)
            runCurrent()
            assertEquals(BackupPhase.EXPORTED, viewModel.state.value.phase)
        }

    private fun cancellation(phase: BackupPhase) =
        runTest(dispatcher) {
            val cleanup = CompletableDeferred<Unit>()
            var cancelled = false
            coEvery { repository.export(uri) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            coEvery { repository.prepare(uri, any()) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            coEvery { repository.cancelPreview(any()) } coAnswers { cleanup.await() }
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            if (phase == BackupPhase.EXPORTING) viewModel.export(uri) else viewModel.prepare(uri)
            runCurrent()
            assertEquals(phase, viewModel.state.value.phase)
            viewModel.cancel()
            runCurrent()
            assertTrue(cancelled)
            assertEquals(phase, viewModel.state.value.phase)
            assertFalse(viewModel.select())
            cleanup.complete(Unit)
            runCurrent()
            assertEquals(
                BackupUiState(error = if (phase == BackupPhase.EXPORTING) BackupError.WRITE else null),
                viewModel.state.value,
            )
            coVerify(exactly = 1) { repository.cancelPreview(any()) }
        }

    @Test fun cancelledExportCannotPublishLateSuccess() = cancelledExportCompletion(fail = false)

    @Test fun cancelledExportCannotPublishLateFailure() = cancelledExportCompletion(fail = true)

    private fun cancelledExportCompletion(fail: Boolean) =
        runTest(dispatcher) {
            val operationCleanup = CompletableDeferred<Unit>()
            val previewCleanup = CompletableDeferred<Unit>()
            coEvery { repository.export(uri) } coAnswers {
                try {
                    awaitCancellation()
                } catch (_: CancellationException) {
                    withContext(NonCancellable) { operationCleanup.await() }
                }
                if (fail) throw BackupException(BackupError.SPACE)
            }
            coEvery { repository.cancelPreview(any()) } coAnswers { previewCleanup.await() }
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            viewModel.export(uri)
            runCurrent()

            viewModel.cancel()
            viewModel.cancel()
            runCurrent()
            viewModel.cancel()
            viewModel.export(null)
            viewModel.prepare(null)
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.EXPORTING), viewModel.state.value)
            assertFalse(viewModel.select())
            coVerify(exactly = 1) { repository.cancelPreview(null) }
            viewModel.cancel()
            runCurrent()
            previewCleanup.complete(Unit)
            runCurrent()
            val cancelled = BackupUiState(error = BackupError.WRITE)
            assertEquals(cancelled, viewModel.state.value)
            viewModel.cancel()
            runCurrent()
            assertEquals(cancelled, viewModel.state.value)
            coVerify(exactly = 1) { repository.cancelPreview(any()) }

            val retry = CompletableDeferred<Unit>()
            coEvery { repository.export(uri) } coAnswers { retry.await() }
            assertTrue(viewModel.select())
            assertEquals(BackupUiState(BackupPhase.SELECTING), viewModel.state.value)
            viewModel.export(uri)
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.EXPORTING), viewModel.state.value)
            operationCleanup.complete(Unit)
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.EXPORTING), viewModel.state.value)
            retry.complete(Unit)
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.EXPORTED), viewModel.state.value)
            viewModel.cancel()
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.EXPORTED), viewModel.state.value)
            coVerify(exactly = 2) { repository.export(uri) }
            coVerify(exactly = 0) { repository.prepare(any(), any()) }
            coVerify(exactly = 0) { repository.restore(any()) }
        }

    @Test fun nullExportPickerResultRemainsQuiet() = pickerCancellation(export = true)

    @Test fun nullRestorePickerResultRemainsQuiet() = pickerCancellation(export = false)

    private fun pickerCancellation(export: Boolean) =
        runTest(dispatcher) {
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            assertTrue(viewModel.select())
            if (export) viewModel.export(null) else viewModel.prepare(null)
            runCurrent()
            assertEquals(BackupUiState(), viewModel.state.value)
            coVerify(exactly = 0) { repository.export(any()) }
            coVerify(exactly = 0) { repository.prepare(any(), any()) }
            coVerify(exactly = 0) { repository.restore(any()) }
            coVerify(exactly = 0) { repository.cancelPreview(any()) }
        }

    @Test fun selectionCancellationRemainsQuietAndCannotClearLaterSelection() =
        runTest(dispatcher) {
            val cleanup = CompletableDeferred<Unit>()
            coEvery { repository.cancelPreview(null) } coAnswers { cleanup.await() }
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            assertTrue(viewModel.select())
            viewModel.cancel()
            viewModel.cancel()
            runCurrent()
            assertFalse(viewModel.select())
            cleanup.complete(Unit)
            runCurrent()
            assertEquals(BackupUiState(), viewModel.state.value)
            assertTrue(viewModel.select())
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.SELECTING), viewModel.state.value)
            coVerify(exactly = 1) { repository.cancelPreview(null) }
        }

    @Test fun previewCancellationRemainsQuietAndReleasesOnlyItsSelectionBeforeRetry() =
        runTest(dispatcher) {
            val selection = slot<UUID>()
            val preview = BackupPreview(0, "1", 1, 0, 0, 0)
            val cleanup = CompletableDeferred<Unit>()
            coEvery { repository.prepare(uri, capture(selection)) } returns preview
            coEvery { repository.cancelPreview(any()) } coAnswers { cleanup.await() }
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            viewModel.prepare(uri)
            runCurrent()
            val cancelledSelection = selection.captured
            viewModel.cancel()
            viewModel.cancel()
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.PREVIEW, preview), viewModel.state.value)
            assertFalse(viewModel.select())
            viewModel.restore()
            viewModel.prepare(uri)
            viewModel.export(uri)
            runCurrent()
            coVerify(exactly = 0) { repository.restore(any()) }
            coVerify(exactly = 0) { repository.export(any()) }
            coVerify(exactly = 1) { repository.prepare(uri, any()) }
            cleanup.complete(Unit)
            runCurrent()
            assertEquals(BackupUiState(), viewModel.state.value)
            coVerify(exactly = 1) { repository.cancelPreview(cancelledSelection) }
            assertTrue(viewModel.select())
            viewModel.prepare(uri)
            runCurrent()
            assertFalse(cancelledSelection == selection.captured)
            assertEquals(BackupUiState(BackupPhase.PREVIEW, preview), viewModel.state.value)
            viewModel.restore()
            runCurrent()
            coVerify(exactly = 1) { repository.restore(selection.captured) }
            assertEquals(BackupUiState(BackupPhase.RESTORED), viewModel.state.value)
        }

    @Test fun writeFailureStillRequiresDeliberateRetryAndCanSucceed() =
        runTest(dispatcher) {
            coEvery { repository.export(uri) } throws BackupException(BackupError.WRITE)
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            viewModel.export(uri)
            runCurrent()
            assertEquals(BackupUiState(error = BackupError.WRITE), viewModel.state.value)
            coVerify(exactly = 0) { onboarding.markBackupCreated() }
            viewModel.cancel()
            runCurrent()
            assertEquals(BackupUiState(error = BackupError.WRITE), viewModel.state.value)
            coVerify(exactly = 1) { repository.export(uri) }
            coEvery { repository.export(uri) } returns Unit
            assertTrue(viewModel.select())
            viewModel.export(uri)
            runCurrent()
            assertEquals(BackupUiState(BackupPhase.EXPORTED), viewModel.state.value)
        }

    @Test fun restoringIgnoresCancellationAndCompletesNormally() =
        runTest(dispatcher) {
            val finished = CompletableDeferred<Unit>()
            coEvery { repository.prepare(uri, any()) } returns BackupPreview(0, "1", 1, 0, 0, 0)
            coEvery { repository.restore(any()) } coAnswers { finished.await() }
            val viewModel = BackupViewModel(repository, onboarding)
            store.put("backup", viewModel)
            viewModel.prepare(uri)
            runCurrent()
            viewModel.restore()
            runCurrent()
            viewModel.cancel()
            runCurrent()
            assertEquals(BackupPhase.RESTORING, viewModel.state.value.phase)
            coVerify(exactly = 0) { repository.cancelPreview(any()) }
            finished.complete(Unit)
            runCurrent()
            assertEquals(BackupPhase.RESTORED, viewModel.state.value.phase)
            coVerify(exactly = 1) { onboarding.markBasicTipsSeen() }
            coVerify(exactly = 0) { onboarding.markBackupCreated() }
        }
}

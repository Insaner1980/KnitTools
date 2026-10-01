package com.finnvek.knittools.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.backup.BackupPreview
import com.finnvek.knittools.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<BackupRepository>(relaxed = true)
    private val uri = mockk<Uri>()
    private val store = ViewModelStore()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun cleanup() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun exportCancellationWaitsForCleanupBeforeReturningToIdle() = cancellation(BackupPhase.EXPORTING)

    @Test fun validationCancellationWaitsForCleanupBeforeReturningToIdle() = cancellation(BackupPhase.VALIDATING)

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
            val viewModel = BackupViewModel(repository)
            store.put("backup", viewModel)
            if (phase == BackupPhase.EXPORTING) viewModel.export(uri) else viewModel.prepare(uri)
            runCurrent()
            assertEquals(phase, viewModel.state.value.phase)
            viewModel.cancel()
            runCurrent()
            assertTrue(cancelled)
            assertEquals(phase, viewModel.state.value.phase)
            cleanup.complete(Unit)
            runCurrent()
            assertEquals(BackupUiState(), viewModel.state.value)
            coVerify(exactly = 1) { repository.cancelPreview(any()) }
        }

    @Test fun restoringIgnoresCancellationAndCompletesNormally() =
        runTest(dispatcher) {
            val finished = CompletableDeferred<Unit>()
            coEvery { repository.prepare(uri, any()) } returns BackupPreview(0, "1", 1, 0, 0, 0)
            coEvery { repository.restore(any()) } coAnswers { finished.await() }
            val viewModel = BackupViewModel(repository)
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
        }
}

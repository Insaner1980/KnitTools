package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.repository.RecoveryResolutionResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterRecoveryPromptTest : CounterViewModelFixture() {
    private val original =
        activeSession().copy(
            recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
            recoveryIntervalToken = "old-interval",
        )

    @Test
    fun `manually reopened prompt survives same session resume refresh`() =
        runTest {
            val dismissed = original.copy(recoveryPromptShown = true)
            every { repository.observeActiveSession() } returns MutableStateFlow(dismissed)
            coEvery { repository.refreshActiveSession() } returns dismissed
            val owner = ProcessLifecycleOwner.get()
            val observer = slot<LifecycleObserver>()
            every { owner.lifecycle.addObserver(capture(observer)) } just Runs
            val viewModel = viewModel()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)

            viewModel.showRecoveryPrompt()
            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            (observer.captured as DefaultLifecycleObserver).onResume(owner)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            assertEquals(dismissed, viewModel.uiState.value.activeSession)
            coVerify(exactly = 2) { repository.refreshActiveSession() }
        }

    @Test
    fun `manually reopened prompt survives same interval observation`() =
        runTest {
            val dismissed = original.copy(recoveryPromptShown = true)
            val sessions = MutableStateFlow(dismissed)
            every { repository.observeActiveSession() } returns sessions
            coEvery { repository.refreshActiveSession() } returns dismissed
            val viewModel = viewModel()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)

            viewModel.showRecoveryPrompt()
            val updated = dismissed.copy(pendingRowsWorked = 3)
            sessions.value = updated
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            assertEquals(updated, viewModel.uiState.value.activeSession)
        }

    @Test
    fun `dismissed prompt stays closed on same interval updates until explicitly reopened`() =
        runTest {
            val dismissed = original.copy(recoveryPromptShown = true)
            val sessions = MutableStateFlow(dismissed)
            every { repository.observeActiveSession() } returns sessions
            coEvery { repository.refreshActiveSession() } returns dismissed
            coEvery { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") } returns true
            val viewModel = viewModel()
            advanceUntilIdle()
            sessions.value = dismissed.copy(pendingRowsWorked = 1)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)

            viewModel.showRecoveryPrompt()
            viewModel.dismissRecoveryPrompt()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)
            sessions.value = dismissed.copy(pendingRowsWorked = 2)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)
        }

    @Test
    fun `session removal resolution or replacement does not inherit a manually opened prompt`() =
        runTest {
            val dismissed = original.copy(recoveryPromptShown = true)
            val replacements =
                listOf(
                    null,
                    dismissed.copy(recoveryReason = null, recoveryIntervalToken = null),
                    dismissed.copy(sessionToken = "new-session"),
                    dismissed.copy(recoveryIntervalToken = "new-interval"),
                )
            val sessions = MutableStateFlow<ActiveWorkSession?>(dismissed)
            every { repository.observeActiveSession() } returns sessions
            coEvery { repository.refreshActiveSession() } returns dismissed
            val viewModel = viewModel()
            advanceUntilIdle()
            for (replacement in replacements) {
                sessions.value = dismissed
                advanceUntilIdle()
                viewModel.showRecoveryPrompt()
                assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)

                sessions.value = replacement
                advanceUntilIdle()

                assertEquals(replacement, viewModel.uiState.value.activeSession)
                assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)
            }
            sessions.value = original.copy(recoveryIntervalToken = "fresh-interval")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
        }

    @Test
    fun `successful recovery actions close a manually reopened prompt`() =
        runTest {
            val dismissed = original.copy(recoveryPromptShown = true)
            every { repository.observeActiveSession() } returns MutableStateFlow(dismissed)
            coEvery { repository.refreshActiveSession() } returns dismissed
            coEvery { repository.addRecoveryInterval(dismissed.sessionToken, "old-interval", 120L) } returns
                RecoveryResolutionResult.Continued(dismissed.copy(recoveryReason = null, recoveryIntervalToken = null))
            coEvery { repository.discardRecoveryInterval(dismissed.sessionToken, "old-interval") } returns
                RecoveryResolutionResult.DiscardedAndStopped(1L)
            coEvery { repository.editRecoveryDurationAndStop(dismissed.sessionToken, "old-interval", 180L) } returns
                RecoveryResolutionResult.EditedAndStopped(1L)
            val viewModel = viewModel()
            advanceUntilIdle()
            val actions =
                listOf<() -> Unit>(
                    { viewModel.addRecoveryInterval(120L) },
                    { viewModel.discardRecoveryInterval() },
                    { viewModel.editRecoveryDurationAndStop(180L) },
                )
            for (action in actions) {
                viewModel.showRecoveryPrompt()
                assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
                action()
                advanceUntilIdle()
                assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)
            }
        }

    @Test
    fun `failed prompt dismissal keeps prompt and retries the same interval`() =
        runTest {
            val sessions = MutableStateFlow(original)
            every { repository.observeActiveSession() } returns sessions
            coEvery { repository.refreshActiveSession() } returns original
            coEvery { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") } throws
                IllegalStateException("write failed")
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.dismissRecoveryPrompt()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            assertEquals(R.string.work_session_could_not_save, viewModel.uiState.value.workSessionErrorRes)
            sessions.value = original.copy(recoveryIntervalToken = "new-interval")
            advanceUntilIdle()
            coEvery { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") } returns false
            viewModel.retryWorkSessionAction()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            coVerify(exactly = 2) { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") }
            coVerify(exactly = 0) { repository.markRecoveryPromptShown(original.sessionToken, "new-interval") }
        }

    @Test
    fun `prompt dismissal waits for commit and preserves a newer interval`() =
        runTest {
            val sessions = MutableStateFlow(original)
            every { repository.observeActiveSession() } returns sessions
            coEvery { repository.refreshActiveSession() } returns original
            val reply = CompletableDeferred<Boolean>()
            coEvery { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") } coAnswers {
                reply.await()
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.dismissRecoveryPrompt()
            runCurrent()
            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            sessions.value = original.copy(recoveryIntervalToken = "new-interval")
            runCurrent()
            reply.complete(true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            assertEquals(
                "new-interval",
                viewModel.uiState.value.activeSession
                    ?.recoveryIntervalToken,
            )
        }

    @Test
    fun `successful prompt dismissal closes the matching prompt`() =
        runTest {
            every { repository.observeActiveSession() } returns MutableStateFlow(original)
            coEvery { repository.refreshActiveSession() } returns original
            coEvery { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") } returns true
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.dismissRecoveryPrompt()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `cancelled prompt dismissal preserves the prompt without an error`() =
        runTest {
            every { repository.observeActiveSession() } returns MutableStateFlow(original)
            coEvery { repository.refreshActiveSession() } returns original
            coEvery { repository.markRecoveryPromptShown(original.sessionToken, "old-interval") } throws
                CancellationException("cancelled")
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.dismissRecoveryPrompt()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `late refresh preserves a newer observed recovery interval`() =
        runTest {
            for (oldSnapshot in listOf(null, original.copy(recoveryPromptShown = true))) {
                val sessions = MutableStateFlow<ActiveWorkSession?>(oldSnapshot)
                val reply = CompletableDeferred<ActiveWorkSession?>()
                every { repository.observeActiveSession() } returns sessions
                coEvery { repository.refreshActiveSession() } coAnswers { reply.await() }
                val viewModel = viewModel()
                runCurrent()

                val newer = original.copy(recoveryIntervalToken = "new-interval")
                sessions.value = newer
                runCurrent()
                reply.complete(oldSnapshot)
                advanceUntilIdle()

                assertEquals(newer, viewModel.uiState.value.activeSession)
                assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
            }
        }

    @Test
    fun `failed refresh preserves the observed recovery prompt`() =
        runTest {
            every { repository.observeActiveSession() } returns MutableStateFlow(original)
            coEvery { repository.refreshActiveSession() } throws IOException("synthetic read failure")
            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(original, viewModel.uiState.value.activeSession)
            assertTrue(viewModel.uiState.value.showSessionRecoveryPrompt)
        }
}

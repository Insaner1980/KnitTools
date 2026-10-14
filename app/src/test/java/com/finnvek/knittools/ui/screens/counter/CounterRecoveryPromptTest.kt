package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveWorkSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

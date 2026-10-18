package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.ProjectYarnNote
import com.finnvek.knittools.repository.ProjectYarnNoteEditResult
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectYarnNoteEditViewModelTest : CounterViewModelFixture() {
    private val original = ProjectYarnNote(id = 4, projectId = 7, name = "Wool", savedYarnCardId = 3)
    private val draft = original.copy(name = "Edited")

    @Test
    fun `edit forwards original snapshot and draft without a pro check`() =
        runTest {
            val repository = mockk<ProjectYarnNoteRepository>()
            coEvery { repository.edit(original, draft) } returns ProjectYarnNoteEditResult.Saved
            val model = viewModel(yarnNoteRepositoryOverride = repository)
            advanceUntilIdle()
            assertEquals(ProjectYarnNoteEditResult.Saved, model.editProjectYarnNote(original, draft).await())
            coVerify(exactly = 1) { repository.edit(original, draft) }
            coVerify(exactly = 0) { repository.save(any()) }
        }

    @Test
    fun `wrong project is rejected before persistence`() =
        runTest {
            val repository = mockk<ProjectYarnNoteRepository>()
            val model = viewModel(yarnNoteRepositoryOverride = repository)
            advanceUntilIdle()
            assertEquals(
                ProjectYarnNoteEditResult.Missing,
                model.editProjectYarnNote(original.copy(projectId = 99), draft).await(),
            )
            coVerify(exactly = 0) { repository.edit(any(), any()) }
        }

    @Test
    fun `failure is returned for inline retry and cancellation propagates`() =
        runTest {
            val repository = mockk<ProjectYarnNoteRepository>()
            coEvery { repository.edit(original, draft) } throws IOException("Synthetic failure")
            val model = viewModel(yarnNoteRepositoryOverride = repository)
            advanceUntilIdle()
            assertEquals(ProjectYarnNoteEditResult.Failed, model.editProjectYarnNote(original, draft).await())
            coEvery { repository.edit(original, draft) } throws CancellationException("Synthetic cancellation")
            try {
                model.editProjectYarnNote(original, draft).await()
                error("Cancellation must propagate")
            } catch (_: CancellationException) {
                coVerify(exactly = 2) { repository.edit(original, draft) }
            }
        }

    @Test
    fun `cancelling the UI waiter does not cancel the ViewModel save`() =
        runTest {
            val repository = mockk<ProjectYarnNoteRepository>()
            val pending = CompletableDeferred<ProjectYarnNoteEditResult>()
            coEvery { repository.edit(original, draft) } coAnswers { pending.await() }
            val model = viewModel(yarnNoteRepositoryOverride = repository)
            advanceUntilIdle()
            val save = model.editProjectYarnNote(original, draft)
            val waiter = launch { save.await() }
            advanceUntilIdle()
            waiter.cancel()
            advanceUntilIdle()
            assertFalse(save.isCancelled)
            pending.complete(ProjectYarnNoteEditResult.Saved)
            assertEquals(ProjectYarnNoteEditResult.Saved, save.await())
            coVerify(exactly = 1) { repository.edit(original, draft) }
        }
}

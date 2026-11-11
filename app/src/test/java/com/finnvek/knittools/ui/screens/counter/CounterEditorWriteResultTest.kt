package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.ProjectNotesSaveResult
import com.finnvek.knittools.repository.mergeProjectNotes
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterEditorWriteResultTest : CounterViewModelFixture() {
    @Test
    fun `queued note appends retain their merge base after another project starts editing`() =
        runTest {
            var stored = observedProject.value.copy(notes = "Base", notesCreated = true)
            observedProject.value = stored
            every { repository.getActiveProjects() } returns flowOf(listOf(stored))
            val other = CounterProject(id = 8L, name = "Other", notes = "Other base", notesCreated = true)
            every { repository.observeProject(8L) } returns flowOf(other)
            val release = CompletableDeferred<Unit>()
            var saves = 0
            coEvery { repository.saveProjectNotes(7L, any(), any(), any()) } coAnswers {
                if (++saves == 1) release.await()
                stored = stored.copy(notes = mergeProjectNotes(secondArg(), thirdArg(), stored.notes))
                ProjectNotesSaveResult.Saved(stored)
            }
            coEvery { repository.saveProjectNotes(8L, any(), any(), any()) } coAnswers {
                ProjectNotesSaveResult.Saved(other.copy(notes = thirdArg()))
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setNotes("Base a")
            runCurrent()
            viewModel.setNotes("Base ab")
            runCurrent()
            viewModel.selectProject(other)
            runCurrent()
            viewModel.setNotes("Other draft")
            runCurrent()
            release.complete(Unit)
            advanceUntilIdle()

            assertEquals("Base ab", stored.notes)
            assertEquals(8L, viewModel.uiState.value.projectId)
            assertEquals("Other draft", viewModel.uiState.value.notes)
        }

    @Test
    fun `a newer draft equal to an older request survives intermediate note observations`() =
        runTest {
            var stored = observedProject.value.copy(notes = "Base", notesCreated = true)
            observedProject.value = stored
            every { repository.getActiveProjects() } returns flowOf(listOf(stored))
            val releases = List(3) { CompletableDeferred<Unit>() }
            var saves = 0
            coEvery { repository.saveProjectNotes(7L, any(), any(), any()) } coAnswers {
                releases[saves++].await()
                stored = stored.copy(notes = mergeProjectNotes(secondArg(), thirdArg(), stored.notes))
                observedProject.value = stored
                ProjectNotesSaveResult.Saved(stored)
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setNotes("Base a")
            runCurrent()
            viewModel.setNotes("Base ab")
            viewModel.setNotes("Base a")
            runCurrent()
            releases[0].complete(Unit)
            runCurrent()
            releases[1].complete(Unit)
            runCurrent()

            val pendingDraft = viewModel.uiState.value.notes
            releases[2].complete(Unit)
            advanceUntilIdle()
            assertEquals("Base a", pendingDraft)
            assertEquals("Base a", stored.notes)
            assertEquals("Base a", viewModel.uiState.value.notes)
        }

    @Test
    fun `project detail save preserves a notes draft while its save is pending`() =
        runTest {
            val (project, release) = deferNotesSave()
            coEvery { repository.updateProjectDetails(7L, "Renamed", any(), any(), any()) } returns
                project.copy(name = "Renamed")
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setNotes("Local draft")
            runCurrent()
            viewModel.setProjectDetails("Renamed", project.craftType, project.mainCounterLabelType, null)
            runCurrent()

            assertEquals("Renamed", viewModel.uiState.value.projectName)
            assertEquals("Local draft", viewModel.uiState.value.notes)
            release.complete(Unit)
            advanceUntilIdle()
            assertEquals("Local draft", viewModel.uiState.value.notes)
        }

    @Test
    fun `late project detail result preserves newer observed count target and notes`() =
        runTest {
            val saved = observedProject.value.copy(name = "Renamed", targetRows = 20, notes = "Base")
            val reply = CompletableDeferred<CounterProject?>()
            coEvery { repository.updateProjectDetails(7L, "Renamed", any(), any(), any()) } coAnswers { reply.await() }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setProjectDetails("Renamed", saved.craftType, saved.mainCounterLabelType, null)
            runCurrent()
            observedProject.value = saved.copy(count = 42, targetRows = 80, notes = "Current", notesCreated = true)
            runCurrent()
            reply.complete(saved)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Renamed", state.projectName)
            assertEquals(42, state.counter.count)
            assertEquals(80, state.targetRows)
            assertEquals("Current", state.notes)
            assertEquals(true, state.notesCreated)
        }

    @Test
    fun `project observations preserve a notes draft while its save is pending`() =
        runTest {
            val (project, release) = deferNotesSave()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setNotes("Local draft")
            runCurrent()
            observedProject.value = project.copy(count = 1)
            runCurrent()

            assertEquals("Local draft", viewModel.uiState.value.notes)
            assertEquals(1, viewModel.uiState.value.counter.count)
            release.complete(Unit)
            advanceUntilIdle()
            assertEquals("Local draft", viewModel.uiState.value.notes)
        }

    @Test
    fun `overlapping note appends replace this editors earlier text without duplication`() =
        runTest {
            var stored = observedProject.value.copy(notes = "Base", notesCreated = true)
            observedProject.value = stored
            every { repository.getActiveProjects() } returns flowOf(listOf(stored))
            val transaction = Mutex()
            val firstSave = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            coEvery { repository.saveProjectNotes(7L, any(), any(), any()) } coAnswers {
                val base = secondArg<String>()
                val requested = thirdArg<String>()
                transaction.withLock {
                    if (!firstSave.isCompleted) {
                        firstSave.complete(Unit)
                        release.await()
                    }
                    stored = stored.copy(notes = mergeProjectNotes(base, requested, stored.notes))
                    ProjectNotesSaveResult.Saved(stored)
                }
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setNotes("Base a")
            runCurrent()
            firstSave.await()
            viewModel.setNotes("Base ab")
            runCurrent()
            release.complete(Unit)
            advanceUntilIdle()

            assertEquals("Base ab", stored.notes)
            assertEquals("Base ab", viewModel.uiState.value.notes)
        }

    @Test
    fun `failed notes save reports failure while preserving editable draft for retry`() =
        runTest {
            val project = observedProject.value.copy(notes = "Base", notesCreated = true)
            observedProject.value = project
            every { repository.getActiveProjects() } returns flowOf(listOf(project))
            coEvery { repository.saveProjectNotes(7L, any(), any(), any()) } returns
                ProjectNotesSaveResult.PersistenceFailure
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()

            viewModel.setNotes("Draft")
            advanceUntilIdle()

            assertEquals("Draft", viewModel.uiState.value.notes)
            assertEquals(1, errors.size)
            coEvery { repository.saveProjectNotes(7L, any(), any(), any()) } answers {
                ProjectNotesSaveResult.Saved(
                    project.copy(notes = mergeProjectNotes(secondArg(), thirdArg(), project.notes)),
                )
            }
            viewModel.setNotes("Draft")
            advanceUntilIdle()
            assertEquals("Draft", viewModel.uiState.value.notes)
            assertEquals(1, errors.size)
        }

    @Test
    fun `secondary count exception restores stored count and reports failure`() =
        runTest {
            val project = observedProject.value.copy(secondaryCount = 4, secondaryCounterUsed = true)
            observedProject.value = project
            every { repository.getActiveProjects() } returns flowOf(listOf(project))
            coEvery { repository.updateProjectSecondaryCount(7L, 5) } throws IOException("synthetic failure")
            coEvery { repository.getProject(7L) } returns project
            val viewModel = viewModel()
            val errors = collectErrors(viewModel.projectContentErrors)
            advanceUntilIdle()

            viewModel.incrementSecondary()
            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.secondaryCount)
            assertEquals(1, errors.size)
        }

    private fun deferNotesSave(): Pair<CounterProject, CompletableDeferred<Unit>> {
        val project = observedProject.value.copy(notes = "Base", notesCreated = true)
        observedProject.value = project
        every { repository.getActiveProjects() } returns flowOf(listOf(project))
        val release = CompletableDeferred<Unit>()
        coEvery { repository.saveProjectNotes(7L, any(), any(), any()) } coAnswers {
            release.await()
            ProjectNotesSaveResult.Saved(project.copy(notes = thirdArg()))
        }
        return project to release
    }
}

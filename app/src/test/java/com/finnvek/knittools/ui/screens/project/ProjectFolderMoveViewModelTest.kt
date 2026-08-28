package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderMembership
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.repository.ProjectFolderMutationResult
import com.finnvek.knittools.repository.ProjectFolderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectFolderMoveViewModelTest {
    private val repository = mockk<ProjectFolderRepository>()
    private val snapshot =
        MutableStateFlow(
            ProjectFolderSnapshot(
                listOf(ProjectFolder(2, "Gifts", 0)),
                listOf(ProjectFolderMembership(7, null, true)),
            ),
        )
    private val store = ViewModelStore()
    private lateinit var viewModel: ProjectFolderMoveViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observeOrganization(any()) } returns snapshot
        viewModel = ProjectFolderMoveViewModel(repository)
        store.put("move", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `single move uses stable project identity and emits success only after repository result`() =
        runTest {
            coEvery { repository.moveProjects(setOf(7), 2) } returns ProjectFolderMutationResult.Assigned(7, 2)
            var moved = false
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { viewModel.movedEvents.collect { moved = true } }

            viewModel.prepareProject(7)
            assertEquals(snapshot.value, viewModel.state.value.snapshot)
            viewModel.moveTo(2)

            assertTrue(moved)
            assertFalse(viewModel.state.value.isMutating)
            coVerify(exactly = 1) { repository.moveProjects(setOf(7), 2) }
        }

    @Test
    fun `single move success survives an absent collector and is consumed only once`() =
        runTest {
            val firstMove = CompletableDeferred<ProjectFolderMutationResult>()
            coEvery { repository.moveProjects(setOf(7), 2) } coAnswers { firstMove.await() }

            viewModel.prepareProject(7)
            viewModel.moveTo(2)
            viewModel.prepareProject(9)
            firstMove.complete(ProjectFolderMutationResult.Assigned(7, 2))
            runCurrent()

            val events = mutableListOf<Long>()
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.movedEvents.collect { events += it }
                }
            runCurrent()
            assertEquals(listOf(7L), events)
            collector.cancel()

            val replayedEvents = mutableListOf<Long>()
            val replayCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.movedEvents.collect { replayedEvents += it }
                }
            runCurrent()
            assertTrue(replayedEvents.isEmpty())
            replayCollector.cancel()
        }

    @Test
    fun `repreparing the same project during a move keeps it busy and does not duplicate the write`() =
        runTest {
            val result = CompletableDeferred<ProjectFolderMutationResult>()
            coEvery { repository.moveProjects(setOf(7), 2) } coAnswers { result.await() }

            viewModel.prepareProject(7)
            viewModel.moveTo(2)
            assertTrue(viewModel.state.value.isMutating)

            viewModel.prepareProject(7)

            assertTrue(viewModel.state.value.isMutating)
            viewModel.moveTo(2)
            coVerify(exactly = 1) { repository.moveProjects(setOf(7), 2) }
            result.complete(ProjectFolderMutationResult.Assigned(7, 2))
            runCurrent()
        }

    @Test
    fun `earlier failure does not replace the newer project error before its own move starts`() =
        runTest {
            val firstResult = CompletableDeferred<ProjectFolderMutationResult>()
            val secondResult = CompletableDeferred<ProjectFolderMutationResult>()
            coEvery { repository.moveProjects(setOf(7), 2) } coAnswers { firstResult.await() }
            coEvery { repository.moveProjects(setOf(9), null) } coAnswers { secondResult.await() }

            viewModel.prepareProject(7)
            viewModel.moveTo(2)
            assertTrue(viewModel.state.value.isMutating)
            viewModel.prepareProject(9)
            assertTrue(viewModel.state.value.isMutating)
            viewModel.moveTo(null)
            coVerify(exactly = 0) { repository.moveProjects(setOf(9), null) }

            firstResult.complete(ProjectFolderMutationResult.FolderMissing)
            runCurrent()

            assertFalse(viewModel.state.value.isMutating)
            assertEquals(null, viewModel.state.value.mutationError)
            viewModel.moveTo(null)
            coVerify(exactly = 1) { repository.moveProjects(setOf(9), null) }
            assertTrue(viewModel.state.value.isMutating)
            secondResult.complete(ProjectFolderMutationResult.Unassigned(9))
            runCurrent()
            assertFalse(viewModel.state.value.isMutating)
        }

    @Test
    fun `missing target and failed writes retain the sheet and allow explicit retry`() =
        runTest {
            coEvery { repository.moveProjects(setOf(7), 2) } returns ProjectFolderMutationResult.FolderMissing
            var moved = false
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { viewModel.movedEvents.collect { moved = true } }
            viewModel.prepareProject(7)
            viewModel.moveTo(2)
            assertFalse(moved)
            assertEquals(ProjectFolderMutationResult.FolderMissing, viewModel.state.value.mutationError)

            coEvery { repository.moveProjects(setOf(7), null) } returns
                ProjectFolderMutationResult.AlreadyAssigned(setOf(7))
            viewModel.moveTo(null)
            assertTrue(moved)
            assertEquals(null, viewModel.state.value.mutationError)
        }

    @Test
    fun `reopening for another project does not reuse the old project id or error`() =
        runTest {
            coEvery { repository.moveProjects(setOf(7), 2) } returns ProjectFolderMutationResult.ProjectMissing
            viewModel.prepareProject(7)
            viewModel.moveTo(2)
            snapshot.value = snapshot.value.copy(memberships = listOf(ProjectFolderMembership(9, 2, false)))
            coEvery { repository.moveProjects(setOf(9), null) } returns ProjectFolderMutationResult.Unassigned(9)
            viewModel.prepareProject(9)
            assertEquals(null, viewModel.state.value.mutationError)
            viewModel.moveTo(null)
            coVerify(exactly = 1) { repository.moveProjects(setOf(9), null) }
        }

    @Test
    fun `read failure stays recoverable and retry replaces it with a real snapshot`() =
        runTest {
            var observationCount = 0
            every { repository.observeOrganization(any()) } answers {
                if (observationCount++ == 0) {
                    firstArg<() -> Unit>().invoke()
                    emptyFlow<ProjectFolderSnapshot>()
                } else {
                    snapshot
                }
            }

            viewModel.prepareProject(7)
            assertTrue(viewModel.state.value.readFailed)
            assertTrue(viewModel.state.value.isLoading)

            viewModel.retryLoading()

            assertEquals(snapshot.value, viewModel.state.value.snapshot)
            assertFalse(viewModel.state.value.readFailed)
            assertFalse(viewModel.state.value.isLoading)
            verify(exactly = 2) { repository.observeOrganization(any()) }
        }
}

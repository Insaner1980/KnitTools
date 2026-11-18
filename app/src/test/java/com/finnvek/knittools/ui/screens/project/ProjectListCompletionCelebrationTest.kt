package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCompletionSnapshot
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.ProjectCompletionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListCompletionCelebrationTest : ProjectListViewModelFixture() {
    @Test
    fun `single completion retains committed facts and newest photo until matching event is consumed`() =
        runTest {
            val project = CounterProject(id = 1L, name = "First", count = 5)
            val snapshot = snapshot(project, eventId = 41L)
            every { photoRepository.observeLatestPhotoUris() } returns flowOf(mapOf(1L to "content://photos/newest"))
            coEvery { repository.completeProjectWithSessionChoice(1L, null) } returns
                ProjectCompletionResult.CompletedNow(snapshot)
            val vm = viewModel(project)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.projectLatestPhotoUris.collect {} }

            vm.enterMultiSelectMode(1L)
            vm.completeSelectedProjects()
            runCurrent()

            val expected = ProjectCompletionCelebration(snapshot, "content://photos/newest")
            assertEquals(expected, vm.pendingCompletion.value)
            assertFalse(vm.isMultiSelectMode.value)
            vm.consumeCompletion(40L)
            assertEquals(expected, vm.pendingCompletion.value)
            vm.consumeCompletion(41L)
            assertNull(vm.pendingCompletion.value)
        }

    @Test
    fun `single selection preserves celebration intent through active session choice`() =
        runTest {
            val project = CounterProject(id = 1L, name = "First")
            val vm = viewModel(project)
            val session = session(project.id)
            coEvery { repository.refreshActiveSession() } returns session
            coEvery {
                repository.completeProjectWithSessionChoice(
                    1L,
                    ActiveSessionCompletionChoice.SAVE,
                    expectedSession = session,
                )
            } returns ProjectCompletionResult.CompletedNow(snapshot(project, 41L))

            vm.enterMultiSelectMode(1L)
            vm.completeSelectedProjects()
            runCurrent()
            assertTrue(vm.pendingCompletionSessionAction.value?.celebrateCompletion == true)
            assertNull(vm.pendingCompletion.value)
            vm.resolvePendingCompletion(saveSession = true)
            runCurrent()

            assertEquals(
                41L,
                vm.pendingCompletion.value
                    ?.snapshot
                    ?.eventId,
            )
            assertNull(vm.pendingCompletionSessionAction.value)
            assertFalse(vm.isMultiSelectMode.value)
        }

    @Test
    fun `bulk completion never celebrates when only the last project remains at session confirmation`() =
        runTest {
            val first = CounterProject(id = 1L, name = "First")
            val last = CounterProject(id = 2L, name = "Last")
            val session = session(last.id)
            coEvery { repository.completeProjectWithSessionChoice(1L, null) } returns
                ProjectCompletionResult.CompletedNow(snapshot(first, 41L))
            coEvery { repository.completeProjectWithSessionChoice(2L, null) } returns
                ProjectCompletionResult.NeedsActiveSessionChoice(session)
            coEvery {
                repository.completeProjectWithSessionChoice(
                    2L,
                    ActiveSessionCompletionChoice.DISCARD,
                    expectedSession = session,
                )
            } returns ProjectCompletionResult.CompletedNow(snapshot(last, 42L))
            val vm = viewModel(first, last)

            vm.enterMultiSelectMode(1L)
            vm.toggleProjectSelection(2L)
            vm.completeSelectedProjects()
            runCurrent()

            assertEquals(setOf(2L), vm.selectedProjectIds.value)
            assertEquals(setOf(2L), vm.pendingCompletionSessionAction.value?.projectIds)
            assertFalse(vm.pendingCompletionSessionAction.value?.celebrateCompletion ?: true)
            assertNull(vm.pendingCompletion.value)
            vm.resolvePendingCompletion(saveSession = false)
            runCurrent()

            assertNull(vm.pendingCompletion.value)
            assertFalse(vm.isMultiSelectMode.value)
        }

    @Test
    fun `reopened single project celebrates a distinct event and ignores a stale close`() =
        runTest {
            val project = CounterProject(id = 1L, name = "First")
            coEvery { repository.completeProjectWithSessionChoice(1L, null) } returnsMany
                listOf(
                    ProjectCompletionResult.CompletedNow(snapshot(project, 41L)),
                    ProjectCompletionResult.CompletedNow(snapshot(project, 42L)),
                )
            val vm = viewModel(project)
            vm.enterMultiSelectMode(1L)
            vm.completeSelectedProjects()
            runCurrent()
            vm.consumeCompletion(41L)

            vm.enterMultiSelectMode(1L)
            vm.completeSelectedProjects()
            runCurrent()
            vm.consumeCompletion(41L)

            assertEquals(
                42L,
                vm.pendingCompletion.value
                    ?.snapshot
                    ?.eventId,
            )
        }

    @Test
    fun `failed and idempotent completion results never create celebration events`() =
        runTest {
            val project = CounterProject(id = 1L, name = "First")
            coEvery { repository.completeProjectWithSessionChoice(1L, null) } returnsMany
                listOf(
                    ProjectCompletionResult.PersistenceFailure,
                    ProjectCompletionResult.Completed,
                )
            val vm = viewModel(project)
            vm.enterMultiSelectMode(1L)

            vm.completeSelectedProjects()
            runCurrent()
            assertNull(vm.pendingCompletion.value)
            assertTrue(vm.isMultiSelectMode.value)
            vm.completeSelectedProjects()
            runCurrent()

            assertNull(vm.pendingCompletion.value)
            assertFalse(vm.isMultiSelectMode.value)
        }

    private fun TestScope.viewModel(vararg projects: CounterProject): ProjectListViewModel {
        every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(projects.toList())
        projects.forEach { project -> coEvery { repository.getProject(project.id) } returns project }
        coEvery { repository.refreshActiveSession() } returns null
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.activeProjects.collect {} }
        return vm
    }

    private fun session(projectId: Long): ActiveWorkSession =
        mockk {
            every { this@mockk.projectId } returns projectId
            every { needsRecoveryReview } returns false
        }

    private fun snapshot(
        project: CounterProject,
        eventId: Long,
    ) = ProjectCompletionSnapshot(
        eventId,
        project.copy(count = 37, isCompleted = true, completedAt = 2_000L),
        totalSessionMinutes = 90,
    )
}

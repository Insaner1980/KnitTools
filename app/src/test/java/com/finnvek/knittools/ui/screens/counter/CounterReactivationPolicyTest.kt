package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ProjectReactivationResult
import com.finnvek.knittools.serializedCopy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterReactivationPolicyTest : CounterViewModelFixture() {
    @Test
    fun `same route selected during recreation preserves pending target`() =
        runTest {
            val (active, restored) = pendingReactivationSnapshot()
            val second = viewModel(savedStateHandle = restored)
            second.selectProjectByIdForLaunch(7L)
            runCurrent()
            active.value = emptyList()
            runCurrent()
            coVerify(exactly = 2) { repository.reactivateProject(7L, 100L) }
        }

    @Test
    fun `different route selected during recreation cancels pending target`() =
        runTest {
            val (active, restored) = pendingReactivationSnapshot()
            coEvery { repository.getProject(8L) } returns CounterProject(id = 8L, name = "Other")
            val second = viewModel(savedStateHandle = restored)
            second.selectProjectByIdForLaunch(8L)
            runCurrent()
            active.value = emptyList()
            second.retryPendingReactivation()
            runCurrent()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            assertEquals(8L, second.uiState.value.projectId)
        }

    @Test
    fun `restored pending reactivation resumes when entitlement changes without UI callback`() =
        runTest {
            val (_, restored) = pendingReactivationSnapshot()
            val entitlement = MutableStateFlow(ProState(ProStatus.TRIAL_EXPIRED))
            val pro = mockk<ProManager>()
            every { pro.proState } returns entitlement
            every { pro.hasFeature(any()) } answers {
                entitlement.value.hasFeature(firstArg(), debugUnlockAllFeatures = false)
            }
            val second = viewModel(savedStateHandle = restored, proManagerOverride = pro)
            runCurrent()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            entitlement.value = ProState(ProStatus.TRIAL_ACTIVE)
            runCurrent()
            coVerify(exactly = 2) { repository.reactivateProject(7L, 100L) }
            assertNull(second.uiState.value.reactivationPromptCount)
        }

    private fun TestScope.pendingReactivationSnapshot(): Pair<
        MutableStateFlow<List<CounterProject>>,
        SavedStateHandle,
    > {
        val active = MutableStateFlow(listOf(CounterProject(id = 8L, name = "Active")))
        every { repository.getActiveProjects() } returns active
        every { repository.observeProject(8L) } returns flowOf(active.value.single())
        observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
        coEvery { repository.getProject(7L) } answers { observedProject.value }
        coEvery { repository.getActiveProjectCount() } answers { active.value.size }
        coEvery { repository.reactivateProject(7L, 100L) } returnsMany
            listOf(ProjectReactivationResult.LimitReached, ProjectReactivationResult.Reactivated)
        val handle = SavedStateHandle()
        val first = viewModel(savedStateHandle = handle)
        runCurrent()
        first.selectProject(observedProject.value)
        runCurrent()
        first.reactivateProject()
        runCurrent()
        val restored = handle.serializedCopy()
        ViewModelStore().apply { put("counter", first) }.clear()
        return active to restored
    }

    @Test
    fun `recreated pending reactivation reloads original completed target and waits for free slot`() =
        runTest {
            val (active, restored) = pendingReactivationSnapshot()
            val second = viewModel(savedStateHandle = restored)
            runCurrent()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            active.value = emptyList()
            runCurrent()
            coVerify(exactly = 2) { repository.reactivateProject(7L, 100L) }
            coVerify(exactly = 0) { repository.reactivateProject(8L, any(), any()) }
            assertEquals(7L, second.uiState.value.projectId)
            assertNull(second.uiState.value.reactivationPromptCount)
        }

    @Test
    fun `restored stale target is discarded without reactivating another project`() =
        runTest {
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.reactivateProject(7L, 100L) } returns ProjectReactivationResult.LimitReached
            val handle = SavedStateHandle()
            val first = viewModel(savedStateHandle = handle)
            runCurrent()
            first.reactivateProject()
            runCurrent()
            val snapshot = handle.serializedCopy()
            ViewModelStore().apply { put("counter", first) }.clear()
            for (target in listOf(
                null,
                observedProject.value.copy(completedAt = 101L),
                observedProject.value.copy(isCompleted = false, completedAt = null),
            )) {
                coEvery { repository.getProject(7L) } returns target
                val restored = snapshot.serializedCopy()
                val second = viewModel(savedStateHandle = restored)
                runCurrent()
                second.retryPendingReactivation()
                runCurrent()
                assertNull(second.uiState.value.reactivationPromptCount)
                ViewModelStore().apply { put("counter", second) }.clear()
            }
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
        }

    @Test
    fun `zero observed before limit result is reconciled after the attempt finishes`() =
        runTest { assertFinishingReactivationRetries(waitForCount = false) }

    @Test
    fun `retry signal during finishing count read is retained and retried once`() =
        runTest { assertFinishingReactivationRetries(waitForCount = true) }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    private suspend fun TestScope.assertFinishingReactivationRetries(waitForCount: Boolean) {
        val active = MutableStateFlow(listOf(CounterProject(id = 8L, name = "Active")))
        every { repository.getActiveProjects() } returns active
        every { repository.observeProject(8L) } returns flowOf(active.value.single())
        observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
        // CPD-ON
        val result = CompletableDeferred<ProjectReactivationResult>()
        val countRelease = CompletableDeferred<Unit>()
        var attempts = 0
        coEvery { repository.reactivateProject(7L, 100L) } coAnswers {
            attempts++
            if (attempts == 1) result.await() else ProjectReactivationResult.Reactivated
        }
        coEvery { repository.getActiveProjectCount() } coAnswers {
            if (waitForCount) countRelease.await()
            active.value.size
        }
        val vm = viewModel()
        runCurrent()
        vm.selectProject(observedProject.value)
        runCurrent()
        vm.reactivateProject()
        runCurrent()
        assertEquals(1, attempts)
        if (waitForCount) {
            result.complete(ProjectReactivationResult.LimitReached)
            runCurrent()
        }
        active.value = emptyList()
        runCurrent()
        if (waitForCount) vm.retryPendingReactivation()
        runCurrent()
        assertEquals(1, attempts)
        result.complete(ProjectReactivationResult.LimitReached)
        countRelease.complete(Unit)
        runCurrent()
        assertEquals(2, attempts)
        assertNull(vm.uiState.value.reactivationPromptCount)
        active.value = listOf(CounterProject(id = 8L, name = "Active"))
        runCurrent()
        active.value = emptyList()
        vm.retryPendingReactivation()
        runCurrent()
        assertEquals(2, attempts)
    }

    @Test
    fun `reactivation keeps completion identity and consumes a successful retry once`() =
        runTest {
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.reactivateProject(7L, 100L) } returnsMany
                listOf(
                    ProjectReactivationResult.LimitReached,
                    ProjectReactivationResult.Reactivated,
                )
            val vm = viewModel()
            advanceUntilIdle()
            vm.reactivateProject()
            vm.reactivateProject()
            advanceUntilIdle()
            assertEquals(1, vm.uiState.value.reactivationPromptCount)
            vm.retryPendingReactivation()
            vm.retryPendingReactivation()
            advanceUntilIdle()
            vm.retryPendingReactivation()
            advanceUntilIdle()
            coVerify(exactly = 2) { repository.reactivateProject(7L, 100L) }
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

    @Test
    fun `dismissal prevents a later entitlement callback from reactivating`() =
        runTest {
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            coEvery { repository.reactivateProject(7L, 100L) } returns ProjectReactivationResult.LimitReached
            coEvery { repository.getActiveProjectCount() } returns 1
            val vm = viewModel()
            advanceUntilIdle()
            vm.reactivateProject()
            advanceUntilIdle()
            vm.dismissPendingReactivation()
            vm.retryPendingReactivation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.reactivateProject(7L, 100L) }
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `another completion cycle invalidates the pending reactivation`() =
        runTest {
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            coEvery { repository.reactivateProject(7L, 100L) } returns ProjectReactivationResult.LimitReached
            coEvery { repository.getActiveProjectCount() } returns 1
            val vm = viewModel()
            advanceUntilIdle()
            vm.reactivateProject()
            advanceUntilIdle()
            // CPD-ON
            observedProject.value = observedProject.value.copy(completedAt = 200L)
            advanceUntilIdle()
            vm.retryPendingReactivation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `project navigation discards the original reactivation before loading a new target`() =
        runTest {
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            coEvery { repository.reactivateProject(7L, 100L) } returns ProjectReactivationResult.LimitReached
            coEvery { repository.getActiveProjectCount() } returns 1
            val vm = viewModel()
            advanceUntilIdle()
            vm.reactivateProject()
            advanceUntilIdle()
            // CPD-ON
            val next = CounterProject(id = 8L, name = "Other", isCompleted = true, completedAt = 200L)
            every { repository.observeProject(8L) } returns flowOf(next)
            vm.selectProject(next)
            vm.retryPendingReactivation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            assertEquals(8L, vm.uiState.value.projectId)
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

    @Test
    fun `deletion invalidates a pending reactivation`() =
        runTest {
            val target =
                MutableStateFlow<CounterProject?>(observedProject.value.copy(isCompleted = true, completedAt = 100L))
            every { repository.observeProject(7L) } returns target
            coEvery { repository.reactivateProject(7L, 100L) } returns ProjectReactivationResult.LimitReached
            coEvery { repository.getActiveProjectCount() } returns 1
            val vm = viewModel()
            advanceUntilIdle()
            vm.reactivateProject()
            advanceUntilIdle()
            target.value = null
            advanceUntilIdle()
            vm.retryPendingReactivation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            assertNull(vm.uiState.value.projectId)
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

    @Test
    fun `active count dropping to zero retries original reactivation without a UI entitlement override`() =
        runTest {
            val activeRows = MutableStateFlow(listOf(CounterProject(id = 8L, name = "Active")))
            every { repository.getActiveProjects() } returns activeRows
            every { repository.observeProject(8L) } returns flowOf(activeRows.value.single())
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            coEvery { repository.reactivateProject(7L, 100L) } returnsMany
                listOf(
                    ProjectReactivationResult.LimitReached,
                    ProjectReactivationResult.Reactivated,
                )
            coEvery { repository.getActiveProjectCount() } answers { activeRows.value.size }
            val vm = viewModel()
            advanceUntilIdle()
            vm.selectProject(observedProject.value)
            advanceUntilIdle()
            vm.reactivateProject()
            advanceUntilIdle()
            activeRows.value = emptyList()
            advanceUntilIdle()
            coVerify(exactly = 2) { repository.reactivateProject(7L, 100L) }
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

    @Test
    fun `stale sheet callback cannot reactivate whichever project is now selected`() =
        runTest {
            observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
            val vm = viewModel()
            advanceUntilIdle()
            vm.reactivateProject(projectId = 8L, expectedCompletedAt = 100L)
            vm.reactivateProject(projectId = 7L, expectedCompletedAt = 99L)
            advanceUntilIdle()
            coVerify(exactly = 0) { repository.reactivateProject(any(), any(), any()) }
        }
}

package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType.CROCHET
import com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.repository.ProjectCreationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectCreationPolicyTest : ProjectListViewModelFixture() {
    @Test
    fun `zero observed during stale count resumes the exact pending draft once`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } answers { active.value.size }
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returnsMany
                listOf(ProjectCreationResult.LimitReached, ProjectCreationResult.Created(42L))
            val handle = SavedStateHandle()
            val vm = createViewModel(handle)
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            vm.createProject(
                "Original",
                com.finnvek.knittools.domain.model.CraftType.CROCHET,
                com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM,
                "Circuits",
                12L,
            )
            runCurrent()
            assertEquals(1, vm.projectCreationPromptCount.value)
            val staleCount = CompletableDeferred<Int>()
            var reads = 0
            coEvery { repository.getActiveProjectCount() } coAnswers {
                reads++
                if (reads == 1) staleCount.await() else active.value.size
            }
            active.value = listOf(CounterProject(id = 9L, name = "Changed"))
            runCurrent()
            assertEquals(1, reads)
            repeat(3) {
                active.value = emptyList()
                runCurrent()
                assertEquals(false, staleCount.isCompleted)
                assertEquals(1, reads)
                active.value = listOf(CounterProject(id = 9L, name = "Changed"))
                runCurrent()
            }
            active.value = emptyList()
            runCurrent()
            staleCount.complete(1)
            runCurrent()
            assertEquals(listOf(42L), navigated)
            assertEquals(2, reads)
            assertEquals(null, handle.get<PendingProjectCreation>("pending_project_creation"))
            assertEquals(null, vm.projectCreationPromptCount.value)
            active.value = listOf(CounterProject(id = 10L, name = "Later"))
            runCurrent()
            active.value = emptyList()
            vm.retryPendingProjectCreation()
            runCurrent()
            assertEquals(listOf(42L), navigated)
            coVerify(exactly = 2) {
                repository.createProject("Original", CROCHET, CUSTOM, "Circuits", false, null, 12L)
            }
            coVerify(exactly = 2) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `zero and entitlement observations consume pending creation once`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            val entitlement =
                MutableStateFlow(
                    com.finnvek.knittools.pro
                        .ProState(),
                )
            every { repository.getActiveProjects() } returns active
            every { proManager.proState } returns entitlement
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } answers { entitlement.value.isPro }
            val count = CompletableDeferred<Int>()
            coEvery { repository.getActiveProjectCount() } coAnswers { count.await() }
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                ProjectCreationResult.Created(42L)
            val handle = pendingDraftHandle()
            val vm = createViewModel(handle)
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            runCurrent()
            active.value = emptyList()
            runCurrent()
            entitlement.value =
                com.finnvek.knittools.pro
                    .ProState(status = com.finnvek.knittools.pro.ProStatus.PRO_PURCHASED)
            runCurrent()
            count.complete(1)
            runCurrent()
            vm.retryPendingProjectCreation()
            runCurrent()
            assertEquals(listOf(42L), navigated)
            assertEquals(null, handle.get<PendingProjectCreation>("pending_project_creation"))
            coVerify(exactly = 1) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `dismissal clears draft while zero reconciliation is queued`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            val count = CompletableDeferred<Int>()
            coEvery { repository.getActiveProjectCount() } coAnswers { count.await() }
            val handle = pendingDraftHandle()
            val vm = createViewModel(handle)
            runCurrent()
            active.value = emptyList()
            runCurrent()
            coVerify(exactly = 1) { repository.getActiveProjectCount() }
            vm.dismissPendingProjectCreation()
            count.complete(1)
            runCurrent()
            vm.retryPendingProjectCreation()
            runCurrent()
            assertEquals(null, handle.get<PendingProjectCreation>("pending_project_creation"))
            assertEquals(null, vm.projectCreationPromptCount.value)
            coVerify(exactly = 0) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { repository.getActiveProjectCount() }
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `queued reconciliation stops at unchanged limit and does not loop on query failure`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            val count = CompletableDeferred<Int>()
            // CPD-ON
            var reads = 0
            coEvery { repository.getActiveProjectCount() } coAnswers {
                reads++
                if (reads == 1) count.await() else 1
            }
            val errors = mutableListOf<Throwable>()
            val scope =
                CoroutineScope(
                    SupervisorJob() + StandardTestDispatcher(testScheduler) +
                        CoroutineExceptionHandler { _, e -> errors += e },
                )
            try {
                val actions =
                    ProjectCreationActions(
                        repository,
                        proManager,
                        pendingDraftHandle(),
                        scope,
                    ) { error("Unexpected creation") }
                runCurrent()
                active.value = emptyList()
                runCurrent()
                count.complete(1)
                runCurrent()
                assertEquals(2, reads)
                assertEquals(1, actions.projectCreationPromptCount.value)
                coEvery { repository.getActiveProjectCount() } coAnswers {
                    reads++
                    error("Count failed")
                }
                active.value = listOf(CounterProject(id = 10L, name = "Another"))
                runCurrent()
                assertEquals(3, reads)
                assertEquals(listOf("Count failed"), errors.map { it.message })
                runCurrent()
                assertEquals(3, reads)
                coVerify(exactly = 0) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun `zero observed during automatic create retries exact draft once`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            var reads = 0
            coEvery { repository.getActiveProjectCount() } coAnswers {
                reads++
                active.value.size
            }
            val heldCreate = CompletableDeferred<ProjectCreationResult>()
            var attempts = 0
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
                attempts++
                if (attempts == 1) heldCreate.await() else ProjectCreationResult.Created(42L)
            }
            val handle = pendingDraftHandle()
            val vm = createViewModel(handle)
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            runCurrent()
            assertEquals(1, vm.projectCreationPromptCount.value)
            active.value = emptyList()
            runCurrent()
            assertEquals(1, attempts)
            val readsBeforeSignals = reads
            repeat(3) {
                active.value = listOf(CounterProject(id = 10L, name = "Competing"))
                runCurrent()
                active.value = emptyList()
                runCurrent()
            }
            assertEquals(false, heldCreate.isCompleted)
            assertEquals(1, attempts)
            assertEquals(readsBeforeSignals, reads)
            heldCreate.complete(ProjectCreationResult.LimitReached)
            runCurrent()
            assertEquals("Queued free slot must retry the original draft", listOf(42L), navigated)
            assertEquals(readsBeforeSignals + 2, reads)
            assertEquals(null, handle.get<PendingProjectCreation>("pending_project_creation"))
            assertEquals(null, vm.projectCreationPromptCount.value)
            vm.retryPendingProjectCreation()
            active.value = listOf(CounterProject(id = 11L, name = "Later"))
            runCurrent()
            // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
            active.value = emptyList()
            runCurrent()
            assertEquals(listOf(42L), navigated)
            coVerify(exactly = 2) {
                repository.createProject("Original", CROCHET, CUSTOM, "Circuits", false, null, 12L)
            }
            coVerify(exactly = 2) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `automatic limit without a new signal stays gated`() = assertAutomaticCreationStops(signal = false)
    // CPD-ON

    @Test
    fun `dismissal during automatic create discards queued signals`() = assertAutomaticCreationStops(dismiss = true)

    @Test
    fun `successful automatic create consumes queued signals`() =
        assertAutomaticCreationStops(result = ProjectCreationResult.Created(42L))

    @Test
    fun `automatic persistence failure does not retry queued signals`() =
        assertAutomaticCreationStops(failure = IllegalStateException("Persistence failed"))

    @Test
    fun `automatic cancellation does not retry queued signals`() =
        assertAutomaticCreationStops(failure = kotlinx.coroutines.CancellationException("Cancelled"))

    @Test
    fun `automatic limit count failure does not retry queued signals`() =
        assertAutomaticCreationStops(failure = IllegalStateException("Count failed"), failCount = true)

    private fun assertAutomaticCreationStops(
        signal: Boolean = true,
        dismiss: Boolean = false,
        result: ProjectCreationResult = ProjectCreationResult.LimitReached,
        failure: Throwable? = null,
        failCount: Boolean = false,
        // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    ) = runTest {
        val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
        every { repository.getActiveProjects() } returns active
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
        coEvery { repository.getActiveProjectCount() } answers { active.value.size }
        val heldCreate = CompletableDeferred<ProjectCreationResult>()
        // CPD-ON
        coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } coAnswers
            { heldCreate.await() }
        val errors = mutableListOf<Throwable>()
        val scope =
            CoroutineScope(
                SupervisorJob() + StandardTestDispatcher(testScheduler) +
                    CoroutineExceptionHandler { _, error -> errors += error },
            )
        val handle = pendingDraftHandle()
        val navigated = mutableListOf<Long>()
        try {
            val actions = ProjectCreationActions(repository, proManager, handle, scope) { navigated += it }
            runCurrent()
            assertEquals(1, actions.projectCreationPromptCount.value)
            active.value = emptyList()
            runCurrent()
            coVerify(exactly = 1) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
            if (signal) {
                active.value = listOf(CounterProject(id = 9L, name = "Active"))
                runCurrent()
                active.value = emptyList()
                runCurrent()
            }
            if (dismiss) actions.dismissPendingProjectCreation()
            if (failCount) coEvery { repository.getActiveProjectCount() } throws requireNotNull(failure)
            if (failure == null || failCount) heldCreate.complete(result) else heldCreate.completeExceptionally(failure)
            runCurrent()
            runCurrent()
            coVerify(exactly = 1) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
            val created = result is ProjectCreationResult.Created && !dismiss && failure == null
            assertEquals(if (created) listOf(42L) else emptyList<Long>(), navigated)
            val expectedPrompt =
                when {
                    dismiss || created -> null
                    failure == null -> 0
                    else -> 1
                }
            assertEquals(expectedPrompt, actions.projectCreationPromptCount.value)
            assertEquals(dismiss || created, handle.get<Any>("pending_project_creation") == null)
            val expectedErrors =
                listOfNotNull(failure).filterNot { it is kotlinx.coroutines.CancellationException }.map { it.message }
            assertEquals(expectedErrors, errors.map { it.message })
        } finally {
            scope.cancel()
        }
    }

    private fun pendingDraftHandle() =
        SavedStateHandle(
            mapOf(
                "pending_project_creation" to
                    PendingProjectCreation(
                        "Original",
                        com.finnvek.knittools.domain.model.CraftType.CROCHET,
                        com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM,
                        "Circuits",
                        12L,
                    ),
            ),
        )

    @Test
    fun `dismissal while retry count is loading cannot restore the obsolete prompt`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                ProjectCreationResult.LimitReached
            val vm = createViewModel()
            vm.createProject(
                "Original",
                com.finnvek.knittools.domain.model.CraftType.KNITTING,
                com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                null,
            )
            runCurrent()
            assertEquals(1, vm.projectCreationPromptCount.value)
            val count = CompletableDeferred<Int>()
            coEvery { repository.getActiveProjectCount() } coAnswers { count.await() }
            vm.retryPendingProjectCreation()
            runCurrent()
            vm.dismissPendingProjectCreation()
            count.complete(1)
            runCurrent()
            assertEquals(null, vm.projectCreationPromptCount.value)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `creation reconciles a slot opened before limit result arrives`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } answers { active.value.size }
            val firstResult = CompletableDeferred<ProjectCreationResult>()
            var attempts = 0
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
                attempts++
                if (attempts == 1) firstResult.await() else ProjectCreationResult.Created(42L)
                // CPD-ON
            }
            val vm = createViewModel()
            vm.createProject(
                "Original",
                com.finnvek.knittools.domain.model.CraftType.KNITTING,
                com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                null,
            )
            runCurrent()
            active.value = emptyList()
            runCurrent()
            firstResult.complete(ProjectCreationResult.LimitReached)
            runCurrent()
            assertEquals(2, attempts)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `dismissed opening and draft do not resume on a later free slot`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } answers { active.value.size }
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                // CPD-ON
                ProjectCreationResult.LimitReached
            val vm = createViewModel()
            var dialogs = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.showCreateProjectDialog.collect { dialogs++ }
            }
            vm.requestProjectCreation()
            vm.dismissPendingProjectCreation()
            active.value = emptyList()
            runCurrent()
            assertEquals(0, dialogs)
            active.value = listOf(CounterProject(id = 9L, name = "Active"))
            vm.createProject(
                "Dismissed",
                com.finnvek.knittools.domain.model.CraftType.KNITTING,
                com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                null,
            )
            vm.dismissPendingProjectCreation()
            active.value = emptyList()
            vm.retryPendingProjectCreation()
            runCurrent()
            coVerify(exactly = 1) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
            assertEquals(null, vm.projectCreationPromptCount.value)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `pending opening resumes once when unfiltered active projects become empty`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } answers { active.value.size }
            // CPD-ON
            val vm = createViewModel()
            var dialogs = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.showCreateProjectDialog.collect { dialogs++ }
            }
            vm.requestProjectCreation()
            assertEquals(0, dialogs)
            active.value = emptyList()
            runCurrent()
            assertEquals(1, dialogs)
            active.value = listOf(CounterProject(id = 10L, name = "Another"))
            active.value = emptyList()
            runCurrent()
            assertEquals(1, dialogs)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `pending draft resumes exact creation when free slot opens`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 9L, name = "Active")))
            every { repository.getActiveProjects() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } answers { active.value.size }
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returnsMany
                listOf(ProjectCreationResult.LimitReached, ProjectCreationResult.Created(42L))
            val vm = createViewModel()
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            vm.createProject(
                "Original",
                com.finnvek.knittools.domain.model.CraftType.CROCHET,
                com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM,
                "Circuits",
                12L,
            )
            // CPD-ON
            active.value = emptyList()
            runCurrent()
            assertEquals(listOf(42L), navigated)
            coVerify(exactly = 2) {
                repository.createProject(
                    "Original",
                    com.finnvek.knittools.domain.model.CraftType.CROCHET,
                    com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM,
                    "Circuits",
                    false,
                    null,
                    12L,
                )
            }
        }
}

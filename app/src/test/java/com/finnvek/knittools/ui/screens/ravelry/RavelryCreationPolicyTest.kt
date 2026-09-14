package com.finnvek.knittools.ui.screens.ravelry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.serializedCopy
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RavelryCreationPolicyTest : RavelryViewModelFixture() {
    @Test
    fun `zero observed during stale count resumes original pattern once despite changed detail`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            coEvery { repository.getActiveProjectCount() } answers { active.value }
            val original = PatternDetail(id = 42, name = "Pattern A")
            val other = PatternDetail(id = 43, name = "Pattern B")
            coEvery { repository.getPatternDetail(42) } returns original
            coEvery { repository.getPatternDetail(43) } returns other
            coEvery { repository.createProjectFromPattern(original, false) } returnsMany
                listOf(ProjectCreationResult.LimitReached, ProjectCreationResult.Created(7L))
            val handle = SavedStateHandle()
            val vm = createViewModel(false, handle)
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            vm.loadDetail(42)
            runCurrent()
            // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
            vm.createProjectFromPattern()
            runCurrent()
            assertEquals(1, vm.projectCreationPromptCount.value)
            val staleCount = CompletableDeferred<Int>()
            var reads = 0
            coEvery { repository.getActiveProjectCount() } coAnswers {
                reads++
                if (reads == 1) staleCount.await() else active.value
                // CPD-ON
            }
            active.value = 2
            runCurrent()
            assertEquals(1, reads)
            vm.loadDetail(43)
            runCurrent()
            repeat(3) {
                active.value = 0
                runCurrent()
                assertEquals(false, staleCount.isCompleted)
                assertEquals(1, reads)
                active.value = 1
                runCurrent()
            }
            active.value = 0
            runCurrent()
            staleCount.complete(1)
            runCurrent()
            assertEquals(listOf(7L), navigated)
            assertEquals(2, reads)
            assertNull(handle.get<String>("ravelryPendingProjectPattern"))
            assertNull(vm.projectCreationPromptCount.value)
            active.value = 1
            runCurrent()
            active.value = 0
            vm.createProjectFromPattern(retryPending = true)
            runCurrent()
            assertEquals(listOf(7L), navigated)
            coVerify(exactly = 2) { repository.createProjectFromPattern(original, false) }
            coVerify(exactly = 0) { repository.createProjectFromPattern(other, any()) }
            coVerify(exactly = 1) { repository.getPatternDetail(42) }
        }

    @Test
    fun `zero and entitlement observations consume pending pattern once`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            val entitlement =
                MutableStateFlow(
                    com.finnvek.knittools.pro
                        .ProState(),
                )
            every { repository.observeActiveProjectCount() } returns active
            every { proManager.proState } returns entitlement
            val count = CompletableDeferred<Int>()
            coEvery { repository.getActiveProjectCount() } coAnswers { count.await() }
            coEvery { repository.createProjectFromPattern(any(), any()) } returns ProjectCreationResult.Created(7L)
            val handle = pendingPatternHandle()
            val vm = createViewModel(false, handle)
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } answers { entitlement.value.isPro }
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            runCurrent()
            active.value = 0
            runCurrent()
            entitlement.value =
                com.finnvek.knittools.pro
                    .ProState(status = com.finnvek.knittools.pro.ProStatus.PRO_PURCHASED)
            runCurrent()
            count.complete(1)
            runCurrent()
            vm.createProjectFromPattern(retryPending = true)
            runCurrent()
            assertEquals(listOf(7L), navigated)
            assertNull(handle.get<String>("ravelryPendingProjectPattern"))
            coVerify(
                exactly = 1,
            ) { repository.createProjectFromPattern(PatternDetail(id = 42, name = "Pattern A"), true) }
            coVerify(exactly = 1) { repository.createProjectFromPattern(any(), any()) }
        }

    @Test
    fun `dismissal clears pattern while zero reconciliation is queued`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            val count = CompletableDeferred<Int>()
            coEvery { repository.getActiveProjectCount() } coAnswers { count.await() }
            val handle = pendingPatternHandle()
            val vm = createViewModel(false, handle)
            runCurrent()
            active.value = 0
            runCurrent()
            coVerify(exactly = 1) { repository.getActiveProjectCount() }
            vm.projectCreationActions.dismissPendingProjectCreation()
            count.complete(1)
            runCurrent()
            vm.createProjectFromPattern(retryPending = true)
            runCurrent()
            assertNull(handle.get<String>("ravelryPendingProjectPattern"))
            assertNull(vm.projectCreationPromptCount.value)
            coVerify(exactly = 0) { repository.createProjectFromPattern(any(), any()) }
            coVerify(exactly = 1) { repository.getActiveProjectCount() }
        }

    @Test
    fun `queued reconciliation stops at unchanged limit and does not loop on query failure`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
            every { repository.observeActiveProjectCount() } returns active
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            val count = CompletableDeferred<Int>()
            var reads = 0
            coEvery { repository.getActiveProjectCount() } coAnswers {
                reads++
                if (reads == 1) count.await() else 1
            }
            val errors = mutableListOf<Throwable>()
            val scope =
                CoroutineScope(
                    SupervisorJob() + testDispatcher + CoroutineExceptionHandler { _, e -> errors += e },
                    // CPD-ON
                )
            try {
                val actions = RavelryProjectCreationActions(repository, proManager, pendingPatternHandle(), scope)
                runCurrent()
                active.value = 0
                // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
                runCurrent()
                count.complete(1)
                runCurrent()
                assertEquals(2, reads)
                assertEquals(1, actions.projectCreationPromptCount.value)
                coEvery { repository.getActiveProjectCount() } coAnswers {
                    reads++
                    error("Count failed")
                }
                active.value = 1
                // CPD-ON
                runCurrent()
                assertEquals(3, reads)
                assertEquals(listOf("Count failed"), errors.map { it.message })
                runCurrent()
                assertEquals(3, reads)
                coVerify(exactly = 0) { repository.createProjectFromPattern(any(), any()) }
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun `zero observed during automatic create retries pattern A once despite displayed B`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            var reads = 0
            coEvery { repository.getActiveProjectCount() } coAnswers {
                reads++
                active.value
            }
            val original = PatternDetail(id = 42, name = "Pattern A")
            val other = PatternDetail(id = 43, name = "Pattern B")
            coEvery { repository.getPatternDetail(43) } returns other
            val heldCreate = CompletableDeferred<ProjectCreationResult>()
            var attempts = 0
            coEvery { repository.createProjectFromPattern(original, false) } coAnswers {
                attempts++
                if (attempts == 1) heldCreate.await() else ProjectCreationResult.Created(7L)
            }
            val handle = pendingPatternHandle()
            val vm = createViewModel(false, handle)
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            runCurrent()
            assertEquals(1, vm.projectCreationPromptCount.value)
            active.value = 0
            runCurrent()
            assertEquals(1, attempts)
            vm.loadDetail(43)
            runCurrent()
            val readsBeforeSignals = reads
            repeat(3) {
                active.value = 1
                runCurrent()
                active.value = 0
                runCurrent()
            }
            assertEquals(false, heldCreate.isCompleted)
            assertEquals(1, attempts)
            assertEquals(readsBeforeSignals, reads)
            heldCreate.complete(ProjectCreationResult.LimitReached)
            runCurrent()
            assertEquals("Queued free slot must retry Pattern A", listOf(7L), navigated)
            assertEquals(readsBeforeSignals + 2, reads)
            assertNull(handle.get<String>("ravelryPendingProjectPattern"))
            assertNull(vm.projectCreationPromptCount.value)
            vm.createProjectFromPattern(retryPending = true)
            active.value = 1
            runCurrent()
            active.value = 0
            // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
            runCurrent()
            assertEquals(listOf(7L), navigated)
            coVerify(exactly = 2) { repository.createProjectFromPattern(original, false) }
            coVerify(exactly = 0) { repository.createProjectFromPattern(other, any()) }
            coVerify(exactly = 0) { repository.getPatternDetail(42) }
        }

    @Test
    fun `automatic limit without a new signal stays gated`() = assertAutomaticCreationStops(signal = false)

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
    ) = runTest {
        val active = MutableStateFlow(1)
        // CPD-ON
        every { repository.observeActiveProjectCount() } returns active
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
        coEvery { repository.getActiveProjectCount() } answers { active.value }
        val heldCreate = CompletableDeferred<ProjectCreationResult>()
        // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
        coEvery { repository.createProjectFromPattern(any(), any()) } coAnswers { heldCreate.await() }
        val errors = mutableListOf<Throwable>()
        val scope =
            CoroutineScope(
                SupervisorJob() + StandardTestDispatcher(testScheduler) +
                    CoroutineExceptionHandler { _, error -> errors += error },
            )
        val handle = pendingPatternHandle()
        val navigated = mutableListOf<Long>()
        // CPD-ON
        try {
            val actions = RavelryProjectCreationActions(repository, proManager, handle, scope)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actions.navigateToProject.collect { navigated += it }
            }
            runCurrent()
            assertEquals(1, actions.projectCreationPromptCount.value)
            active.value = 0
            runCurrent()
            coVerify(exactly = 1) { repository.createProjectFromPattern(any(), any()) }
            if (signal) {
                active.value = 1
                runCurrent()
                active.value = 0
                // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
                runCurrent()
            }
            if (dismiss) actions.dismissPendingProjectCreation()
            if (failCount) coEvery { repository.getActiveProjectCount() } throws requireNotNull(failure)
            if (failure == null || failCount) heldCreate.complete(result) else heldCreate.completeExceptionally(failure)
            runCurrent()
            runCurrent()
            coVerify(exactly = 1) { repository.createProjectFromPattern(any(), any()) }
            // CPD-ON
            val created = result is ProjectCreationResult.Created && !dismiss && failure == null
            assertEquals(if (created) listOf(42L) else emptyList<Long>(), navigated)
            assertEquals(
                if (dismiss ||
                    created
                ) {
                    null
                } else if (failure == null) {
                    0
                } else {
                    1
                },
                actions.projectCreationPromptCount.value,
            )
            assertEquals(dismiss || created, handle.get<Any>("ravelryPendingProjectPattern") == null)
            assertEquals(emptyList<Throwable>(), errors)
        } finally {
            scope.cancel()
        }
    }

    private fun pendingPatternHandle() =
        SavedStateHandle(
            mapOf(
                "ravelryPendingProjectPattern" to
                    Json.encodeToString(PatternDetail.serializer(), PatternDetail(id = 42, name = "Pattern A")),
            ),
        )

    @Test
    fun `dismissal while retry count is loading cannot restore the obsolete prompt`() =
        runTest(testDispatcher) {
            coEvery { repository.getActiveProjectCount() } returns 1
            val original = PatternDetail(id = 42, name = "Original")
            coEvery { repository.getPatternDetail(42) } returns original
            coEvery { repository.createProjectFromPattern(original, false) } returns ProjectCreationResult.LimitReached
            val vm = createViewModel(false)
            vm.loadDetail(42)
            runCurrent()
            vm.createProjectFromPattern()
            runCurrent()
            assertEquals(1, vm.projectCreationPromptCount.value)
            val count = CompletableDeferred<Int>()
            coEvery { repository.getActiveProjectCount() } coAnswers { count.await() }
            vm.createProjectFromPattern(retryPending = true)
            runCurrent()
            vm.projectCreationActions.dismissPendingProjectCreation()
            count.complete(1)
            runCurrent()
            assertNull(vm.projectCreationPromptCount.value)
        }

    @Test
    fun `saved creation snapshot preserves metadata without pattern instructions`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            coEvery { repository.getActiveProjectCount() } answers { active.value }
            val creation =
                PatternDetail(
                    id = 42,
                    name = "Original",
                    permalink = "original",
                    designer =
                        com.finnvek.knittools.data.remote
                            .PatternDesigner("Designer"),
                    photos =
                        listOf(
                            com.finnvek.knittools.data.remote
                                .PatternPhoto(mediumUrl = "https://example.com/a.jpg"),
                        ),
                    difficultyAverage = 3.5f,
                    availability = com.finnvek.knittools.domain.model.PatternAvailability.Paid,
                    canonicalUrl = "https://www.ravelry.com/patterns/library/original",
                    originalUrl = "https://www.ravelry.com/patterns/library/original?source=share",
                    rowGauge = 24f,
                    yardage = 500,
                    patternNeedleSizes =
                        listOf(
                            com.finnvek.knittools.data.remote
                                .NeedleSize(name = "4 mm"),
                        ),
                    yarnWeight =
                        com.finnvek.knittools.data.remote
                            .YarnWeightInfo("DK"),
                )
            val displayed =
                creation.copy(
                    notes = "Private instructions".repeat(1000),
                    notesHtml = "<p>Instructions</p>",
                )
            coEvery { repository.getPatternDetail(42) } returns displayed
            coEvery { repository.createProjectFromPattern(displayed, false) } returns ProjectCreationResult.LimitReached
            coEvery { repository.createProjectFromPattern(creation, false) } returns ProjectCreationResult.Created(7L)
            val handle = SavedStateHandle()
            val first = createViewModel(false, handle)
            first.loadDetail(42)
            runCurrent()
            first.createProjectFromPattern()
            runCurrent()
            val restored = handle.serializedCopy()
            ViewModelStore().apply { put("ravelry", first) }.clear()
            val second = createViewModel(false, restored)
            runCurrent()
            active.value = 0
            runCurrent()
            coVerify(exactly = 1) { repository.createProjectFromPattern(creation, false) }
            assertNull(second.projectCreationPromptCount.value)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `creation reconciles a slot opened before limit result arrives`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            coEvery { repository.getActiveProjectCount() } answers { active.value }
            val original = PatternDetail(id = 42, name = "Original")
            // CPD-ON
            coEvery { repository.getPatternDetail(42) } returns original
            val firstResult = CompletableDeferred<ProjectCreationResult>()
            var attempts = 0
            coEvery { repository.createProjectFromPattern(original, false) } coAnswers {
                attempts++
                if (attempts == 1) firstResult.await() else ProjectCreationResult.Created(7L)
            }
            val vm = createViewModel(false)
            vm.loadDetail(42)
            runCurrent()
            vm.createProjectFromPattern()
            runCurrent()
            active.value = 0
            runCurrent()
            firstResult.complete(ProjectCreationResult.LimitReached)
            runCurrent()
            assertEquals(2, attempts)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `recreated creation retains original inputs while another pattern is current`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            coEvery { repository.getActiveProjectCount() } answers { active.value }
            val original = PatternDetail(id = 42, name = "Original", permalink = "original")
            val other = PatternDetail(id = 43, name = "Other")
            coEvery { repository.getPatternDetail(42) } returns original
            coEvery { repository.getPatternDetail(43) } returns other
            coEvery { repository.createProjectFromPattern(original, false) } returnsMany
                listOf(ProjectCreationResult.LimitReached, ProjectCreationResult.Created(7L))
            val handle = SavedStateHandle()
            val first = createViewModel(false, handle)
            first.loadDetail(42)
            runCurrent()
            first.createProjectFromPattern()
            runCurrent()
            val restored = handle.serializedCopy()
            ViewModelStore().apply { put("ravelry", first) }.clear()
            val second = createViewModel(false, restored)
            runCurrent()
            // CPD-ON
            coVerify(exactly = 1) { repository.createProjectFromPattern(any(), any()) }
            second.loadDetail(43)
            runCurrent()
            active.value = 0
            runCurrent()
            second.createProjectFromPattern(retryPending = true)
            // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
            runCurrent()
            coVerify(exactly = 2) { repository.createProjectFromPattern(original, false) }
            coVerify(exactly = 0) { repository.createProjectFromPattern(other, any()) }
            coVerify(exactly = 1) { repository.getPatternDetail(42) }
        }

    @Test
    fun `dismissal before recreation removes the saved creation intent`() =
        runTest(testDispatcher) {
            val active = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns active
            coEvery { repository.getActiveProjectCount() } answers { active.value }
            val original = PatternDetail(id = 42, name = "Original")
            // CPD-ON
            coEvery { repository.getPatternDetail(42) } returns original
            coEvery { repository.createProjectFromPattern(original, false) } returns ProjectCreationResult.LimitReached
            val handle = SavedStateHandle()
            val first = createViewModel(false, handle)
            first.loadDetail(42)
            runCurrent()
            first.createProjectFromPattern()
            runCurrent()
            first.projectCreationActions.dismissPendingProjectCreation()
            val restored = handle.serializedCopy()
            ViewModelStore().apply { put("ravelry", first) }.clear()
            val second = createViewModel(false, restored)
            active.value = 0
            runCurrent()
            second.createProjectFromPattern(retryPending = true)
            runCurrent()
            coVerify(exactly = 1) { repository.createProjectFromPattern(any(), any()) }
        }

    @Test
    fun `dismissed pattern creation cannot resume on free slot or entitlement callback`() =
        runTest(testDispatcher) {
            val activeCount = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns activeCount
            coEvery { repository.getActiveProjectCount() } answers { activeCount.value }
            // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
            val original = PatternDetail(id = 42, name = "Original")
            coEvery { repository.getPatternDetail(42) } returns original
            coEvery { repository.createProjectFromPattern(original, false) } returns ProjectCreationResult.LimitReached
            val vm = createViewModel(false)
            vm.loadDetail(42)
            // CPD-ON
            advanceUntilIdle()
            vm.createProjectFromPattern()
            advanceUntilIdle()
            vm.projectCreationActions.dismissPendingProjectCreation()
            activeCount.value = 0
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            vm.createProjectFromPattern(retryPending = true)
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.createProjectFromPattern(any(), any()) }
            assertNull(vm.projectCreationPromptCount.value)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `free slot resumes original pending pattern once despite changed detail`() =
        runTest(testDispatcher) {
            val activeCount = MutableStateFlow(1)
            every { repository.observeActiveProjectCount() } returns activeCount
            coEvery { repository.getActiveProjectCount() } answers { activeCount.value }
            val original = PatternDetail(id = 42, name = "Original")
            val other = PatternDetail(id = 43, name = "Other")
            coEvery { repository.getPatternDetail(42) } returns original
            coEvery { repository.getPatternDetail(43) } returns other
            coEvery { repository.createProjectFromPattern(original, false) } returnsMany
                listOf(ProjectCreationResult.LimitReached, ProjectCreationResult.Created(7L))
            val vm = createViewModel(false)
            // CPD-ON
            val navigated = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigated += it }
            }
            vm.loadDetail(42)
            advanceUntilIdle()
            vm.createProjectFromPattern()
            advanceUntilIdle()
            vm.loadDetail(43)
            advanceUntilIdle()
            activeCount.value = 0
            advanceUntilIdle()
            assertEquals(listOf(7L), navigated)
            activeCount.value = 1
            advanceUntilIdle()
            activeCount.value = 0
            advanceUntilIdle()
            coVerify(exactly = 2) { repository.createProjectFromPattern(original, false) }
            coVerify(exactly = 0) { repository.createProjectFromPattern(other, any()) }
        }
}

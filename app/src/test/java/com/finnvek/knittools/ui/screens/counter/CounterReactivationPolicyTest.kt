package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.PatternAnnotationLayer
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentFileAvailability
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectReactivationResult
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.serializedCopy
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterReactivationPolicyTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val layers = MutableStateFlow(listOf(layer(41L, active = true), layer(42L, active = false)))
    private val observedProject = MutableStateFlow(CounterProject(id = 7L, name = "Project"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        every { ProcessLifecycleOwner.get() } returns mockk<LifecycleOwner>(relaxed = true)
        coEvery { CounterWidgetState.syncAll(any(), any()) } returns Unit
        val project = CounterProject(id = 7L, name = "Project")
        observedProject.value = project
        every { repository.getActiveProjects() } returns flowOf(listOf(project))
        every { repository.observeProject(7L) } returns observedProject
        every { repository.observeActiveSession() } returns flowOf(null)
        coEvery { repository.refreshActiveSession() } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        Dispatchers.resetMain()
    }

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

    private suspend fun TestScope.assertFinishingReactivationRetries(waitForCount: Boolean) {
        val active = MutableStateFlow(listOf(CounterProject(id = 8L, name = "Active")))
        every { repository.getActiveProjects() } returns active
        every { repository.observeProject(8L) } returns flowOf(active.value.single())
        observedProject.value = observedProject.value.copy(isCompleted = true, completedAt = 100L)
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
            observedProject.value = observedProject.value.copy(completedAt = 200L)
            advanceUntilIdle()
            vm.retryPendingReactivation()
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.reactivateProject(any(), any(), any()) }
            assertNull(vm.uiState.value.reactivationPromptCount)
        }

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

    private fun TestScope.viewModel(
        savedPatternRows: Flow<List<SavedPattern>> = flowOf(emptyList()),
        countersOverride: ProjectCounterRepository? = null,
        proManagerOverride: ProManager? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): CounterViewModel {
        val preferences = mockk<PreferencesManager>()
        every { preferences.preferences } returns emptyFlow()
        val proManager = proManagerOverride ?: mockk<ProManager>()
        if (proManagerOverride == null) {
            every { proManager.proState } returns MutableStateFlow(ProState())
            every { proManager.hasFeature(any()) } returns false
        }
        val yarnRepository = mockk<YarnCardRepository>()
        every { yarnRepository.getAllCards() } returns emptyFlow()
        val savedPatterns = mockk<SavedPatternRepository>()
        every { savedPatterns.getAll() } returns savedPatternRows
        val reminders = mockk<ReminderRepository>()
        every { reminders.getRemindersForProject(any()) } returns flowOf(emptyList())
        val counters = countersOverride ?: mockk<ProjectCounterRepository>()
        if (countersOverride == null) {
            every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
        }
        val photos = mockk<ProgressPhotoRepository>()
        every { photos.getLatestPhotos(any()) } returns flowOf(emptyList())
        every { photos.getPhotosForProject(any()) } returns flowOf(emptyList())
        val yarnNotes = mockk<ProjectYarnNoteRepository>()
        every { yarnNotes.observeForProject(any()) } returns flowOf(emptyList())
        val documentDao = mockk<ProjectDocumentDao>()
        every { documentDao.observeForProject(any()) } returns flowOf(listOf(document(41L), document(42L)))
        val layerRepository = mockk<PatternAnnotationLayerRepository>()
        every { layerRepository.observeLayers(any()) } returns layers
        val availability = mockk<ProjectDocumentFileAvailability>()
        coEvery { availability.isAvailable(any()) } returns true
        val documents =
            ProjectDocumentRepository(documentDao, mockk(), savedPatterns, layerRepository, mockk(), availability)
        return CounterViewModel(
            repository = repository,
            reminderRepository = reminders,
            projectCounterRepository = counters,
            photoRepository = photos,
            projectYarnNoteRepository = yarnNotes,
            preferencesManager = preferences,
            proManager = proManager,
            yarnCardRepository = yarnRepository,
            savedPatternRepository = savedPatterns,
            projectDocumentRepository = documents,
            patternDocumentStorage = mockk(),
            inAppReviewManager = mockk(),
            savedStateHandle = savedStateHandle,
            context = mockk<Context>(relaxed = true),
            ioDispatcher = dispatcher,
            applicationScope = backgroundScope,
        )
    }

    private fun document(id: Long) =
        ProjectDocumentEntity(
            id = id,
            projectId = 7L,
            savedPatternId = null,
            documentKey = "local:$id",
            label = "Document $id",
            localPdfUri = "content://pattern/$id",
            sortOrder = id.toInt(),
            isPrimary = id == 41L,
            currentPage = if (id == 41L) 1 else 4,
            rowMapping =
                serializeMapping(
                    listOf(if (id == 41L) RowMarker(10, 1, 0.2f) else RowMarker(20, 4, 0.8f)),
                ),
            readingLineEnabled = true,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = false,
            verticalReadingGuideEnabled = id == 42L,
            verticalReadingGuideXFraction = if (id == 41L) 0.2f else 0.8f,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun layer(
        id: Long,
        active: Boolean,
    ) = PatternAnnotationLayer(id, PatternAnnotationOwner.Project(7L, "local:$id"), active, 1L, 1L)
}

package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.repository.retryOnRepositoryReadFailure
import com.finnvek.knittools.serializedCopy
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelRestorationTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val completed =
        CounterProject(id = 7L, name = "Completed", count = 42, isCompleted = true, completedAt = 100L)
    private val active = CounterProject(id = 8L, name = "Active", count = 13)
    private val activeRows = MutableStateFlow(listOf(active))
    private val store = ViewModelStore()
    private val counters = mockk<ProjectCounterRepository>(relaxed = true)
    private var viewModelIndex = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        every { ProcessLifecycleOwner.get() } returns mockk<LifecycleOwner>(relaxed = true)
        coEvery { CounterWidgetState.syncAll(any(), any()) } returns Unit
        every { repository.getActiveProjects() } returns activeRows
        every { repository.observeProject(7L) } returns flowOf(completed)
        every { repository.observeProject(8L) } returns flowOf(active)
        coEvery { repository.getProject(7L) } returns completed
        coEvery { repository.getProject(8L) } returns active
        every { repository.observeActiveSession() } returns flowOf(null)
        coEvery { repository.refreshActiveSession() } returns null
        coEvery { repository.getTotalMinutesForProject(any()) } answers { firstArg<Long>().toInt() * 10 }
        every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        store.clear()
        unmockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        Dispatchers.resetMain()
    }

    @Test
    fun `fresh view model restores publicly selected completed project instead of active alternative`() =
        runTest {
            val restored = completedSelectionSnapshot()
            val vm = viewModel(restored)
            val states = recordStates(vm)
            runCurrent()

            assertEquals(7L, vm.uiState.value.projectId)
            assertEquals(7L, restored.get<Long>("counter.selected_project_id"))
            assertTrue(vm.uiState.value.isCompleted)
            assertEquals(42, vm.uiState.value.counter.count)
            assertFalse(states.any { it.projectId == 8L })
            assertEquals(70, vm.uiState.value.totalSessionMinutes)
            assertEquals(listOf(7L), vm.allPhotos.value.map { it.projectId })
            assertRestorationOnlyReads()
        }

    @Test
    fun `fresh view model restores completed project with no active projects`() =
        runTest {
            val restored = completedSelectionSnapshot()
            activeRows.value = emptyList()
            val vm = viewModel(restored)
            val states = recordStates(vm)
            runCurrent()

            assertEquals(7L, vm.uiState.value.projectId)
            assertTrue(vm.uiState.value.isCompleted)
            assertFalse(states.any { it.shouldLeaveCounter })
            assertRestorationOnlyReads()
        }

    @Test
    fun `active list emissions cannot replace a pending saved project`() =
        runTest { assertPendingRestoration(activeList = listOf(active)) }

    @Test
    fun `empty active list cannot close a pending saved project`() =
        runTest { assertPendingRestoration(activeList = emptyList()) }

    private fun TestScope.assertPendingRestoration(activeList: List<CounterProject>) {
        val restored = completedSelectionSnapshot()
        val result = CompletableDeferred<CounterProject?>()
        every { repository.observeProject(7L) } returns flow { emit(result.await()) }
        activeRows.value = activeList
        val vm = viewModel(restored)
        val states = recordStates(vm)
        val closed = recordClosedEvents(vm)
        runCurrent()
        activeRows.value = listOf(active.copy(name = "Updated active"))
        runCurrent()
        activeRows.value = activeList
        runCurrent()

        assertEquals(7L, restored.get<Long>("counter.selected_project_id"))
        assertTrue(states.all { it.projectId == null && !it.shouldLeaveCounter })
        assertTrue(closed.isEmpty())
        assertRestorationOnlyReads()
        result.complete(completed)
        runCurrent()
        assertEquals(7L, vm.uiState.value.projectId)
        assertTrue(vm.uiState.value.isCompleted)
        assertEquals(42, vm.uiState.value.counter.count)
        assertTrue(closed.isEmpty())
        assertRestorationOnlyReads()
    }

    @Test
    fun `saved project can load before the first active list result`() =
        runTest {
            val restored = completedSelectionSnapshot()
            val rows = MutableSharedFlow<List<CounterProject>>()
            every { repository.getActiveProjects() } returns rows
            val vm = viewModel(restored)
            val states = recordStates(vm)
            runCurrent()
            assertEquals(7L, vm.uiState.value.projectId)
            assertFalse(vm.uiState.value.projectsLoaded)
            rows.emit(listOf(active))
            runCurrent()
            assertTrue(vm.uiState.value.projectsLoaded)
            assertFalse(states.any { it.projectId == 8L || it.shouldLeaveCounter })
            assertRestorationOnlyReads()
        }

    @Test
    fun `newer explicit selection cancels delayed restoration and retains its content`() =
        runTest {
            val restored = completedSelectionSnapshot()
            val result = CompletableDeferred<CounterProject?>()
            val nextResult = CompletableDeferred<CounterProject?>()
            coEvery { repository.getProject(8L) } coAnswers { nextResult.await() }
            activeRows.value = emptyList()
            var cancelled = false
            every { repository.observeProject(7L) } returns
                flow {
                    try {
                        emit(result.await())
                    } finally {
                        cancelled = true
                    }
                }
            val vm = viewModel(restored)
            val states = recordStates(vm)
            val closed = recordClosedEvents(vm)
            runCurrent()
            vm.selectProjectByIdForLaunch(8L)
            runCurrent()
            assertFalse(states.any { it.shouldLeaveCounter })
            assertTrue(closed.isEmpty())
            nextResult.complete(active)
            runCurrent()
            val selected = vm.uiState.value
            val photos = vm.allPhotos.value
            result.complete(completed)
            runCurrent()

            assertTrue(cancelled)
            assertEquals(8L, restored.get<Long>("counter.selected_project_id"))
            assertEquals(8L, selected.projectId)
            assertEquals(13, selected.counter.count)
            assertEquals(80, selected.totalSessionMinutes)
            assertEquals(listOf(8L), photos.map { it.projectId })
            assertEquals(selected, vm.uiState.value)
            assertEquals(photos, vm.allPhotos.value)
            assertFalse(states.any { it.projectId == 7L })
            assertTrue(closed.isEmpty())
        }

    @Test
    fun `cleared newer selection cannot be resurrected by old restoration`() =
        runTest {
            val restored = completedSelectionSnapshot()
            val result = CompletableDeferred<CounterProject?>()
            every { repository.observeProject(7L) } returns flow { emit(result.await()) }
            val observedActive = MutableStateFlow<CounterProject?>(active)
            every { repository.observeProject(8L) } returns observedActive
            val vm = viewModel(restored)
            val closed = recordClosedEvents(vm)
            runCurrent()
            vm.selectProject(active)
            runCurrent()
            observedActive.value = null
            runCurrent()
            val cleared = vm.uiState.value
            result.complete(completed)
            runCurrent()

            assertNull(restored.get<Long>("counter.selected_project_id"))
            assertNull(vm.uiState.value.projectId)
            assertEquals(cleared, vm.uiState.value)
            assertEquals(1, closed.size)
        }

    @Test
    fun `authoritative missing saved project closes without selecting an active fallback`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val vm = viewModel(restored)
            val states = recordStates(vm)
            val closed = recordClosedEvents(vm)
            runCurrent()
            activeRows.value = listOf(active.copy(count = 14))
            runCurrent()

            assertNull(restored.get<Long>("counter.selected_project_id"))
            assertNull(vm.uiState.value.projectId)
            assertFalse(vm.uiState.value.isRestoringProject)
            assertTrue(states.all { it.projectId == null })
            assertEquals(1, closed.size)
            assertRestorationOnlyReads()
        }

    @Test
    fun `queued missing restoration close cannot close a subsequently selected project`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val vm = viewModel(restored)
            runCurrent()
            assertNull(vm.uiState.value.projectId)
            var loaded = false
            vm.selectProjectByIdForLaunch(8L) { loaded = it }
            runCurrent()
            assertTrue(loaded)
            val selected = vm.uiState.value
            val closed = recordClosedEvents(vm)
            runCurrent()

            assertEquals("Stale A request must not permit a back effect for B", 0, closed.size)
            assertEquals(8L, selected.projectId)
            assertEquals(13, selected.counter.count)
            assertEquals(selected, vm.uiState.value)
            assertEquals(8L, restored.get<Long>("counter.selected_project_id"))
            assertEquals(listOf(8L), vm.allPhotos.value.map { it.projectId })
        }

    @Test
    fun `queued close is rejected as soon as the next ID selection begins loading`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val nextResult = CompletableDeferred<CounterProject?>()
            coEvery { repository.getProject(8L) } coAnswers { nextResult.await() }
            val vm = viewModel(restored)
            runCurrent()
            vm.selectProjectByIdForLaunch(8L)
            val closed = recordClosedEvents(vm)
            runCurrent()

            assertNull(vm.uiState.value.projectId)
            assertTrue(closed.isEmpty())
            coVerify(exactly = 1) { repository.getProject(8L) }
            nextResult.complete(active)
            runCurrent()
            assertEquals(8L, vm.uiState.value.projectId)
            assertEquals(13, vm.uiState.value.counter.count)
            assertEquals(8L, restored.get<Long>("counter.selected_project_id"))
            assertTrue(closed.isEmpty())
        }

    @Test
    fun `old project disappearance during the next load cannot produce a current close`() =
        runTest {
            val restored = completedSelectionSnapshot()
            val observed = MutableStateFlow<CounterProject?>(completed)
            every { repository.observeProject(7L) } returns observed
            val nextResult = CompletableDeferred<CounterProject?>()
            coEvery { repository.getProject(8L) } coAnswers { nextResult.await() }
            val vm = viewModel(restored)
            runCurrent()
            vm.selectProjectByIdForLaunch(8L)
            runCurrent()
            observed.value = null
            runCurrent()
            val closed = recordClosedEvents(vm)
            runCurrent()

            assertTrue(closed.isEmpty())
            nextResult.complete(active)
            runCurrent()
            assertEquals(8L, vm.uiState.value.projectId)
            assertEquals(13, vm.uiState.value.counter.count)
            assertTrue(closed.isEmpty())
        }

    @Test
    fun `missing restoration still closes once with a delayed collector in the same context`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val vm = viewModel(restored)
            runCurrent()
            activeRows.value = listOf(active.copy(count = 14))
            runCurrent()
            val closed = recordClosedEvents(vm)
            runCurrent()

            assertEquals(1, closed.size)
            assertNull(vm.uiState.value.projectId)
            assertNull(restored.get<Long>("counter.selected_project_id"))
            assertRestorationOnlyReads()
        }

    @Test
    fun `current project disappearance closes once and does not replay after resubscription`() =
        runTest {
            val observed = MutableStateFlow<CounterProject?>(active)
            every { repository.observeProject(8L) } returns observed
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            runCurrent()
            vm.selectProjectByIdForLaunch(8L)
            runCurrent()
            var backCalls = 0
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.projectClosedEvents.collect { selectionVersion ->
                        vm.consumeProjectClosedEvent(selectionVersion) { backCalls++ }
                    }
                }
            observed.value = null
            runCurrent()
            assertEquals(1, backCalls)
            assertNull(vm.uiState.value.projectId)
            assertNull(handle.get<Long>("counter.selected_project_id"))
            collector.cancel()
            runCurrent()
            val repeated = recordClosedEvents(vm)
            runCurrent()
            assertTrue(repeated.isEmpty())
            assertEquals(1, backCalls)
        }

    @Test
    fun `rejecting stale A close preserves the later legitimate close for B`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val observed = MutableStateFlow<CounterProject?>(active)
            every { repository.observeProject(8L) } returns observed
            val vm = viewModel(restored)
            runCurrent()
            vm.selectProjectByIdForLaunch(8L)
            runCurrent()
            val closed = recordClosedEvents(vm)
            runCurrent()
            assertTrue(closed.isEmpty())
            assertEquals(8L, vm.uiState.value.projectId)
            observed.value = null
            runCurrent()

            assertEquals(1, closed.size)
            assertNull(vm.uiState.value.projectId)
            assertNull(restored.get<Long>("counter.selected_project_id"))
        }

    @Test
    fun `conflation retains the newer valid close when both projects disappear before collection`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val observed = MutableStateFlow<CounterProject?>(active)
            every { repository.observeProject(8L) } returns observed
            val vm = viewModel(restored)
            runCurrent()
            vm.selectProjectByIdForLaunch(8L)
            runCurrent()
            observed.value = null
            runCurrent()
            val closed = recordClosedEvents(vm)
            runCurrent()

            assertEquals(1, closed.size)
            assertNull(vm.uiState.value.projectId)
            assertNull(restored.get<Long>("counter.selected_project_id"))
        }

    @Test
    fun `returning to the same project ID does not revive its obsolete close`() =
        runTest {
            val restored = completedSelectionSnapshot()
            every { repository.observeProject(7L) } returns flowOf(null)
            val vm = viewModel(restored)
            runCurrent()
            vm.selectProject(active)
            runCurrent()
            every { repository.observeProject(7L) } returns flowOf(completed)
            vm.selectProject(completed)
            runCurrent()
            val closed = recordClosedEvents(vm)
            runCurrent()

            assertTrue(closed.isEmpty())
            assertEquals(7L, vm.uiState.value.projectId)
            assertTrue(vm.uiState.value.isCompleted)
            assertEquals(42, vm.uiState.value.counter.count)
        }

    @Test
    fun `repository read retry retains saved identity and does not report deletion`() =
        runTest {
            val restored = completedSelectionSnapshot()
            var attempts = 0
            every { repository.observeProject(7L) } returns
                flow {
                    attempts++
                    if (attempts == 1) throw IOException("read failed")
                    emit(completed)
                }.retryOnRepositoryReadFailure()
            activeRows.value = emptyList()
            val vm = viewModel(restored)
            val states = recordStates(vm)
            val closed = recordClosedEvents(vm)
            runCurrent()
            assertEquals(1, attempts)
            assertEquals(7L, restored.get<Long>("counter.selected_project_id"))
            assertTrue(vm.uiState.value.isRestoringProject)
            assertTrue(closed.isEmpty())
            advanceTimeBy(250L)
            runCurrent()

            assertEquals(7L, vm.uiState.value.projectId)
            assertFalse(states.any { it.shouldLeaveCounter })
            assertTrue(closed.isEmpty())
            assertRestorationOnlyReads()
        }

    @Test
    fun `saved active identity wins over a different first active project`() =
        runTest {
            activeRows.value = listOf(active.copy(id = 9L), active)
            val handle = SavedStateHandle(mapOf("counter.selected_project_id" to 8L))
            val vm = viewModel(handle)
            val states = recordStates(vm)
            runCurrent()
            assertEquals(8L, vm.uiState.value.projectId)
            assertFalse(vm.uiState.value.isCompleted)
            assertFalse(states.any { it.projectId == 9L })
            assertRestorationOnlyReads()
        }

    @Test
    fun `ordinary startup still selects first active project or leaves an empty counter`() =
        runTest {
            val first = viewModel()
            runCurrent()
            assertEquals(8L, first.uiState.value.projectId)
            store.clear()
            activeRows.value = emptyList()
            val empty = viewModel()
            runCurrent()
            assertNull(empty.uiState.value.projectId)
            assertTrue(empty.uiState.value.shouldLeaveCounter)
        }

    @Test
    fun `restoration displays repeat position without persisting an initial correction`() =
        runTest {
            val restored = completedSelectionSnapshot()
            val counter =
                ProjectCounter(
                    id = 3L,
                    projectId = 7L,
                    name = "Repeat",
                    counterType = ProjectCounterType.REPEAT_SECTION,
                    repeatStartRow = 1,
                    repeatEndRow = 10,
                    totalRepeats = 10,
                )
            every { counters.getCountersForProject(7L) } returns flowOf(listOf(counter))
            val vm = viewModel(restored)
            runCurrent()
            val restoredCounter =
                vm.uiState.value.projectCounters
                    .single()
            assertEquals(2, restoredCounter.count)
            coVerify(exactly = 0) { counters.updateRepeatSectionState(any(), any()) }
            assertRestorationOnlyReads()
        }

    private fun TestScope.completedSelectionSnapshot(): SavedStateHandle {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        runCurrent()
        first.selectProjectByIdForLaunch(7L)
        runCurrent()
        assertEquals(7L, first.uiState.value.projectId)
        assertTrue(first.uiState.value.isCompleted)
        val restored = handle.serializedCopy()
        store.clear()
        clearMocks(repository, answers = false)
        return restored
    }

    private fun assertRestorationOnlyReads() {
        coVerify(atLeast = 0) {
            repository.getActiveProjects()
            repository.observeProject(any())
            repository.observeActiveSession()
            repository.refreshActiveSession()
            repository.getTotalMinutesForProject(any())
        }
        confirmVerified(repository)
    }

    private fun TestScope.recordClosedEvents(vm: CounterViewModel): List<Unit> {
        val events = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.projectClosedEvents.collect { selectionVersion ->
                vm.consumeProjectClosedEvent(selectionVersion) { events.add(Unit) }
            }
        }
        return events
    }

    private fun TestScope.recordStates(vm: CounterViewModel): List<CounterUiState> {
        val states = mutableListOf<CounterUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { states.add(it) } }
        return states
    }

    private fun TestScope.viewModel(handle: SavedStateHandle = SavedStateHandle()): CounterViewModel {
        val preferences = mockk<PreferencesManager>()
        every { preferences.preferences } returns emptyFlow()
        val proManager = mockk<ProManager>()
        every { proManager.proState } returns MutableStateFlow(ProState())
        every { proManager.hasFeature(any()) } returns false
        val yarns = mockk<YarnCardRepository>()
        every { yarns.getAllCards() } returns emptyFlow()
        val patterns = mockk<SavedPatternRepository>()
        every { patterns.getAll() } returns flowOf(emptyList())
        val reminders = mockk<ReminderRepository>()
        every { reminders.getRemindersForProject(any()) } returns flowOf(emptyList())
        val photos = mockk<ProgressPhotoRepository>()
        every { photos.getLatestPhotos(any()) } answers { flowOf(listOf(photo(firstArg()))) }
        every { photos.getPhotosForProject(any()) } answers { flowOf(listOf(photo(firstArg()))) }
        val yarnNotes = mockk<ProjectYarnNoteRepository>()
        every { yarnNotes.observeForProject(any()) } returns flowOf(emptyList())
        val documents = mockk<ProjectDocumentRepository>()
        every { documents.observeDocuments(any<Long>()) } returns flowOf(emptyList())
        every { documents.observeActiveDocument(any()) } returns flowOf(null)
        return CounterViewModel(
            repository = repository,
            reminderRepository = reminders,
            projectCounterRepository = counters,
            photoRepository = photos,
            projectYarnNoteRepository = yarnNotes,
            preferencesManager = preferences,
            proManager = proManager,
            yarnCardRepository = yarns,
            savedPatternRepository = patterns,
            projectDocumentRepository = documents,
            patternDocumentStorage = mockk(),
            inAppReviewManager = mockk(),
            savedStateHandle = handle,
            context = mockk<Context>(relaxed = true),
            ioDispatcher = dispatcher,
            applicationScope = backgroundScope,
        ).also { store.put("counter-${viewModelIndex++}", it) }
    }

    private fun photo(projectId: Long) =
        ProgressPhoto(projectId = projectId, photoUri = "content://test/$projectId", rowNumber = 0, createdAt = 1L)
}

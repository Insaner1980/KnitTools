package com.finnvek.knittools.ui.screens.counter

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val store = ViewModelStore()
    private var viewModelIndex = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockCounterProcessAndWidget()
        every { repository.getActiveProjects() } returns
            flowOf(listOf(CounterProject(id = 8L, name = "Active", count = 13)))
        every { repository.observeProject(8L) } returns flowOf(CounterProject(id = 8L, name = "Active", count = 13))
        repository.stubSessionReads()
    }

    @After
    fun tearDown() {
        store.clear()
        unmockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        Dispatchers.resetMain()
    }

    @Test
    fun `switching projects cancels the previous widget sync without replacing the new selection`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            val previous = CounterProject(id = 9L, name = "Previous", count = 2)
            val current = CounterProject(id = 10L, name = "Current", count = 5)
            every { repository.observeProject(9L) } returns flowOf(previous)
            every { repository.observeProject(10L) } returns flowOf(current)
            val finishSync = CompletableDeferred<Unit>()
            var syncCancelled = false
            coEvery { CounterWidgetState.syncAll(any(), match { it.projectId == 9L }) } coAnswers {
                try {
                    finishSync.await()
                } catch (cancelled: CancellationException) {
                    syncCancelled = true
                    throw cancelled
                }
            }

            vm.selectProject(previous)
            runCurrent()
            assertEquals(9L, vm.uiState.value.projectId)
            vm.selectProject(current)
            runCurrent()
            finishSync.complete(Unit)
            runCurrent()

            assertTrue(syncCancelled)
            assertEquals(10L, vm.uiState.value.projectId)
            assertEquals(5, vm.uiState.value.counter.count)
            coVerify(exactly = 1) { CounterWidgetState.syncAll(any(), match { it.projectId == 10L }) }
            coVerify(exactly = 0) { repository.getTotalMinutesForProject(9L) }
        }

    @Test
    fun `widget metadata failure does not fail project selection`() =
        runTest {
            val vm = viewModel()
            runCurrent()
            val project = CounterProject(id = 9L, name = "Selected", count = 2)
            coEvery { repository.getProject(9L) } returns project
            every { repository.observeProject(9L) } returns flowOf(project)
            coEvery { CounterWidgetState.syncAll(any(), match { it.projectId == 9L }) } throws
                IllegalStateException("Widget service unavailable")
            var loaded = false

            vm.selectProjectByIdForLaunch(9L) { loaded = it }
            runCurrent()

            assertTrue(loaded)
            assertEquals(9L, vm.uiState.value.projectId)
            assertEquals(2, vm.uiState.value.counter.count)
            coVerify(exactly = 1) { repository.getTotalMinutesForProject(9L) }
        }

    @Test
    fun `process observer callbacks belong only to each live view model across recreation`() =
        runTest {
            val owner = ProcessLifecycleOwner.get()
            val lifecycle = owner.lifecycle
            val observers = captureProcessObservers()
            val session = activeSession()
            every { repository.observeActiveSession() } returns flowOf(session)
            coEvery { repository.refreshActiveSession() } returns session
            coEvery { repository.checkpointActiveSession() } returns session
            var previousObserver: DefaultLifecycleObserver? = null

            for (lifetime in 1..2) {
                val vm = viewModel()
                assertEquals(1, observers.size)
                val observer = observers.single()
                assertNotSame(previousObserver, observer)
                verify(exactly = lifetime) { lifecycle.addObserver(any()) }
                verify(exactly = 1) { lifecycle.addObserver(refEq(observer)) }
                runCurrent()
                coVerify(exactly = 1) { repository.refreshActiveSession() }
                coVerify(atLeast = 1) { repository.activeSessionDurationSeconds(session) }
                repository.assertRestorationOnlyReads()
                assertEquals(session, vm.uiState.value.activeSession)
                assertNull(vm.uiState.value.sessionStopSummary)
                clearMocks(repository, answers = false)

                observers.toList().forEach { it.onPause(owner) }
                runCurrent()
                coVerify(exactly = 1) { repository.checkpointActiveSession() }
                coVerify(exactly = 0) { repository.refreshActiveSession() }
                confirmVerified(repository)
                assertEquals(session, vm.uiState.value.activeSession)
                assertNull(vm.uiState.value.sessionStopSummary)

                observers.toList().forEach { it.onResume(owner) }
                runCurrent()
                coVerify(exactly = 1) { repository.checkpointActiveSession() }
                coVerify(exactly = 1) { repository.refreshActiveSession() }
                coVerify(exactly = 1) { repository.activeSessionDurationSeconds(session) }
                confirmVerified(repository)
                assertEquals(session, vm.uiState.value.activeSession)
                assertNull(vm.uiState.value.sessionStopSummary)

                store.clear()
                assertTrue(observers.isEmpty())
                verify(exactly = 1) { lifecycle.removeObserver(refEq(observer)) }
                verify(exactly = lifetime) { lifecycle.removeObserver(any()) }
                val clearedState = vm.uiState.value
                observers.toList().forEach { it.onPause(owner) }
                observers.toList().forEach { it.onResume(owner) }
                runCurrent()
                confirmVerified(repository)
                assertEquals(clearedState, vm.uiState.value)
                previousObserver = observer
                clearMocks(repository, answers = false)
            }
        }

    @Test
    fun `admitted process pause checkpoint can finish after its view model is cleared`() =
        runTest {
            val owner = ProcessLifecycleOwner.get()
            val lifecycle = owner.lifecycle
            val observers = captureProcessObservers()
            val checkpointStarted = CompletableDeferred<Unit>()
            val finishCheckpoint = CompletableDeferred<Unit>()
            var checkpointFinished = false
            coEvery { repository.checkpointActiveSession() } coAnswers {
                checkpointStarted.complete(Unit)
                finishCheckpoint.await()
                checkpointFinished = true
                null
            }
            viewModel()
            assertEquals(1, observers.size)
            val observer = observers.single()
            verify(exactly = 1) { lifecycle.addObserver(refEq(observer)) }
            verify(exactly = 1) { lifecycle.addObserver(any()) }
            runCurrent()
            coVerify(exactly = 1) { repository.refreshActiveSession() }
            repository.assertRestorationOnlyReads()
            clearMocks(repository, answers = false)

            observers.toList().forEach { it.onPause(owner) }
            runCurrent()
            assertTrue(checkpointStarted.isCompleted)
            assertFalse(checkpointFinished)
            coVerify(exactly = 1) { repository.checkpointActiveSession() }
            confirmVerified(repository)

            store.clear()
            assertTrue(observers.isEmpty())
            verify(exactly = 1) { lifecycle.removeObserver(refEq(observer)) }
            verify(exactly = 1) { lifecycle.removeObserver(any()) }
            observers.toList().forEach { it.onPause(owner) }
            observers.toList().forEach { it.onResume(owner) }
            runCurrent()
            assertFalse(checkpointFinished)
            confirmVerified(repository)

            finishCheckpoint.complete(Unit)
            runCurrent()
            assertTrue(checkpointFinished)
            coVerify(exactly = 1) { repository.checkpointActiveSession() }
            confirmVerified(repository)
        }

    private fun captureProcessObservers(): MutableList<DefaultLifecycleObserver> {
        val observers = mutableListOf<DefaultLifecycleObserver>()
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        every { lifecycle.addObserver(any()) } answers {
            observers.add(firstArg())
        }
        every { lifecycle.removeObserver(any()) } answers {
            val observer = firstArg<DefaultLifecycleObserver>()
            assertTrue(observers.removeAll { it === observer })
        }
        return observers
    }

    private fun TestScope.viewModel(): CounterViewModel =
        emptyCounterViewModel(repository, dispatcher)
            .also { store.put("counter-${viewModelIndex++}", it) }
}

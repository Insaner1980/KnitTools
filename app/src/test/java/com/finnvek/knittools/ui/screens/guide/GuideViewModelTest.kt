package com.finnvek.knittools.ui.screens.guide

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.analytics.UsageEvent
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class GuideViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CounterRepository>()
    private val proManager = mockk<ProManager>()
    private val analytics = mockk<UsageAnalytics>(relaxed = true)
    private val ready = MutableStateFlow(false)
    private val state = MutableStateFlow(ProState())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { proManager.proState } returns state
        every { proManager.initialStateReady } returns ready
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `counter uses latest active while project page includes completed projects`() =
        runTest {
            every { repository.getActiveProjects() } returns
                flowOf(listOf(CounterProject(id = 7), CounterProject(id = 2)))
            every { repository.getAllProjects() } returns
                flowOf(listOf(CounterProject(id = 9, isCompleted = true), CounterProject(id = 7)))
            val vm = createViewModel()
            assertEquals(7L, vm.projectForAction(GuideAction.OpenCounterForLatestProject).await())
            assertEquals(9L, vm.projectForAction(GuideAction.OpenLatestProjectPage).await())
        }

    @Test
    fun `project action waits for real repository emission before choosing fallback`() =
        runTest {
            val projects = MutableSharedFlow<List<CounterProject>>()
            every { repository.getActiveProjects() } returns projects
            val result = createViewModel().projectForAction(GuideAction.OpenCounterForLatestProject)
            runCurrent()
            assertFalse(result.isCompleted)
            projects.emit(emptyList())
            assertNull(result.await())
        }

    @Test
    fun `project resolution is cancelled when the view model is cleared`() =
        runTest {
            every { repository.getActiveProjects() } returns MutableSharedFlow()
            val vm = createViewModel()
            val store = ViewModelStore().apply { put("guide", vm) }
            val result = vm.projectForAction(GuideAction.OpenCounterForLatestProject)
            runCurrent()
            assertFalse(result.isCompleted)

            store.clear()

            assertTrue(result.isCancelled)
        }

    @Test
    fun `actions without a project do not query project storage`() =
        runTest {
            val vm = createViewModel()
            GuideAction.entries
                .filterNot { it == GuideAction.OpenCounterForLatestProject || it == GuideAction.OpenLatestProjectPage }
                .forEach { assertNull(vm.projectForAction(it).await()) }
            verify(exactly = 0) { repository.getActiveProjects() }
            verify(exactly = 0) { repository.getAllProjects() }
        }

    @Test
    fun `pro presentation waits for initial billing and trial state`() =
        runTest {
            val vm = createViewModel()
            assertNull(vm.proState.first())
            state.value = ProState(status = ProStatus.TRIAL_ACTIVE)
            assertNull(vm.proState.first())
            ready.value = true
            assertEquals(state.value, vm.proState.first())
            ready.value = false
            assertNull(vm.proState.first())
        }

    @Test
    fun `guide opened is recorded once for the retained entry`() {
        val vm = createViewModel()
        vm.onOpened()
        vm.onOpened()
        verify(exactly = 1) { analytics.track(UsageEvent.GUIDE_OPENED) }
    }

    private fun createViewModel() = GuideViewModel(repository, proManager, analytics, dispatcher)
}

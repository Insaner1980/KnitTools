package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class InsightsStateRestorationTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saved filters are available during loading and survive a new view model`() =
        runTest {
            val projects = MutableSharedFlow<List<CounterProject>>(replay = 1)
            val repository = mockk<CounterRepository>()
            every { repository.getAllProjects() } returns projects
            every { repository.observeCompletions() } returns flowOf(emptyList())
            stubSessions(repository, flowOf(emptyList()))
            val proManager = mockk<ProManager>()
            every { proManager.hasFeature(any()) } returns false
            every { proManager.hasFeatureFlow(any()) } returns flowOf(false)
            val savedState = SavedStateHandle()
            val first =
                InsightsViewModel(
                    repository,
                    proManager,
                    dispatcher,
                    savedState,
                    mockk(relaxed = true),
                    backgroundScope,
                )
            first.selectTimeRange(TimeRange.THIS_MONTH)
            first.selectProject(7L)
            val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })
            val restored =
                InsightsViewModel(
                    repository,
                    proManager,
                    dispatcher,
                    restoredState,
                    mockk(relaxed = true),
                    backgroundScope,
                )
            assertEquals(TimeRange.THIS_MONTH, restored.uiState.value.timeRange)
            assertEquals(7L, restored.uiState.value.selectedProjectId)
            assertTrue(restored.uiState.value.isLoading)
            val job = backgroundScope.launch(dispatcher) { restored.uiState.collect() }
            runCurrent()
            assertEquals(7L, restored.selectedProjectId.value)
            projects.emit(listOf(CounterProject(id = 7, name = "Saved project")))
            runCurrent()
            assertFalse(restored.uiState.value.isLoading)
            assertEquals(7L, restored.uiState.value.selectedProjectId)
            projects.emit(emptyList())
            runCurrent()
            assertNull(restored.selectedProjectId.value)
            assertEquals(TimeRange.THIS_MONTH, restored.timeRange.value)
            job.cancel()
        }
}

package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.SessionInsightsFacts
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsQueryCancellationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CounterRepository>()
    private val proManager = mockk<ProManager>()

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeCompletions() } returns flowOf(emptyList())
        every { proManager.hasFeature(any()) } returns false
        every { proManager.hasFeatureFlow(any()) } returns flowOf(false)
    }

    @After fun close() = Dispatchers.resetMain()

    @Test
    fun `rapid range and project changes cancel previous queries before publishing`() =
        runTest {
            every { repository.getAllProjects() } returns flowOf(listOf(CounterProject(id = 7, name = "Selected")))
            val cancelled = mutableListOf<Pair<Long?, Long?>>()
            val requested = mutableListOf<Pair<Long?, Long?>>()
            every {
                repository.observeSessionsForInsights<InsightsSessionAccumulator>(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } answers
                {
                    val key = firstArg<Long?>() to secondArg<Long?>()
                    val create = arg<(SessionInsightsFacts) -> InsightsSessionAccumulator>(3)
                    flow {
                        requested += key
                        try {
                            if (key.first == 7L &&
                                key.second != null
                            ) {
                                emit(create(SessionInsightsFacts(true, emptyList())))
                            }
                            awaitCancellation()
                        } finally {
                            cancelled += key
                        }
                    }
                }
            val viewModel = InsightsViewModel(repository, proManager, testDispatcher)
            val states = mutableListOf<InsightsUiState>()
            backgroundScope.launch(testDispatcher) { viewModel.uiState.collect { states += it } }
            runCurrent()
            viewModel.selectTimeRange(TimeRange.THIS_WEEK)
            runCurrent()
            viewModel.selectProject(7)
            viewModel.selectTimeRange(TimeRange.THIS_MONTH)
            runCurrent()
            assertTrue(cancelled.contains(null to null))
            assertTrue(cancelled.any { it.first == null && it.second != null })
            assertEquals(7L, requested.last().first)
            assertEquals(TimeRange.THIS_MONTH, viewModel.uiState.value.timeRange)
            assertTrue(viewModel.uiState.value.hasAnySessionData)
            assertTrue(states.filter { !it.isLoading }.all { it.selectedProjectId == 7L })
        }
}

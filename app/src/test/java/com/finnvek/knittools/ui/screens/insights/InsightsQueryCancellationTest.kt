package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.local.SessionProjectActivity
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.SessionInsightsFacts
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsQueryCancellationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockInsightsRepository()
    private val proManager = mockk<ProManager>()

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
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
            val viewModel =
                InsightsViewModel(
                    repository,
                    proManager,
                    testDispatcher,
                    SavedStateHandle(),
                    mockk(relaxed = true),
                    backgroundScope,
                )
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

    @Test
    fun `actual accumulation completes across the batch boundary without a selection change`() =
        verifyAccumulationAtBatchBoundary(changeProject = false)

    @Test
    fun `project change cancels actual partial accumulation at yield without obsolete publication`() =
        verifyAccumulationAtBatchBoundary(changeProject = true)

    private fun verifyAccumulationAtBatchBoundary(changeProject: Boolean) =
        runTest {
            val foldDispatcher = StandardTestDispatcher(testScheduler)
            val batch = BatchAccumulation(foldDispatcher)
            batch.stubQueries()
            val viewModel =
                InsightsViewModel(
                    repository,
                    proManager,
                    testDispatcher,
                    SavedStateHandle(),
                    mockk(relaxed = true),
                    backgroundScope,
                )
            val states = mutableListOf<InsightsUiState>()
            val collection = backgroundScope.launch(testDispatcher) { viewModel.uiState.collect { states += it } }
            try {
                // Completion queues this test ahead of the 257th session's yield continuation.
                assertEquals(SessionMetricSummary(15_616L, 512L, 256), batch.partial.await())
                assertEquals(SessionMetricSummary(15_616L, 512L, 256), batch.accumulators.getValue(null).summary)
                assertTrue(batch.tailEntered)
                assertFalse(batch.tailReturned)
                assertTrue(batch.cleaned.isEmpty())
                assertTrue(batch.emitted.isEmpty())
                assertTrue(states.all { it.isLoading })

                if (changeProject) viewModel.selectProject(7)
                runCurrent()

                batch.assertResult(changeProject, states, viewModel)
            } finally {
                collection.cancel()
                viewModel.viewModelScope.cancel()
                runCurrent()
            }
        }

    private inner class BatchAccumulation(
        private val foldDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val date = LocalDate.of(2026, 10, 1)
        val start = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val sessions =
            List(257) { index ->
                val seconds = if (index == 256) 91L else 61L
                val progress = if (index == 256) 13 else 2
                KnitSession(
                    id = index + 1L,
                    projectId = if (index == 0 || index == 256) 7L else 1L,
                    startedAt = start,
                    endedAt = start + seconds * 1000,
                    startRow = 0,
                    endRow = progress,
                    durationMinutes = 2,
                    durationSeconds = seconds,
                    rowsWorked = progress,
                    zoneId = "UTC",
                )
            }
        val partial = CompletableDeferred<SessionMetricSummary>()
        val accumulators = mutableMapOf<Long?, InsightsSessionAccumulator>()
        val cancelled = mutableListOf<Long?>()
        val cleaned = mutableListOf<Long?>()
        val emitted = mutableListOf<Long?>()
        var tailEntered = false
        var tailReturned = false

        fun stubQueries() {
            every { repository.getAllProjects() } returns
                flowOf(listOf(CounterProject(id = 1, name = "Other"), CounterProject(id = 7, name = "Selected")))
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
                    val projectId = firstArg<Long?>()
                    assertEquals(null, secondArg<Long?>())
                    val create = arg<(SessionInsightsFacts) -> InsightsSessionAccumulator>(3)
                    val accumulate = arg<suspend (InsightsSessionAccumulator, List<KnitSession>) -> Unit>(4)
                    val scoped = sessions.filter { projectId == null || it.projectId == projectId }
                    flow {
                        val facts =
                            SessionInsightsFacts(
                                true,
                                scoped.map { it.projectId }.distinct().map { SessionProjectActivity(it, start) },
                                date,
                            )
                        val accumulator = create(facts)
                        accumulators[projectId] = accumulator
                        try {
                            // Queue only the fold; the VM's query/selection flow remains unconfined.
                            withContext(foldDispatcher) {
                                if (projectId == null) {
                                    accumulate(accumulator, scoped.take(256))
                                    partial.complete(accumulator.summary)
                                    tailEntered = true
                                    // No test suspension here: the actual callback yields before add.
                                    accumulate(accumulator, scoped.drop(256))
                                    tailReturned = true
                                } else {
                                    accumulate(accumulator, scoped)
                                }
                            }
                            emit(accumulator)
                            emitted += projectId
                        } catch (failure: CancellationException) {
                            cancelled += projectId
                            throw failure
                        } finally {
                            cleaned += projectId
                        }
                    }
                }
        }

        fun assertResult(
            changeProject: Boolean,
            states: List<InsightsUiState>,
            viewModel: InsightsViewModel,
        ) {
            val selectedId = if (changeProject) 7L else null
            val expected =
                if (changeProject) SessionMetricSummary(152L, 15L, 2) else SessionMetricSummary(15_707L, 525L, 257)
            assertEquals(if (changeProject) listOf<Long?>(null) else emptyList<Long?>(), cancelled)
            assertEquals(if (changeProject) listOf(null, 7L) else listOf(null), cleaned)
            assertEquals(listOf(selectedId), emitted)
            assertEquals(!changeProject, tailReturned)
            if (changeProject) {
                assertEquals(SessionMetricSummary(15_616L, 512L, 256), accumulators.getValue(null).summary)
            }
            assertEquals(expected, accumulators.getValue(selectedId).summary)
            val loaded = states.filterNot { it.isLoading }
            assertTrue(loaded.isNotEmpty())
            loaded.forEach { state ->
                assertEquals(selectedId, state.selectedProjectId)
                assertEquals(if (changeProject) "Selected" else null, state.selectedProjectName)
                assertEquals(TimeRange.ALL_TIME, state.timeRange)
                assertEquals(if (changeProject) 3 else 262, state.totalMinutes)
                assertEquals(expected.totalRows, state.totalRows)
                assertTrue(state.hasSessionData)
                assertTrue(state.hasAnySessionData)
                assertFalse(state.rowMetricsAvailable)
                assertEquals(MinutesPerRowDisplay.Unavailable, state.minutesPerRow)
            }
            assertEquals(loaded.last(), viewModel.uiState.value)
        }
    }
}

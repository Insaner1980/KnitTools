package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCompletion
import com.finnvek.knittools.pro.ProFeature
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsCompletionViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CounterRepository>()
    private val proManager = mockk<ProManager>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getAllProjects() } returns flowOf(emptyList())
        every { repository.getSessionsForInsights(null, null) } returns flowOf(emptyList())
        every { proManager.hasFeature(ProFeature.INSIGHTS_CHARTS) } returns false
        every { proManager.hasFeatureFlow(ProFeature.INSIGHTS_CHARTS) } returns flowOf(false)
        every { proManager.hasFeature(ProFeature.STREAK) } returns false
        every { proManager.hasFeatureFlow(ProFeature.STREAK) } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = InsightsViewModel(repository, proManager, testDispatcher)

    @Test
    fun `completion loading updates range and project without replacing session metrics`() =
        runTest {
            val events = MutableSharedFlow<List<ProjectCompletion>>(replay = 1)
            every { repository.observeCompletions() } returns events
            every { repository.getAllProjects() } returns
                flowOf(listOf(CounterProject(id = 1, name = "Socks"), CounterProject(id = 2, name = "Hat")))
            val viewModel = createViewModel()
            val job = backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val timestamp = today.atStartOfDay(zone).toInstant().toEpochMilli()
            events.emit(listOf(ProjectCompletion(1, 1, timestamp, zone.id)))
            runCurrent()
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, viewModel.uiState.value.completions.events.size)
            assertTrue(
                viewModel.uiState.value.completions.buckets
                    .isEmpty(),
            )
            viewModel.selectTimeRange(TimeRange.THIS_MONTH)
            runCurrent()
            assertEquals(1, viewModel.uiState.value.completions.events.size)
            viewModel.selectProject(2)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.completions.events
                    .isEmpty(),
            )
            assertTrue(viewModel.uiState.value.hasAnyCompletionData)
            viewModel.selectProject(1)
            events.emit(emptyList())
            runCurrent()
            assertFalse(viewModel.uiState.value.hasAnyCompletionData)
            job.cancel()
        }

    @Test
    fun `calendar captures zone and date once and detects timezone change on same date`() =
        runTest {
            var zone = ZoneId.of("Europe/Helsinki")
            var zoneReads = 0
            var timeReads = 0
            val values = mutableListOf<InsightsCalendar>()
            val timestamp =
                java.time.Instant
                    .parse("2026-09-13T12:00:00Z")
                    .toEpochMilli()
            val job =
                backgroundScope.launch(testDispatcher) {
                    insightsCalendarChanges(
                        nowMillis = {
                            timeReads++
                            timestamp
                        },
                        zoneProvider = {
                            zoneReads++
                            zone
                        },
                    ).collect { values += it }
                }
            runCurrent()
            assertEquals(1, zoneReads)
            assertEquals(1, timeReads)
            assertEquals(InsightsCalendar(LocalDate.of(2026, 9, 13), zone), values.single())
            zone = ZoneId.of("America/New_York")
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(2, values.size)
            assertEquals(zone, values.last().zone)
            job.cancel()
        }
}

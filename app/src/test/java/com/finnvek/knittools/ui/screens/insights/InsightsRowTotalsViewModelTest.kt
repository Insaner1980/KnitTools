package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsRowTotalsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<CounterRepository>()
    private val proManager = mockk<ProManager>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeCompletions() } returns flowOf(emptyList())
        every { proManager.hasFeature(ProFeature.INSIGHTS_CHARTS) } returns true
        every { proManager.hasFeatureFlow(ProFeature.INSIGHTS_CHARTS) } returns flowOf(true)
        every { proManager.hasFeature(ProFeature.STREAK) } returns false
        every { proManager.hasFeatureFlow(ProFeature.STREAK) } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `large row totals reach UI metrics charts formatting and project filters intact`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            every { repository.getAllProjects() } returns flowOf(listOf(CounterProject(1, "A"), CounterProject(2, "B")))
            stubSessions(
                repository,
                flowOf(
                    listOf(
                        sessionAt(today, 10, 1_500_000_000, zone),
                        sessionAt(today, 11, 1_500_000_000, zone),
                        sessionAt(today, 12, 28, zone).copy(projectId = 2),
                    ),
                ),
            )
            val viewModel = InsightsViewModel(repository, proManager, testDispatcher)

            val state = viewModel.uiState.first { it.hasSessionData }

            assertEquals(3_000_000_028L, state.totalRows)
            assertEquals("3,000,000,028", formatIntegerForDisplay(state.totalRows, Locale.US))
            assertEquals(MinutesPerRowDisplay.UnderOneMinute, state.minutesPerRow)
            assertEquals(listOf(3_000_000_000L, 28L), state.timePerProject.map { it.totalRows })
            assertEquals(3_000_000_028L, state.chartBuckets.sumOf { it.totalRows })

            viewModel.selectProject(1)
            val filtered = viewModel.uiState.first { it.selectedProjectId == 1L && !it.isLoading }

            assertEquals(3_000_000_000L, filtered.totalRows)
            assertEquals(MinutesPerRowDisplay.UnderOneMinute, filtered.minutesPerRow)
            assertEquals(3_000_000_000L, filtered.timePerProject.single().totalRows)
            assertEquals(3_000_000_000L, filtered.chartBuckets.sumOf { it.totalRows })
        }

    private fun sessionAt(
        date: LocalDate,
        hour: Int,
        rows: Int,
        zone: ZoneId,
    ): KnitSession {
        val startedAt =
            date
                .atTime(hour, 0)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        return KnitSession(
            projectId = 1L,
            startedAt = startedAt,
            endedAt = startedAt + 1_800_000L,
            startRow = 0,
            endRow = rows,
            durationMinutes = 30,
            durationSeconds = 1_800L,
            rowsWorked = rows,
        )
    }
}

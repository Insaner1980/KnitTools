package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager
    private lateinit var proState: MutableStateFlow<ProState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        proManager = mockk()
        proState = MutableStateFlow(ProState())
        every { repository.getAllProjects() } returns flowOf(emptyList())
        every { repository.getSessionsForInsights(null, null) } returns flowOf(emptyList())
        every { repository.getSessionsForInsights(null, any()) } returns flowOf(emptyList())
        every { proManager.proState } returns proState
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = InsightsViewModel(repository, proManager, testDispatcher)

    @Test
    fun `isPro uses insights charts feature gate`() =
        runTest {
            val values = mutableListOf<Boolean>()
            val viewModel = createViewModel()
            val job =
                launch {
                    viewModel.isPro.take(2).toList(values)
                }
            advanceUntilIdle()

            assertFalse(viewModel.isPro.value)
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            advanceUntilIdle()

            assertEquals(listOf(false, true), values)
            assertTrue(viewModel.isPro.value)
            assertTrue(proState.value.hasFeature(ProFeature.INSIGHTS_CHARTS))
            job.cancel()
        }

    @Test
    fun `chart data stays empty before insights charts feature is available`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val session = sessionAt(date = today, hour = 10, minute = 0, rows = 12, minutes = 30, zone = zone)
            every { repository.getSessionsForInsights(null, null) } returns flowOf(listOf(session))

            val viewModel = createViewModel()
            val state = viewModel.uiState.first { it.hasSessionData }

            assertEquals(30, state.totalMinutes)
            assertEquals(emptyList<ProjectTime>(), state.timePerProject)
            assertEquals(emptyList<PaceOverTimePoint>(), state.paceOverTime)
            assertEquals(emptyMap<LocalDate, Int>(), state.dailyActivity)
        }

    @Test
    fun `daily activity keeps heatmap lookback when time range changes`() =
        runTest {
            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            val zone = ZoneId.systemDefault()
            val activityDate = LocalDate.now(zone).minusDays(30)
            val activityStart = instantMillis(activityDate, 10, 0, zone)
            val session =
                KnitSession(
                    projectId = 1L,
                    startedAt = activityStart,
                    endedAt = activityStart + 30 * 60 * 1_000L,
                    startRow = 0,
                    endRow = 10,
                    durationMinutes = 30,
                    durationSeconds = 30 * 60L,
                    rowsWorked = 10,
                )
            val recentBoundary =
                LocalDate
                    .now(zone)
                    .minusDays(20)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            every { repository.getSessionsForInsights(null, match { it < recentBoundary }) } returns
                flowOf(listOf(session))
            every { repository.getSessionsForInsights(null, match { it >= recentBoundary }) } returns
                flowOf(emptyList())

            val viewModel = createViewModel()
            val initialActivity = viewModel.dailyActivity.first { it.isNotEmpty() }

            viewModel.selectTimeRange(TimeRange.THIS_WEEK)
            advanceUntilIdle()

            assertEquals(30, initialActivity[activityDate])
            assertEquals(30, viewModel.dailyActivity.value[activityDate])
        }

    @Test
    fun `pace over time uses daily buckets sorted by bucket start for ranged views`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val sessions =
            listOf(
                sessionAt(date = today, hour = 10, minute = 0, rows = 24, minutes = 30, zone = zone),
                sessionAt(date = yesterday, hour = 10, minute = 0, rows = 10, minutes = 20, zone = zone),
            )
        val rangeStart = yesterday.atStartOfDay(zone).toInstant().toEpochMilli()

        val points =
            InsightsViewModel.buildPaceOverTime(
                sessions = sessions,
                timeRange = TimeRange.THIS_WEEK,
                rangeStartMillis = rangeStart,
                zone = zone,
            )

        assertEquals(listOf(yesterday, today), points.map { it.bucketStart })
        assertTrue(points.all { it.interval == PaceGroupingInterval.DAY })
        assertEquals(30f, points.first().rowsPerHour, 0.01f)
        assertEquals(48f, points.last().rowsPerHour, 0.01f)
    }

    @Test
    fun `pace over time groups all time by month and sorts out of order sessions`() {
        val zone = ZoneId.of("UTC")
        val january = LocalDate.of(2026, 1, 12)
        val february = LocalDate.of(2026, 2, 2)
        val sessions =
            listOf(
                sessionAt(date = february, hour = 9, minute = 0, rows = 20, minutes = 30, zone = zone),
                sessionAt(date = january, hour = 9, minute = 0, rows = 10, minutes = 30, zone = zone),
            )

        val points =
            InsightsViewModel.buildPaceOverTime(
                sessions = sessions,
                timeRange = TimeRange.ALL_TIME,
                rangeStartMillis = null,
                zone = zone,
            )

        assertEquals(listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)), points.map { it.bucketStart })
        assertTrue(points.all { it.interval == PaceGroupingInterval.MONTH })
    }

    @Test
    fun `pace over time is empty when sessions have no rows`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val session = sessionAt(date = today, hour = 10, minute = 0, rows = 0, minutes = 30, zone = zone)

        val points =
            InsightsViewModel.buildPaceOverTime(
                sessions = listOf(session),
                timeRange = TimeRange.THIS_MONTH,
                rangeStartMillis = today.atStartOfDay(zone).toInstant().toEpochMilli(),
                zone = zone,
            )

        assertEquals(emptyList<PaceOverTimePoint>(), points)
    }

    @Test
    fun `pace over time keeps extreme pace finite`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startedAt = instantMillis(today, 10, 0, zone)
        val session =
            KnitSession(
                projectId = 1L,
                startedAt = startedAt,
                endedAt = startedAt + 1_000L,
                startRow = 0,
                endRow = Int.MAX_VALUE,
                durationMinutes = 1,
                durationSeconds = 1L,
                rowsWorked = Int.MAX_VALUE,
            )

        val points =
            InsightsViewModel.buildPaceOverTime(
                sessions = listOf(session),
                timeRange = TimeRange.ALL_TIME,
                rangeStartMillis = null,
                zone = zone,
            )

        assertTrue(points.single().rowsPerHour.isFinite())
    }

    @Test
    fun `streak counts local activity dates for session crossing midnight`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val session =
            KnitSession(
                projectId = 1L,
                startedAt = instantMillis(yesterday, 23, 50, zone),
                endedAt = instantMillis(today, 0, 20, zone),
                startRow = 0,
                endRow = 12,
                durationMinutes = 30,
                durationSeconds = 30 * 60L,
                rowsWorked = 12,
            )

        assertEquals(2, InsightsViewModel.calculateStreak(listOf(session)))
        assertEquals(2, InsightsViewModel.calculateCurrentStreak(listOf(session)))
    }

    @Test
    fun `streak ignores sessions without time or row activity`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val session =
            KnitSession(
                projectId = 1L,
                startedAt = instantMillis(today, 12, 0, zone),
                endedAt = instantMillis(today, 12, 0, zone),
                startRow = 5,
                endRow = 5,
                durationMinutes = 0,
                durationSeconds = 0L,
                rowsWorked = 0,
            )

        assertEquals(0, InsightsViewModel.calculateStreak(listOf(session)))
        assertEquals(0, InsightsViewModel.calculateCurrentStreak(listOf(session)))
    }

    private fun instantMillis(
        date: LocalDate,
        hour: Int,
        minute: Int,
        zone: ZoneId,
    ): Long =
        ZonedDateTime
            .of(date.year, date.monthValue, date.dayOfMonth, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

    private fun sessionAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        rows: Int,
        minutes: Int,
        zone: ZoneId,
    ): KnitSession {
        val startedAt = instantMillis(date, hour, minute, zone)
        return KnitSession(
            projectId = 1L,
            startedAt = startedAt,
            endedAt = startedAt + minutes * 60 * 1_000L,
            startRow = 0,
            endRow = rows,
            durationMinutes = minutes,
            durationSeconds = minutes * 60L,
            rowsWorked = rows,
        )
    }
}

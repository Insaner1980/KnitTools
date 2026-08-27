package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager
    private lateinit var insightsFeature: MutableStateFlow<Boolean>
    private lateinit var streakFeature: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        proManager = mockk()
        insightsFeature = MutableStateFlow(false)
        streakFeature = MutableStateFlow(false)
        every { repository.getAllProjects() } returns flowOf(emptyList())
        every { repository.getSessionsForInsights(null, null) } returns flowOf(emptyList())
        every { proManager.hasFeature(ProFeature.INSIGHTS_CHARTS) } answers { insightsFeature.value }
        every { proManager.hasFeatureFlow(ProFeature.INSIGHTS_CHARTS) } returns insightsFeature
        every { proManager.hasFeature(ProFeature.STREAK) } answers { streakFeature.value }
        every { proManager.hasFeatureFlow(ProFeature.STREAK) } returns streakFeature
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
            insightsFeature.value = true
            advanceUntilIdle()

            assertEquals(listOf(false, true), values)
            assertTrue(viewModel.isPro.value)
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
            assertEquals(12, state.totalRows)
            assertEquals(emptyList<InsightsChartBucket>(), state.chartBuckets)
            assertFalse(state.hasMeaningfulChartData)
            assertEquals(listOf(1L), state.timePerProject.map { it.projectId })
        }

    @Test
    fun `free insights reports meaningful comparison without exposing chart buckets`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val sessions =
                listOf(
                    sessionAt(date = today.minusDays(1), hour = 10, minute = 0, rows = 8, minutes = 20, zone = zone),
                    sessionAt(date = today, hour = 10, minute = 0, rows = 12, minutes = 30, zone = zone),
                )
            every { repository.getSessionsForInsights(null, null) } returns flowOf(sessions)

            val state = createViewModel().uiState.first { it.hasSessionData }

            assertTrue(state.hasMeaningfulChartData)
            assertEquals(emptyList<InsightsChartBucket>(), state.chartBuckets)
        }

    @Test
    fun `range without sessions keeps the all time flag so the empty state stays intentional`() =
        runTest {
            insightsFeature.value = true
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val oldDate = today.minusDays(40)
            val session = sessionAt(date = oldDate, hour = 10, minute = 0, rows = 10, minutes = 30, zone = zone)
            every { repository.getSessionsForInsights(null, null) } returns flowOf(listOf(session))

            val viewModel = createViewModel()
            viewModel.selectTimeRange(TimeRange.THIS_WEEK)
            advanceUntilIdle()
            val state = viewModel.uiState.first { !it.isLoading && it.timeRange == TimeRange.THIS_WEEK }

            assertTrue(state.hasAnySessionData)
            assertFalse(state.hasSessionData)
            assertEquals(0, state.totalMinutes)
            assertEquals(emptyList<ProjectTime>(), state.timePerProject)
            // Pylväitä on tasan yhtä monta kuin osion "x / y päivää" lupaa, eikä
            // akselille piirretä vielä tulematta olevia päiviä.
            assertEquals(state.daysInRange, state.chartBuckets.size)
            assertEquals(today, state.chartBuckets.last().bucketStart)
            assertTrue(state.chartBuckets.all { it.totalMinutes == 0 })
        }

    @Test
    fun `weekly trend wires the previous period comparison into the ui state`() =
        runTest {
            // Katkaisun reunatapaus (viime viikon loppupään poissulkeminen) on
            // yksikkötestattu kiinteillä päivämäärillä puhtaalle previousPeriodMinutes-
            // funktiolle InsightsChartModelTest:ssä. Tämä testi todistaa vain, että
            // ViewModel kytkee tuloksen oikein tilaan: molemmat istunnot osuvat aina
            // omaan ikkunaansa riippumatta siitä minä viikonpäivänä tai millä
            // firstDayOfWeek-asetuksella testi ajetaan, joten väite pätee joka päivä.
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            val sessions =
                listOf(
                    // Kuluvan viikon ensimmäinen päivä: aina osa nykyistä ikkunaa.
                    sessionAt(date = weekStart, hour = 10, minute = 0, rows = 10, minutes = 60, zone = zone),
                    // Edellisen viikon ensimmäinen päivä: aina osa edellistä ikkunaa,
                    // koska ikkuna on aina vähintään yhden päivän mittainen.
                    sessionAt(
                        date = weekStart.minusWeeks(1),
                        hour = 10,
                        minute = 0,
                        rows = 10,
                        minutes = 30,
                        zone = zone,
                    ),
                )
            every { repository.getSessionsForInsights(null, null) } returns flowOf(sessions)

            val viewModel = createViewModel()
            viewModel.selectTimeRange(TimeRange.THIS_WEEK)
            advanceUntilIdle()

            val state = viewModel.uiState.first { !it.isLoading }

            assertEquals(InsightsTrendDirection.UP, state.trend?.direction)
            assertEquals(100, state.trend?.percentChange)
        }

    @Test
    fun `all time has no previous period to compare against`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            every { repository.getSessionsForInsights(null, null) } returns
                flowOf(listOf(sessionAt(date = today, hour = 10, minute = 0, rows = 10, minutes = 60, zone = zone)))

            val viewModel = createViewModel()
            viewModel.selectTimeRange(TimeRange.ALL_TIME)
            advanceUntilIdle()

            val state = viewModel.uiState.first { !it.isLoading }

            assertNull(state.trend)
        }

    @Test
    fun `minutes per row replaces rows per hour in the stats row`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val session = sessionAt(date = today, hour = 10, minute = 0, rows = 10, minutes = 90, zone = zone)
            every { repository.getSessionsForInsights(null, null) } returns flowOf(listOf(session))

            val viewModel = createViewModel()
            val state = viewModel.uiState.first { it.hasSessionData }

            assertEquals(MinutesPerRowDisplay.Minutes(9), state.minutesPerRow)
        }

    @Test
    fun `minutes per row uses exact session seconds`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val startedAt = instantMillis(LocalDate.now(zone), 10, 0, zone)
            val session =
                KnitSession(
                    projectId = 1L,
                    startedAt = startedAt,
                    endedAt = startedAt + 61_000L,
                    startRow = 0,
                    endRow = 1,
                    durationMinutes = 2,
                    durationSeconds = 61L,
                    rowsWorked = 1,
                    zoneId = zone.id,
                )
            every { repository.getSessionsForInsights(null, null) } returns flowOf(listOf(session))

            val state = createViewModel().uiState.first { !it.isLoading }

            assertEquals(MinutesPerRowDisplay.Minutes(1), state.minutesPerRow)
        }

    @Test
    fun `loading remains visible until both repository flows emit`() =
        runTest {
            val projectRows = MutableSharedFlow<List<CounterProject>>(replay = 1)
            val sessionRows = MutableSharedFlow<List<KnitSession>>(replay = 1)
            every { repository.getAllProjects() } returns projectRows
            every { repository.getSessionsForInsights(null, null) } returns sessionRows
            val viewModel = createViewModel()
            val observed = mutableListOf<InsightsUiState>()
            val job = launch { viewModel.uiState.take(2).toList(observed) }
            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)

            projectRows.emit(emptyList())
            sessionRows.emit(emptyList())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            job.cancel()
        }

    @Test
    fun `chart buckets use daily grouping for ranged views`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val sessions =
            listOf(
                sessionAt(date = today, hour = 10, minute = 0, rows = 24, minutes = 30, zone = zone),
                sessionAt(date = yesterday, hour = 10, minute = 0, rows = 10, minutes = 20, zone = zone),
            )
        val rangeStart = yesterday.atStartOfDay(zone).toInstant().toEpochMilli()
        val axis =
            InsightsChartAxis(
                interval = PaceGroupingInterval.DAY,
                bucketStarts = listOf(yesterday, today),
            )

        val buckets =
            fillChartBuckets(
                axis = axis,
                measured =
                    InsightsViewModel.measuredChartBuckets(
                        sessions = sessions,
                        params =
                            InsightsQueryParams(
                                timeRange = TimeRange.THIS_WEEK,
                                startMillis = rangeStart,
                                currentDate = today,
                            ),
                        axis = axis,
                        zone = zone,
                        firstDayOfWeek = DayOfWeek.MONDAY,
                    ),
            )

        assertEquals(listOf(yesterday, today), buckets.map { it.bucketStart })
        assertEquals(listOf(20, 30), buckets.map { it.totalMinutes })
        assertEquals(listOf(10, 24), buckets.map { it.totalRows })
    }

    @Test
    fun `ranged chart keeps a session-local bucket after the device zone changes`() {
        val sessionZone = ZoneId.of("Pacific/Kiritimati")
        val currentDeviceZone = ZoneId.of("Pacific/Honolulu")
        val currentDeviceDate = LocalDate.now(currentDeviceZone)
        // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
        val sessionDate = currentDeviceDate.plusDays(1)
        val startedAt = instantMillis(sessionDate, 0, 30, sessionZone)
        val session =
            KnitSession(
                projectId = 1L,
                startedAt = startedAt,
                endedAt = startedAt + 30 * 60 * 1_000L,
                startRow = 0,
                endRow = 10,
                durationMinutes = 30,
                durationSeconds = 30 * 60L,
                rowsWorked = 10,
                zoneId = sessionZone.id,
            )
        // CPD-ON
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.THIS_MONTH,
                today = currentDeviceDate,
                firstSessionDate = sessionDate,
                firstDayOfWeek = DayOfWeek.MONDAY,
            )

        val measured =
            InsightsViewModel.measuredChartBuckets(
                sessions = listOf(session),
                params =
                    InsightsQueryParams(
                        timeRange = TimeRange.THIS_MONTH,
                        startMillis =
                            currentDeviceDate
                                .withDayOfMonth(1)
                                .atStartOfDay(currentDeviceZone)
                                .toInstant()
                                .toEpochMilli(),
                        currentDate = currentDeviceDate,
                    ),
                axis = axis,
                zone = currentDeviceZone,
                firstDayOfWeek = DayOfWeek.MONDAY,
            )

        assertEquals(
            listOf(sessionDate),
            measured.values.filter { it.totalMinutes > 0 }.map { it.bucketStart },
        )

        // Akseli päättyy nykyiseen laitepäivään, joten tuleva paikallinen päivä
        // rajataan tarkoituksella pois renderöidyistä pylväistä.
        val rendered = fillChartBuckets(axis, measured)
        assertTrue(rendered.none { it.bucketStart == sessionDate })
    }

    @Test
    fun `all time start uses each session persisted zone`() {
        val sessionZone = ZoneId.of("Pacific/Kiritimati")
        val currentDeviceZone = ZoneId.of("Pacific/Honolulu")
        val sessionDate = LocalDate.of(2026, 1, 2)
        val startedAt = instantMillis(sessionDate, 0, 30, sessionZone)
        val session =
            KnitSession(
                projectId = 1L,
                startedAt = startedAt,
                endedAt = startedAt + 30 * 60 * 1_000L,
                startRow = 0,
                endRow = 10,
                durationMinutes = 30,
                durationSeconds = 30 * 60L,
                rowsWorked = 10,
                zoneId = sessionZone.id,
            )

        assertEquals(
            sessionDate,
            InsightsViewModel.firstSessionDate(listOf(session), currentDeviceZone),
        )
    }

    @Test
    fun `all time chart groups out of order sessions by month`() {
        val zone = ZoneId.of("UTC")
        // Yli puolen vuoden historia ryhmitellään kuukausiin; lyhyempi menisi viikkoihin.
        val january = LocalDate.of(2026, 1, 12)
        val february = LocalDate.of(2026, 9, 2)
        val sessions =
            listOf(
                sessionAt(date = february, hour = 9, minute = 0, rows = 20, minutes = 30, zone = zone),
                sessionAt(date = january, hour = 9, minute = 0, rows = 10, minutes = 30, zone = zone),
            )
        val axis =
            insightsChartAxis(
                timeRange = TimeRange.ALL_TIME,
                today = february,
                firstSessionDate = january,
                firstDayOfWeek = DayOfWeek.MONDAY,
            )

        val buckets =
            fillChartBuckets(
                axis = axis,
                measured =
                    InsightsViewModel.measuredChartBuckets(
                        sessions = sessions,
                        params = InsightsQueryParams(timeRange = TimeRange.ALL_TIME, currentDate = february),
                        axis = axis,
                        zone = zone,
                        firstDayOfWeek = DayOfWeek.MONDAY,
                    ),
            )

        assertEquals(PaceGroupingInterval.MONTH, axis.interval)
        assertEquals(LocalDate.of(2026, 1, 1), buckets.first().bucketStart)
        assertEquals(LocalDate.of(2026, 9, 1), buckets.last().bucketStart)
        // Molemmilla istunnoilla on 30 minuuttia; rivit erottavat ne toisistaan.
        assertEquals(listOf(10, 20), buckets.filter { it.totalRows > 0 }.map { it.totalRows })
    }

    @Test
    fun `chart keeps minutes for sessions that recorded no rows`() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val session = sessionAt(date = today, hour = 10, minute = 0, rows = 0, minutes = 30, zone = zone)
        val axis =
            InsightsChartAxis(interval = PaceGroupingInterval.DAY, bucketStarts = listOf(today))

        val buckets =
            fillChartBuckets(
                axis = axis,
                measured =
                    InsightsViewModel.measuredChartBuckets(
                        sessions = listOf(session),
                        params =
                            InsightsQueryParams(
                                timeRange = TimeRange.THIS_MONTH,
                                startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli(),
                                currentDate = today,
                            ),
                        axis = axis,
                        zone = zone,
                        firstDayOfWeek = DayOfWeek.MONDAY,
                    ),
            )

        assertEquals(30, buckets.single().totalMinutes)
        assertEquals(0, buckets.single().totalRows)
    }

    @Test
    fun `chart buckets stack by project in the given order and sum to the bucket total`() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 3, 4)
        val sessions =
            listOf(
                sessionAt(date = day, hour = 9, minute = 0, rows = 10, minutes = 30, zone = zone)
                    .copy(projectId = 7L),
                sessionAt(date = day, hour = 12, minute = 0, rows = 4, minutes = 45, zone = zone)
                    .copy(projectId = 3L),
            )
        val axis = InsightsChartAxis(interval = PaceGroupingInterval.DAY, bucketStarts = listOf(day))

        val bucket =
            InsightsViewModel
                .measuredChartBuckets(
                    sessions = sessions,
                    params = InsightsQueryParams(timeRange = TimeRange.ALL_TIME, currentDate = day),
                    axis = axis,
                    zone = zone,
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    projectOrder = listOf(3L, 7L),
                ).getValue(day)

        assertEquals(listOf(3L, 7L), bucket.segments.map { it.projectId })
        assertEquals(listOf(45, 30), bucket.segments.map { it.minutes })
        assertEquals(bucket.totalMinutes, bucket.segments.sumOf { it.minutes })
        assertEquals(14, bucket.totalRows)
    }

    @Test
    fun `days in range counts the elapsed days of the selected range`() {
        val today = LocalDate.of(2026, 7, 28)

        assertEquals(
            28,
            InsightsViewModel.daysInRange(TimeRange.THIS_MONTH, today, null, DayOfWeek.MONDAY),
        )
        assertEquals(
            2,
            InsightsViewModel.daysInRange(TimeRange.THIS_WEEK, today, null, DayOfWeek.MONDAY),
        )
        assertEquals(
            0,
            InsightsViewModel.daysInRange(TimeRange.ALL_TIME, today, null, DayOfWeek.MONDAY),
        )
        assertEquals(
            10,
            InsightsViewModel.daysInRange(
                TimeRange.ALL_TIME,
                today,
                LocalDate.of(2026, 7, 19),
                DayOfWeek.MONDAY,
            ),
        )
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

    @Test
    fun `project fabric is exposed only for pro all time insights`() =
        runTest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            every { repository.getSessionsForInsights(null, null) } returns
                flowOf(listOf(sessionAt(date = today, hour = 10, minute = 0, rows = 10, minutes = 30, zone = zone)))

            val viewModel = createViewModel()
            val freeState = viewModel.uiState.first { it.hasSessionData }

            assertNull(freeState.projectFabric)

            insightsFeature.value = true
            val proState = viewModel.uiState.first { it.projectFabric != null }

            assertEquals(1, proState.projectFabric?.activeDayCount)

            viewModel.selectTimeRange(TimeRange.THIS_WEEK)
            val weeklyState = viewModel.uiState.first { !it.isLoading && it.timeRange == TimeRange.THIS_WEEK }

            assertNull(weeklyState.projectFabric)
        }

    @Test
    fun `project fabric uses the same project order as the chart and project list`() =
        runTest {
            insightsFeature.value = true
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val sessions =
                listOf(
                    sessionAt(date = today, hour = 9, minute = 0, rows = 10, minutes = 30, zone = zone)
                        .copy(projectId = 7L),
                    sessionAt(date = today, hour = 11, minute = 0, rows = 20, minutes = 60, zone = zone)
                        .copy(projectId = 3L),
                )
            every { repository.getSessionsForInsights(null, null) } returns flowOf(sessions)

            val state = createViewModel().uiState.first { it.projectFabric != null }

            val expectedOrder = state.timePerProject.map { it.projectId }
            assertEquals(listOf(3L, 7L), expectedOrder)
            assertEquals(
                expectedOrder,
                state.projectFabric
                    ?.days
                    ?.single { it.date == today }
                    ?.projectIds,
            )
        }

    @Test
    fun `local date changes emits again after midnight`() =
        runTest {
            val zone = ZoneId.of("Europe/Helsinki")
            val firstDate = LocalDate.of(2026, 7, 12)
            var nowMillis = instantMillis(firstDate, 23, 59, zone) + 50_000L
            val dates = mutableListOf<LocalDate>()
            val job =
                launch {
                    localDateChanges(
                        nowMillis = { nowMillis },
                        zoneProvider = { zone },
                    ).take(2).toList(dates)
                }

            runCurrent()
            nowMillis += 10_000L
            advanceTimeBy(10_000L)
            runCurrent()

            assertEquals(listOf(firstDate, firstDate.plusDays(1)), dates)
            job.cancel()
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

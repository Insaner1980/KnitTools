package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsSessionZoneRangeTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val deviceZone = ZoneId.of("Pacific/Honolulu")
    private val repository = mockInsightsRepository()
    private val proManager = mockk<ProManager>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(::insightsCalendarChanges)
        every { repository.getAllProjects() } returns flowOf(listOf(CounterProject(1, "A"), CounterProject(2, "B")))
        every { proManager.hasFeature(any()) } returns true
        every { proManager.hasFeatureFlow(any()) } returns flowOf(true)
    }

    @After
    fun tearDown() {
        unmockkStatic(::insightsCalendarChanges)
        Dispatchers.resetMain()
    }

    @Test
    fun singularDenominatorExcludesTheLaterStoredDateFromTheDateSet() =
        runTest {
            val rows = listOf(session("2026-10-01T00:00-10:00"), session("2026-10-02T00:30+14:00"))
            val state = stateAt("2026-10-01T02:00-10:00", rows)

            assertRange(state, "2026-10-01", "2026-10-01", 1, 1, 60)
            assertEnglish(state, "Active 1 of 1 day", "Oct 2026")
            assertEquals(listOf(30), state.chartBuckets.map { it.totalMinutes })
        }

    @Test
    fun pluralDenominatorExcludesTheLaterStoredDateFromTheDateSet() =
        runTest {
            val rows =
                listOf(
                    session("2026-10-01T00:00-10:00"),
                    session("2026-10-02T00:00-10:00"),
                    session("2026-10-03T00:30+14:00"),
                )
            val state = stateAt("2026-10-02T02:00-10:00", rows)

            assertRange(state, "2026-10-01", "2026-10-02", 2, 2, 90)
            assertEnglish(state, "Active 2 of 2 days", "Oct 2026")
            assertEquals(listOf(30, 30), state.chartBuckets.map { it.totalMinutes })
        }

    @Test
    fun allTimeStartCannotFollowTheDeviceCalendarEnd() =
        runTest {
            val rows = listOf(session("2026-11-01T00:30+14:00"))
            val state = stateAt("2026-10-31T02:00-10:00", rows)

            assertRange(state, "2026-10-31", "2026-10-31", 0, 1, 30)
            assertEnglish(state, "Active 0 of 1 day", "Oct 2026")
            assertEquals("2026-11-01", InsightsViewModel.firstSessionDate(rows, deviceZone).toString())
            assertEquals(0, state.chartBuckets.single().totalMinutes)
        }

    @Test
    fun sameZoneControlAndProjectFilterKeepOrdinaryRangeBehavior() =
        runTest {
            val rows =
                listOf(
                    session("2026-09-30T00:00-10:00"),
                    session("2026-10-01T00:00-10:00"),
                    session("2026-10-02T00:00-10:00").copy(projectId = 2),
                )
            val all = stateAt("2026-10-02T02:00-10:00", rows)
            assertRange(all, "2026-09-30", "2026-10-02", 3, 3, 90)
            assertEnglish(all, "Active 3 of 3 days", "Sep 2026 – Oct 2026")

            val month = stateAt("2026-10-02T02:00-10:00", rows, TimeRange.THIS_MONTH)
            assertRange(month, "2026-10-01", "2026-10-02", 2, 2, 60)
            val week = stateAt("2026-10-02T02:00-10:00", rows, TimeRange.THIS_WEEK)
            assertEquals(3, week.activeDays)
            assertEquals(90, week.totalMinutes)
            assertEquals(
                InsightsViewModel.rangeStartDate(TimeRange.THIS_WEEK, week.rangeEnd, currentInsightsFirstDayOfWeek()),
                week.rangeStart,
            )
            val selected = stateAt("2026-10-02T02:00-10:00", rows, projectId = 2)
            assertRange(selected, "2026-10-02", "2026-10-02", 1, 1, 30)
            assertEquals(listOf(2L), selected.timePerProject.map { it.projectId })
        }

    @Test
    fun fixedRangesUseTheSameBoundedActiveDatesWithoutChangingDurations() =
        runTest {
            val rows = listOf(session("2026-10-01T00:00-10:00"), session("2026-10-02T00:30+14:00"))
            for (range in listOf(TimeRange.THIS_WEEK, TimeRange.THIS_MONTH)) {
                val state = stateAt("2026-10-01T02:00-10:00", rows, range)
                assertEquals(1, state.activeDays)
                assertEquals(60, state.totalMinutes)
                assertTrue(state.activeDays <= state.daysInRange)
            }
        }

    @Test
    fun adaptiveFixtureKeepsExactTotalsAndSparseChartsAtCalendarBoundaries() =
        runTest {
            mockkStatic(::currentInsightsFirstDayOfWeek)
            try {
                for (firstDay in listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)) {
                    every { currentInsightsFirstDayOfWeek() } returns firstDay
                    for (date in listOf("2026-11-01", "2026-11-02", "2026-11-08", "2026-11-09", "2027-01-01")) {
                        val today = LocalDate.parse(date)
                        val rows =
                            listOf(0L, 1L, 14L, 90L, 700L).map {
                                session("${today.minusDays(it)}T10:00-10:00")
                            }
                        val week = stateAt("${date}T12:00-10:00", rows, TimeRange.THIS_WEEK)
                        val weekDays = if (today.dayOfWeek == firstDay) 1 else 2
                        assertEquals(weekDays * 30, week.totalMinutes)
                        assertEquals(weekDays, week.activeDays)
                        assertEquals(weekDays >= 2, week.hasMeaningfulChartData)
                        assertEquals(weekDays, week.chartBuckets.count { it.totalMinutes > 0 })

                        val month = stateAt("${date}T12:00-10:00", rows, TimeRange.THIS_MONTH)
                        val monthDays = if (today.dayOfMonth == 1) 1 else 2
                        assertEquals(monthDays * 30, month.totalMinutes)
                        assertEquals(monthDays, month.activeDays)
                        assertEquals(monthDays >= 2, month.hasMeaningfulChartData)
                        assertEquals(monthDays, month.chartBuckets.count { it.totalMinutes > 0 })

                        val all = stateAt("${date}T12:00-10:00", rows)
                        assertEquals(150, all.totalMinutes)
                        assertEquals(5, all.activeDays)
                        assertTrue(all.hasMeaningfulChartData)
                    }
                }
            } finally {
                unmockkStatic(::currentInsightsFirstDayOfWeek)
            }
        }

    @Test
    fun weekAndMonthStartIncludeOnlyThePostMidnightPartOfSessions() =
        runTest {
            mockkStatic(::currentInsightsFirstDayOfWeek)
            try {
                every { currentInsightsFirstDayOfWeek() } returns DayOfWeek.SUNDAY
                val rows =
                    listOf(
                        session("2026-10-31T23:30-10:00"),
                        session("2026-10-31T23:45-10:00"),
                        session("2026-11-01T00:00-10:00"),
                    )
                for (range in listOf(TimeRange.THIS_WEEK, TimeRange.THIS_MONTH)) {
                    val state = stateAt("2026-11-01T12:00-10:00", rows, range)
                    assertRange(state, "2026-11-01", "2026-11-01", 1, 1, 45)
                    assertFalse(state.hasMeaningfulChartData)
                    assertEquals(45, state.chartBuckets.single().totalMinutes)
                }
            } finally {
                unmockkStatic(::currentInsightsFirstDayOfWeek)
            }
        }

    private suspend fun TestScope.stateAt(
        now: String,
        rows: List<KnitSession>,
        range: TimeRange = TimeRange.ALL_TIME,
        projectId: Long? = null,
    ): InsightsUiState {
        val instant = OffsetDateTime.parse(now).toInstant()
        val today = instant.atZone(deviceZone).toLocalDate()
        assertTrue("Every completed session ends before now", rows.all { it.endedAt < instant.toEpochMilli() })
        every { insightsCalendarChanges(any(), any()) } returns flowOf(InsightsCalendar(today, deviceZone))
        stubSessions(repository, flowOf(rows))
        val viewModel =
            InsightsViewModel(
                repository,
                proManager,
                dispatcher,
                SavedStateHandle(),
                mockk(relaxed = true),
                backgroundScope,
            )
        viewModel.selectTimeRange(range)
        viewModel.selectProject(projectId)
        return viewModel.uiState.first { !it.isLoading && it.rangeEnd == today }
    }

    private fun assertRange(
        state: InsightsUiState,
        start: String,
        end: String,
        active: Int,
        days: Int,
        minutes: Int,
    ) {
        assertEquals(start, state.rangeStart.toString())
        assertEquals(end, state.rangeEnd.toString())
        assertEquals(active, state.activeDays)
        assertEquals(days, state.daysInRange)
        assertEquals(minutes, state.totalMinutes)
        assertTrue(state.hasSessionData)
        assertFalse(state.rowMetricsAvailable)
        assertEquals(MinutesPerRowDisplay.Unavailable, state.minutesPerRow)
    }

    // Compose wiring is source-checked; JVM composition uses the actual unchanged English resources.
    private fun assertEnglish(
        state: InsightsUiState,
        expectedMeta: String,
        expectedRange: String,
    ) {
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsScreen.kt",
            )
        val compact = source.replace(Regex("\\s+"), " ")
        assertTrue(
            compact.contains(
                "R.plurals.insights_days_meta_format, uiState.daysInRange, uiState.activeDays, uiState.daysInRange,",
            ),
        )
        assertTrue(compact.contains("if (!hasUserSelection || bucket == null) return daysMetaText(uiState)"))
        assertTrue(compact.contains("craftVerbText(uiState.craftMix), rangeLabel, daysMetaText(uiState),"))
        val xml = ProjectSourceFiles.read("app/src/main/res/values/strings.xml")
        val plural =
            requireNotNull(
                Regex(
                    "<plurals name=\"insights_days_meta_format\">(.*?)</plurals>",
                    RegexOption.DOT_MATCHES_ALL,
                ).find(xml),
            ).groupValues[1]
        val quantity = if (state.daysInRange == 1) "one" else "other"
        val pattern = requireNotNull(Regex("<item quantity=\"$quantity\">(.*?)</item>").find(plural)).groupValues[1]
        val meta = String.format(Locale.US, pattern, state.activeDays, state.daysInRange)
        assertEquals(expectedMeta, meta)
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
        val first = requireNotNull(state.rangeStart).withDayOfMonth(1)
        val last = state.rangeEnd.withDayOfMonth(1)
        val rangeLabel =
            if (first == last) {
                first.format(formatter)
            } else {
                String.format(
                    Locale.US,
                    stringValue(xml, "insights_range_format"),
                    first.format(formatter),
                    last.format(formatter),
                )
            }
        assertEquals(expectedRange, rangeLabel)
        for (interval in listOf("daily", "weekly", "monthly")) {
            val description =
                String.format(
                    Locale.US,
                    stringValue(xml, "insights_chart_a11y_$interval"),
                    "knitting",
                    rangeLabel,
                    meta,
                    "30 min",
                )
            assertTrue(description.contains(expectedMeta))
            assertTrue(description.contains(expectedRange))
        }
    }

    private fun stringValue(
        xml: String,
        name: String,
    ): String = requireNotNull(Regex("<string name=\"$name\">(.*?)</string>").find(xml)).groupValues[1]

    private fun session(start: String): KnitSession {
        val time = OffsetDateTime.parse(start)
        val millis = time.toInstant().toEpochMilli()
        return KnitSession(
            projectId = 1,
            startedAt = millis,
            endedAt = millis + 1_800_000,
            startRow = 0,
            endRow = 10,
            durationMinutes = 30,
            durationSeconds = 1800,
            rowsWorked = 10,
            zoneId = if (time.offset.totalSeconds > 0) "Pacific/Kiritimati" else deviceZone.id,
        )
    }
}

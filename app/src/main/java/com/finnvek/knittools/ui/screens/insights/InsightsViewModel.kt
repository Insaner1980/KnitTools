package com.finnvek.knittools.ui.screens.insights

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.calculator.DurationDisplay
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.calculator.MinutesPerRowFormatter
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.ProjectCompletion
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.yield
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneId.systemDefault
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import javax.inject.Inject

private const val DATE_CHANGE_CHECK_INTERVAL_MILLIS = 60_000L
private const val MINIMUM_MEANINGFUL_CHART_BUCKETS = 2

private sealed interface RepositoryLoad<out T> {
    val value: T?

    data object Loading : RepositoryLoad<Nothing> {
        override val value: Nothing? = null
    }

    data class Loaded<T>(
        override val value: T,
    ) : RepositoryLoad<T>
}

private fun <T> Flow<T>.withLoadingState(): Flow<RepositoryLoad<T>> =
    map<T, RepositoryLoad<T>> { RepositoryLoad.Loaded(it) }
        .onStart { emit(RepositoryLoad.Loading) }

data class ProjectTime(
    val projectId: Long,
    val projectName: String?,
    val totalMinutes: Int,
    val totalRows: Int,
    val lastSessionAt: Long,
)

enum class PaceGroupingInterval {
    DAY,
    WEEK,
    MONTH,
}

enum class TimeRange {
    ALL_TIME,
    THIS_WEEK,
    THIS_MONTH,
}

/** Mitä käsityölajia valinnassa on tehty. Ohjaa hero-tekstin verbin. */
enum class InsightsCraftMix {
    KNITTING,
    CROCHET,
    MIXED,
}

@Immutable
internal data class InsightsUiState(
    val isLoading: Boolean = true,
    val totalDuration: DurationDisplay = DurationDisplayFormatter.fromMinutes(0),
    val totalMinutes: Int = 0,
    val totalRows: Int = 0,
    val minutesPerRow: MinutesPerRowDisplay = MinutesPerRowDisplay.Unavailable,
    val activeDays: Int = 0,
    val daysInRange: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val trend: InsightsTrend? = null,
    val projects: List<CounterProject> = emptyList(),
    val selectedProjectId: Long? = null,
    val selectedProjectName: String? = null,
    val craftMix: InsightsCraftMix = InsightsCraftMix.KNITTING,
    val timePerProject: List<ProjectTime> = emptyList(),
    val projectFabric: InsightsProjectFabricModel? = null,
    val chartInterval: PaceGroupingInterval = PaceGroupingInterval.DAY,
    val chartBuckets: List<InsightsChartBucket> = emptyList(),
    val hasMeaningfulChartData: Boolean = false,
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate = LocalDate.now(systemDefault()),
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val hasSessionData: Boolean = false,
    val hasAnySessionData: Boolean = false,
    val hasAnyCompletionData: Boolean = false,
    val completions: InsightsCompletions = InsightsCompletions(),
    val isPro: Boolean = false,
    val canUseStreak: Boolean = false,
)

@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        private val counterRepository: CounterRepository,
        private val proManager: ProManager,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        val isPro: StateFlow<Boolean> =
            proManager
                .hasFeatureFlow(ProFeature.INSIGHTS_CHARTS)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    proManager.hasFeature(ProFeature.INSIGHTS_CHARTS),
                )
        val canUseStreak: StateFlow<Boolean> =
            proManager
                .hasFeatureFlow(ProFeature.STREAK)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    proManager.hasFeature(ProFeature.STREAK),
                )
        private val proFeatureGates: StateFlow<InsightsProFeatureGates> =
            combine(isPro, canUseStreak) { chartsAllowed, streakAllowed ->
                InsightsProFeatureGates(
                    canUseCharts = chartsAllowed,
                    canUseStreak = streakAllowed,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                InsightsProFeatureGates(
                    canUseCharts = proManager.hasFeature(ProFeature.INSIGHTS_CHARTS),
                    canUseStreak = proManager.hasFeature(ProFeature.STREAK),
                ),
            )

        private val _selectedProjectId = MutableStateFlow<Long?>(null)
        private val _timeRange = MutableStateFlow(TimeRange.ALL_TIME)
        val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()
        val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()
        private val currentDate: StateFlow<InsightsCalendar> =
            insightsCalendarChanges()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    captureInsightsCalendar(),
                )

        private val projectLoad: StateFlow<RepositoryLoad<List<CounterProject>>> =
            counterRepository
                .getAllProjects()
                .distinctUntilChanged()
                .withLoadingState()
                .onEach { load ->
                    val projects = load.value ?: return@onEach
                    val selectedId = _selectedProjectId.value ?: return@onEach
                    if (projects.none { it.id == selectedId }) {
                        _selectedProjectId.value = null
                    }
                }.flowOn(ioDispatcher)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    RepositoryLoad.Loading,
                )

        private val queryParams: Flow<InsightsQueryParams> =
            combine(selectedProjectId, timeRange, currentDate) { projectId, activeTimeRange, date ->
                InsightsQueryParams(
                    projectId = projectId,
                    timeRange = activeTimeRange,
                    startMillis =
                        rangeStartDate(
                            activeTimeRange,
                            date.date,
                            WeekFields.of(currentInsightsLocale()).firstDayOfWeek,
                        )?.atStartOfDay(date.zone)?.toInstant()?.toEpochMilli(),
                    currentDate = date.date,
                    zone = date.zone,
                )
            }.distinctUntilChanged()

        private val completionLoad =
            counterRepository
                .observeCompletions()
                .distinctUntilChanged()
                .withLoadingState()

        @OptIn(ExperimentalCoroutinesApi::class)
        internal val uiState: StateFlow<InsightsUiState> =
            queryParams
                .flatMapLatest { params ->
                    val sessions =
                        counterRepository
                            .observeSessionsForInsights(
                                projectId = params.projectId,
                                start = insightsQueryStart(params),
                                zone = params.zone,
                                create = { facts -> InsightsSessionAccumulator(params, facts) },
                                accumulate = { accumulator, batch ->
                                    batch.forEach { session ->
                                        yield()
                                        accumulator.add(session)
                                    }
                                },
                            ).withLoadingState()
                    combine(
                        sessions,
                        projectLoad,
                        proFeatureGates,
                        completionLoad,
                    ) { loadedSessions, projects, gates, completions ->
                        InsightsComputationInput(loadedSessions, projects, params, gates, completions)
                    }.mapLatest { input ->
                        val projectList = input.projects.value
                        val sessionMetrics = input.sessions.value
                        if (sessionMetrics == null || projectList == null || input.completions.value == null) {
                            InsightsUiState(
                                projects = projectList.orEmpty(),
                                selectedProjectId = params.projectId,
                                selectedProjectName = projectList?.firstOrNull { it.id == params.projectId }?.name,
                                rangeStart =
                                    rangeStartDate(
                                        params.timeRange,
                                        params.currentDate,
                                        WeekFields.of(currentInsightsLocale()).firstDayOfWeek,
                                    ),
                                rangeEnd = params.currentDate,
                                timeRange = params.timeRange,
                                isPro = input.featureGates.canUseCharts,
                                canUseStreak = input.featureGates.canUseStreak,
                            )
                        } else {
                            buildUiState(
                                requireNotNull(input.completions.value),
                                sessionMetrics,
                                projectList,
                                params,
                                input.featureGates,
                            )
                        }
                    }
                }.distinctUntilChanged()
                .flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

        @Suppress("LongMethod") // Yksi koonti pitää Insights-tilan keskenään riippuvat laskelmat samassa paikassa.
        private fun buildUiState(
            completions: List<ProjectCompletion>,
            sessions: InsightsSessionAccumulator,
            projectList: List<CounterProject>,
            params: InsightsQueryParams,
            featureGates: InsightsProFeatureGates,
        ): InsightsUiState {
            val today = params.currentDate
            val firstDayOfWeek = WeekFields.of(currentInsightsLocale()).firstDayOfWeek
            val rangeMetrics = sessions.summary
            val firstSessionDate = sessions.firstDate
            val axis = insightsChartAxis(params.timeRange, today, firstSessionDate, firstDayOfWeek)
            val timePerProject = sessions.timePerProject(projectList)
            val projectOrder = timePerProject.map { it.projectId }
            val projectFabric =
                if (params.timeRange == TimeRange.ALL_TIME && featureGates.canUseCharts) {
                    buildInsightsProjectFabric(sessions.fabric.values, today, firstDayOfWeek, projectOrder)
                } else {
                    null
                }
            val measuredBuckets = chartBucketsFromMetrics(sessions.chart(axis.interval).values, projectOrder)
            val hasMeaningfulChartData =
                measuredBuckets.values.count { it.totalMinutes > 0 } +
                    sessions.chart(axis.interval).futureMeasuredBucketCount >=
                    MINIMUM_MEANINGFUL_CHART_BUCKETS
            val chartBuckets =
                if (featureGates.canUseCharts) fillChartBuckets(axis, measuredBuckets) else emptyList()
            val minutesPerRow =
                MinutesPerRowFormatter.fromSeconds(rangeMetrics.totalSeconds, rangeMetrics.totalRows)

            val completionHistory = buildInsightsCompletions(completions, params, firstDayOfWeek)
            return InsightsUiState(
                isLoading = false,
                hasAnyCompletionData = completions.isNotEmpty(),
                completions =
                    if (featureGates.canUseCharts) {
                        completionHistory
                    } else {
                        completionHistory.copy(
                            buckets = emptyList(),
                        )
                    },
                totalDuration = DurationDisplayFormatter.fromMinutes(rangeMetrics.totalMinutes),
                totalMinutes = rangeMetrics.totalMinutes,
                totalRows = rangeMetrics.totalRows,
                minutesPerRow = minutesPerRow,
                activeDays = sessions.activeDates.size,
                daysInRange = daysInRange(params.timeRange, today, firstSessionDate, firstDayOfWeek),
                currentStreak = if (featureGates.canUseStreak) currentStreak(sessions.activeDates, today) else 0,
                bestStreak = if (featureGates.canUseStreak) sessions.activeDates.bestStreak() else 0,
                trend = sessions.trend(),
                projects = projectList,
                selectedProjectId = params.projectId,
                selectedProjectName = projectList.firstOrNull { it.id == params.projectId }?.name,
                craftMix = craftMix(timePerProject, projectList),
                timePerProject = timePerProject,
                projectFabric = projectFabric,
                chartInterval = axis.interval,
                chartBuckets = chartBuckets,
                hasMeaningfulChartData = hasMeaningfulChartData,
                rangeStart = rangeStartDate(params.timeRange, today, firstDayOfWeek) ?: firstSessionDate,
                rangeEnd = today,
                timeRange = params.timeRange,
                hasSessionData = rangeMetrics.sessionCount > 0,
                hasAnySessionData = sessions.facts.hasAnySessionData,
                isPro = featureGates.canUseCharts,
                canUseStreak = featureGates.canUseStreak,
            )
        }

        fun selectProject(projectId: Long?) {
            _selectedProjectId.value = projectId
        }

        fun selectTimeRange(selectedTimeRange: TimeRange) {
            _timeRange.value = selectedTimeRange
        }

        companion object {
            /**
             * Laskee pisimmän peräkkäisten neulontapäivien ketjun.
             */
            fun calculateStreak(
                sessions: List<KnitSession>,
                rangeStartMillis: Long? = null,
            ): Int {
                val days = activityDayKeys(sessions, rangeStartMillis).sorted()
                if (days.isEmpty()) return 0
                var maxStreak = 1
                var currentStreak = 1
                for (i in 1 until days.size) {
                    if (days[i] - days[i - 1] == 1L) {
                        currentStreak++
                        if (currentStreak > maxStreak) maxStreak = currentStreak
                    } else {
                        currentStreak = 1
                    }
                }
                return maxStreak
            }

            fun calculateCurrentStreak(
                sessions: List<KnitSession>,
                rangeStartMillis: Long? = null,
                zone: ZoneId = ZoneId.systemDefault(),
                currentDate: LocalDate = LocalDate.now(zone),
            ): Int {
                val activeDates = activityDates(sessions, rangeStartMillis, zone)
                if (activeDates.isEmpty()) return 0
                val anchor =
                    when {
                        activeDates.contains(currentDate) -> currentDate
                        activeDates.contains(currentDate.minusDays(1)) -> currentDate.minusDays(1)
                        else -> return 0
                    }

                var streak = 0
                var currentDate = anchor
                while (activeDates.contains(currentDate)) {
                    streak++
                    currentDate = currentDate.minusDays(1)
                }
                return streak
            }

            /**
             * Mitatut ämpärit ilman nolla-täydennystä. Rivimäärät tulevat istuntodatasta,
             * niitä ei arvioida.
             *
             * Ämpärit lasketaan projekteittain, jotta kaavio voi pinota päivän ajan
             * projektien väreillä. Minuutit jaetaan ämpärin sisällä samalla
             * [apportionDisplayMinutes]-säännöllä kuin projektilistassa, joten pinon
             * osat summautuvat tasan pylvään kokonaislukemaan.
             *
             * [projectOrder] pitää pinon järjestyksen samana kaikissa pylväissä;
             * tyhjänä osat järjestyvät id:n mukaan.
             */
            internal fun measuredChartBuckets(
                sessions: List<KnitSession>,
                params: InsightsQueryParams,
                axis: InsightsChartAxis,
                zone: ZoneId,
                firstDayOfWeek: DayOfWeek,
                projectOrder: List<Long> = emptyList(),
            ): Map<LocalDate, InsightsChartBucket> {
                val axisStartMillis =
                    axis.bucketStarts
                        .firstOrNull()
                        ?.atStartOfDay(zone)
                        ?.toInstant()
                        ?.toEpochMilli()
                val analysisStartMillis =
                    listOfNotNull(params.startMillis, axisStartMillis).maxOrNull()
                val buckets = SessionPaceAccumulator(analysisStartMillis, axis.interval, zone, firstDayOfWeek)
                sessions.forEach(buckets::add)
                return chartBucketsFromMetrics(buckets.values, projectOrder)
            }

            internal fun chartBucketsFromMetrics(
                bucketsByProject: Map<Long, Map<LocalDate, PaceBucketMetric>>,
                projectOrder: List<Long>,
            ): Map<LocalDate, InsightsChartBucket> {
                val rank = projectOrder.withIndex().associate { (index, id) -> id to index }
                val orderedProjectIds =
                    bucketsByProject.keys.sortedWith(
                        compareBy({ rank[it] ?: projectOrder.size }, { it }),
                    )

                return bucketsByProject.values
                    .flatMap { it.keys }
                    .toSet()
                    .associateWith { bucketStart ->
                        val contributions =
                            orderedProjectIds.mapNotNull { projectId ->
                                bucketsByProject[projectId]?.get(bucketStart)?.let { projectId to it }
                            }
                        val minutes = apportionDisplayMinutes(contributions.map { it.second.totalSeconds })
                        val segments =
                            contributions
                                .mapIndexed { index, (projectId, _) ->
                                    InsightsChartSegment(projectId = projectId, minutes = minutes[index])
                                }.filter { it.minutes > 0 }
                        InsightsChartBucket(
                            bucketStart = bucketStart,
                            totalMinutes = segments.sumOf { it.minutes },
                            totalRows = contributions.sumOf { it.second.totalRows },
                            segments = segments,
                        )
                    }
            }

            internal fun craftMix(
                timePerProject: List<ProjectTime>,
                projectList: List<CounterProject>,
            ): InsightsCraftMix {
                val activeIds = timePerProject.map { it.projectId }.toSet()
                val crafts =
                    projectList
                        .filter { activeIds.isEmpty() || activeIds.contains(it.id) }
                        .map { it.craftType }
                        .toSet()
                return when {
                    crafts.isEmpty() -> InsightsCraftMix.KNITTING
                    crafts == setOf(CraftType.CROCHET) -> InsightsCraftMix.CROCHET
                    crafts == setOf(CraftType.KNITTING) -> InsightsCraftMix.KNITTING
                    else -> InsightsCraftMix.MIXED
                }
            }

            /** Päivien määrä valitulla välillä. All Time lasketaan ensimmäisestä istunnosta tähän päivään. */
            internal fun daysInRange(
                timeRange: TimeRange,
                today: LocalDate,
                firstSessionDate: LocalDate?,
                firstDayOfWeek: DayOfWeek,
            ): Int {
                val start =
                    rangeStartDate(timeRange, today, firstDayOfWeek)
                        ?: firstSessionDate
                        ?: return 0
                return (ChronoUnit.DAYS.between(start, today) + 1).toInt().coerceAtLeast(0)
            }

            internal fun rangeStartDate(
                timeRange: TimeRange,
                today: LocalDate,
                firstDayOfWeek: DayOfWeek,
            ): LocalDate? =
                when (timeRange) {
                    TimeRange.ALL_TIME -> null
                    TimeRange.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                    TimeRange.THIS_MONTH -> today.withDayOfMonth(1)
                }

            internal fun firstSessionDate(
                sessions: List<KnitSession>,
                zone: ZoneId,
            ): LocalDate? =
                sessions
                    .minOfOrNull { session ->
                        Instant
                            .ofEpochMilli(session.startedAt)
                            .atZone(session.analyticsZoneOr(zone))
                            .toLocalDate()
                    }

            private fun activityDayKeys(
                sessions: List<KnitSession>,
                rangeStartMillis: Long?,
            ): Set<Long> {
                val zone = systemDefault()
                return activityDates(sessions, rangeStartMillis, zone).map { it.toEpochDay() }.toSet()
            }

            private fun activityDates(
                sessions: List<KnitSession>,
                rangeStartMillis: Long?,
                zone: ZoneId,
            ): Set<LocalDate> =
                SessionMetrics.activityDates(
                    sessions = sessions,
                    earliestDate = rangeEarliestDate(rangeStartMillis, zone),
                    zone = zone,
                )

            private fun rangeEarliestDate(
                rangeStartMillis: Long?,
                zone: ZoneId,
            ): LocalDate =
                rangeStartMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    ?: LocalDate.MIN
        }
    }

internal data class InsightsQueryParams(
    val projectId: Long? = null,
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val startMillis: Long? = null,
    val zone: ZoneId = systemDefault(),
    val currentDate: LocalDate = LocalDate.now(zone),
)

internal fun localDateChanges(
    nowMillis: () -> Long = System::currentTimeMillis,
    zoneProvider: () -> ZoneId = ZoneId::systemDefault,
): Flow<LocalDate> = insightsCalendarChanges(nowMillis, zoneProvider).map { it.date }.distinctUntilChanged()

private data class InsightsProFeatureGates(
    val canUseCharts: Boolean,
    val canUseStreak: Boolean,
)

private data class InsightsComputationInput(
    val sessions: RepositoryLoad<InsightsSessionAccumulator>,
    val projects: RepositoryLoad<List<CounterProject>>,
    val params: InsightsQueryParams,
    val featureGates: InsightsProFeatureGates,
    val completions: RepositoryLoad<List<ProjectCompletion>>,
)

internal fun LocalDate.bucketStart(
    interval: PaceGroupingInterval,
    firstDayOfWeek: DayOfWeek,
): LocalDate =
    when (interval) {
        PaceGroupingInterval.DAY -> this
        PaceGroupingInterval.WEEK -> with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        PaceGroupingInterval.MONTH -> withDayOfMonth(1)
    }

internal fun LocalDate.nextBucketStart(interval: PaceGroupingInterval): LocalDate =
    when (interval) {
        PaceGroupingInterval.DAY -> plusDays(1)
        PaceGroupingInterval.WEEK -> plusWeeks(1)
        PaceGroupingInterval.MONTH -> plusMonths(1)
    }

internal data class InsightsCalendar(
    val date: LocalDate,
    val zone: ZoneId,
)

private fun captureInsightsCalendar(): InsightsCalendar {
    val zone = systemDefault()
    return InsightsCalendar(LocalDate.now(zone), zone)
}

internal fun insightsCalendarChanges(
    nowMillis: () -> Long = System::currentTimeMillis,
    zoneProvider: () -> ZoneId = ZoneId::systemDefault,
): Flow<InsightsCalendar> =
    flow {
        while (true) {
            val zone = zoneProvider()
            val now = nowMillis()
            val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            emit(InsightsCalendar(date, zone))
            val nextDayStart =
                date
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            delay(minOf((nextDayStart - now).coerceAtLeast(1L), DATE_CHANGE_CHECK_INTERVAL_MILLIS))
        }
    }.distinctUntilChanged()

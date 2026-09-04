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
        private val currentDate: StateFlow<LocalDate> =
            localDateChanges()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    LocalDate.now(systemDefault()),
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

        private val queryParams: StateFlow<InsightsQueryParams> =
            combine(selectedProjectId, timeRange, currentDate) { projectId, activeTimeRange, date ->
                InsightsQueryParams(
                    projectId = projectId,
                    timeRange = activeTimeRange,
                    startMillis = rangeStartMillis(activeTimeRange, date),
                    currentDate = date,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsQueryParams())

        /**
         * Kaikki istunnot. Projekti ja aikaväli rajataan IO-poolissa, mutta koko
         * aineisto säilyttää eron aidosti tyhjän sovelluksen ja tyhjän suodattimen välillä.
         */
        private val sessionLoad: StateFlow<RepositoryLoad<List<KnitSession>>> =
            counterRepository
                .getSessionsForInsights(null, null)
                .distinctUntilChanged()
                .withLoadingState()
                .flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepositoryLoad.Loading)

        @OptIn(ExperimentalCoroutinesApi::class)
        internal val uiState: StateFlow<InsightsUiState> =
            combine(
                sessionLoad,
                projectLoad,
                queryParams,
                proFeatureGates,
            ) { loadedSessions, loadedProjects, params, featureGates ->
                InsightsComputationInput(loadedSessions, loadedProjects, params, featureGates)
            }.mapLatest { input ->
                val loadedSessions = input.sessions
                val loadedProjects = input.projects
                val params = input.params
                val featureGates = input.featureGates
                val sessionList = loadedSessions.value
                val projectList = loadedProjects.value
                if (sessionList == null || projectList == null) {
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
                        isPro = featureGates.canUseCharts,
                        canUseStreak = featureGates.canUseStreak,
                    )
                } else {
                    buildUiState(
                        sessions = sessionList,
                        projectList = projectList,
                        params = params,
                        featureGates = featureGates,
                    )
                }
            }.distinctUntilChanged()
                .flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

        private suspend fun buildUiState(
            sessions: List<KnitSession>,
            projectList: List<CounterProject>,
            params: InsightsQueryParams,
            featureGates: InsightsProFeatureGates,
        ): InsightsUiState {
            val zone = systemDefault()
            val today = params.currentDate
            val firstDayOfWeek = WeekFields.of(currentInsightsLocale()).firstDayOfWeek
            val scopedSessions =
                params.projectId?.let { projectId -> sessions.filter { it.projectId == projectId } } ?: sessions
            yield()
            val rangeMetrics = SessionMetrics.summarize(scopedSessions, params.startMillis, zone)
            val firstSessionDate = firstSessionDate(scopedSessions, zone)
            val axis = insightsChartAxis(params.timeRange, today, firstSessionDate, firstDayOfWeek)
            val timePerProject = buildTimePerProject(scopedSessions, projectList, params.startMillis, zone)
            yield()
            val projectFabric =
                buildProjectFabric(scopedSessions, params, featureGates, zone, firstDayOfWeek, timePerProject)
            val streakMetrics = buildStreakMetrics(scopedSessions, params.startMillis, featureGates.canUseStreak)
            yield()
            val measuredBuckets =
                measuredChartBuckets(
                    sessions = scopedSessions,
                    params = params,
                    axis = axis,
                    zone = zone,
                    firstDayOfWeek = firstDayOfWeek,
                    projectOrder = timePerProject.map { it.projectId },
                )
            val hasMeaningfulChartData =
                measuredBuckets.values.count { it.totalMinutes > 0 } >= MINIMUM_MEANINGFUL_CHART_BUCKETS
            val chartBuckets =
                if (featureGates.canUseCharts) fillChartBuckets(axis, measuredBuckets) else emptyList()
            val minutesPerRow =
                MinutesPerRowFormatter.fromSeconds(rangeMetrics.totalSeconds, rangeMetrics.totalRows)

            return InsightsUiState(
                isLoading = false,
                totalDuration = DurationDisplayFormatter.fromMinutes(rangeMetrics.totalMinutes),
                totalMinutes = rangeMetrics.totalMinutes,
                totalRows = rangeMetrics.totalRows,
                minutesPerRow = minutesPerRow,
                activeDays = activeDaysInRange(scopedSessions, params.startMillis, zone),
                daysInRange = daysInRange(params.timeRange, today, firstSessionDate, firstDayOfWeek),
                currentStreak = streakMetrics.current,
                bestStreak = streakMetrics.best,
                trend = buildTrend(scopedSessions, params, today, firstDayOfWeek, zone, rangeMetrics.totalMinutes),
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
                hasAnySessionData = sessions.isNotEmpty(),
                isPro = featureGates.canUseCharts,
                canUseStreak = featureGates.canUseStreak,
            )
        }

        private fun buildProjectFabric(
            sessions: List<KnitSession>,
            params: InsightsQueryParams,
            featureGates: InsightsProFeatureGates,
            zone: ZoneId,
            firstDayOfWeek: DayOfWeek,
            timePerProject: List<ProjectTime>,
        ): InsightsProjectFabricModel? =
            if (params.timeRange == TimeRange.ALL_TIME && featureGates.canUseCharts) {
                buildInsightsProjectFabric(
                    sessions = sessions,
                    today = params.currentDate,
                    zone = zone,
                    firstDayOfWeek = firstDayOfWeek,
                    projectOrder = timePerProject.map { it.projectId },
                )
            } else {
                null
            }

        private fun buildStreakMetrics(
            sessions: List<KnitSession>,
            rangeStartMillis: Long?,
            canUseStreak: Boolean,
        ): StreakMetrics =
            if (canUseStreak) {
                StreakMetrics(
                    current = calculateCurrentStreak(sessions, rangeStartMillis = rangeStartMillis),
                    best = calculateStreak(sessions, rangeStartMillis = rangeStartMillis),
                )
            } else {
                StreakMetrics(current = 0, best = 0)
            }

        /** Niiden päivien määrä, joilla välillä on istuntoja. */
        private fun activeDaysInRange(
            sessions: List<KnitSession>,
            rangeStartMillis: Long?,
            zone: ZoneId,
        ): Int = activityDates(sessions, rangeStartMillis, zone).size

        /**
         * Trendi vertaa välin minuutteja edelliseen samanmittaiseen jaksoon.
         * All Timella vertailukohtaa ei ole, jolloin trendiä ei näytetä.
         */
        private fun buildTrend(
            sessions: List<KnitSession>,
            params: InsightsQueryParams,
            today: LocalDate,
            firstDayOfWeek: DayOfWeek,
            zone: ZoneId,
            totalMinutes: Int,
        ): InsightsTrend? =
            resolvePreviousPeriodMinutes(
                sessions = sessions,
                timeRange = params.timeRange,
                today = today,
                firstDayOfWeek = firstDayOfWeek,
                zone = zone,
            )?.let { insightsTrend(totalMinutes, it) }

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
            ): Int {
                val zone = ZoneId.systemDefault()
                val activeDates = activityDates(sessions, rangeStartMillis, zone)
                if (activeDates.isEmpty()) return 0
                val today = LocalDate.now(zone)
                val anchor =
                    when {
                        activeDates.contains(today) -> today
                        activeDates.contains(today.minusDays(1)) -> today.minusDays(1)
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
                val bucketsByProject =
                    sessions
                        .groupBy { it.projectId }
                        .mapValues { (_, projectSessions) ->
                            SessionMetrics.paceBuckets(
                                sessions = projectSessions,
                                rangeStartMillis = params.startMillis,
                                interval = axis.interval,
                                zone = zone,
                                firstDayOfWeek = firstDayOfWeek,
                            )
                        }
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

            /**
             * Projektien kestot jaetaan hero-lukeman sisällä: minuutit pyöristetään
             * kerran koko välin sekunneista ja jaetaan projekteille, jolloin listan
             * summa vastaa hero-lukemaa eikä eroa muutamaa minuuttia.
             */
            private fun buildTimePerProject(
                sessions: List<KnitSession>,
                projectList: List<CounterProject>,
                rangeStartMillis: Long?,
                zone: ZoneId,
            ): List<ProjectTime> {
                val projectNames = projectList.associate { it.id to it.name }
                val measured =
                    sessions
                        .groupBy { it.projectId }
                        .mapNotNull { (projectId, projectSessions) ->
                            val summary =
                                SessionMetrics.summarize(
                                    sessions = projectSessions,
                                    rangeStartMillis = rangeStartMillis,
                                    zone = zone,
                                )
                            if (summary.sessionCount == 0) return@mapNotNull null
                            MeasuredProject(
                                projectId = projectId,
                                totalSeconds = summary.totalSeconds,
                                totalRows = summary.totalRows,
                                lastSessionAt = projectSessions.maxOf { it.startedAt },
                            )
                        }
                val displayMinutes = apportionDisplayMinutes(measured.map { it.totalSeconds })
                return measured
                    .mapIndexed { index, project ->
                        ProjectTime(
                            projectId = project.projectId,
                            projectName = projectNames[project.projectId],
                            totalMinutes = displayMinutes[index],
                            totalRows = project.totalRows,
                            lastSessionAt = project.lastSessionAt,
                        )
                    }.sortedWith(
                        compareByDescending<ProjectTime> { it.totalMinutes }
                            .thenByDescending { it.lastSessionAt },
                    )
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

            private fun rangeStartMillis(
                timeRange: TimeRange,
                today: LocalDate,
            ): Long? =
                rangeStartDate(timeRange, today, WeekFields.of(currentInsightsLocale()).firstDayOfWeek)
                    ?.atStartOfDay(systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli()

            /**
             * Ratkaisee edellisen jakson rajat aikavälistä ja delegoi summauksen
             * puhtaalle [previousPeriodMinutes]-funktiolle (InsightsChartModel.kt),
             * jossa katkaisun päivämäärälaskenta on yksikkötestattu kiinteillä
             * päivämäärillä. Sekunnit viedään perille pyöristämättöminä, jotta
             * vertailukohta mitataan samalla tavalla kuin nykyinen jakso.
             */
            private fun resolvePreviousPeriodMinutes(
                sessions: List<KnitSession>,
                timeRange: TimeRange,
                today: LocalDate,
                firstDayOfWeek: DayOfWeek,
                zone: ZoneId,
            ): Int? {
                if (timeRange == TimeRange.ALL_TIME) return null
                val currentStart = rangeStartDate(timeRange, today, firstDayOfWeek) ?: return null
                val previousStart =
                    when (timeRange) {
                        TimeRange.THIS_WEEK -> currentStart.minusWeeks(1)
                        TimeRange.THIS_MONTH -> currentStart.minusMonths(1)
                        TimeRange.ALL_TIME -> return null
                    }
                val dailySeconds = SessionMetrics.dailyActivitySeconds(sessions, previousStart, zone)
                return previousPeriodMinutes(dailySeconds, previousStart, currentStart, today)
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
    val currentDate: LocalDate = LocalDate.now(systemDefault()),
)

internal fun localDateChanges(
    nowMillis: () -> Long = System::currentTimeMillis,
    zoneProvider: () -> ZoneId = ZoneId::systemDefault,
): Flow<LocalDate> =
    flow {
        while (true) {
            val zone = zoneProvider()
            val now = nowMillis()
            val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            emit(date)
            val nextDayStart =
                date
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            delay(minOf((nextDayStart - now).coerceAtLeast(1L), DATE_CHANGE_CHECK_INTERVAL_MILLIS))
        }
    }.distinctUntilChanged()

private data class InsightsProFeatureGates(
    val canUseCharts: Boolean,
    val canUseStreak: Boolean,
)

private data class InsightsComputationInput(
    val sessions: RepositoryLoad<List<KnitSession>>,
    val projects: RepositoryLoad<List<CounterProject>>,
    val params: InsightsQueryParams,
    val featureGates: InsightsProFeatureGates,
)

private data class StreakMetrics(
    val current: Int,
    val best: Int,
)

/** Projektin mitatut arvot ennen minuuttien jakamista hero-lukeman sisään. */
private data class MeasuredProject(
    val projectId: Long,
    val totalSeconds: Long,
    val totalRows: Int,
    val lastSessionAt: Long,
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

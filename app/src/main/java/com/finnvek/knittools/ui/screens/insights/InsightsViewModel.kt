package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.di.IoDispatcher
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneId.systemDefault
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import javax.inject.Inject

private const val HEATMAP_LOOKBACK_DAYS = 55L

data class ProjectTime(
    val projectId: Long,
    val projectName: String,
    val totalMinutes: Int,
    val totalRows: Int,
    val lastSessionAt: Long,
)

data class PaceOverTimePoint(
    val bucketStart: LocalDate,
    val interval: PaceGroupingInterval,
    val rowsPerHour: Float,
    val totalMinutes: Int,
    val totalRows: Int,
)

enum class PaceGroupingInterval {
    DAY,
    MONTH,
}

enum class TimeRange {
    ALL_TIME,
    THIS_WEEK,
    THIS_MONTH,
}

data class InsightsUiState(
    val totalMinutes: Int = 0,
    val avgPace: Float = 0f,
    val completedCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val projects: List<CounterProject> = emptyList(),
    val selectedProjectId: Long? = null,
    val timePerProject: List<ProjectTime> = emptyList(),
    val paceOverTime: List<PaceOverTimePoint> = emptyList(),
    val dailyActivity: Map<LocalDate, Int> = emptyMap(),
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val hasSessionData: Boolean = false,
    val isPro: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        private val counterRepository: CounterRepository,
        private val proManager: ProManager,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        val isPro: StateFlow<Boolean> =
            proManager.proState
                .map { it.hasFeature(ProFeature.INSIGHTS_CHARTS) }
                .distinctUntilChanged()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    proManager.proState.value.hasFeature(ProFeature.INSIGHTS_CHARTS),
                )

        private val _selectedProjectId = MutableStateFlow<Long?>(null)
        private val _timeRange = MutableStateFlow(TimeRange.ALL_TIME)
        val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()
        val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

        val projects: StateFlow<List<CounterProject>> =
            counterRepository
                .getAllProjects()
                .distinctUntilChanged()
                .flowOn(ioDispatcher)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList(),
                )

        private val queryParams: StateFlow<InsightsQueryParams> =
            combine(selectedProjectId, timeRange) { projectId, activeTimeRange ->
                InsightsQueryParams(
                    projectId = projectId,
                    timeRange = activeTimeRange,
                    startMillis = rangeStartMillis(activeTimeRange),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsQueryParams())

        private val insightSessions: StateFlow<List<KnitSession>> =
            queryParams
                .flatMapLatest { params ->
                    counterRepository.getSessionsForInsights(params.projectId, params.startMillis)
                }.distinctUntilChanged()
                .flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val heatmapSessions: StateFlow<List<KnitSession>> =
            combine(selectedProjectId, isPro) { projectId, pro -> projectId to pro }
                .flatMapLatest { (projectId, pro) ->
                    if (pro) {
                        counterRepository.getSessionsForInsights(projectId, heatmapStartMillis())
                    } else {
                        flowOf(emptyList())
                    }
                }.distinctUntilChanged()
                .flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val uiState: StateFlow<InsightsUiState> =
            combine(
                insightSessions,
                heatmapSessions,
                projects,
                queryParams,
                isPro,
            ) { sessions, heatmap, projectList, params, pro ->
                buildUiState(
                    sessions = sessions,
                    heatmapSessions = heatmap,
                    projectList = projectList,
                    params = params,
                    isPro = pro,
                )
            }.distinctUntilChanged()
                .flowOn(ioDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

        val hasSessionData: StateFlow<Boolean> =
            uiState
                .map { it.hasSessionData }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val completedCount: StateFlow<Int> =
            uiState
                .map { it.completedCount }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val totalMinutes: StateFlow<Int> =
            uiState
                .map { it.totalMinutes }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val avgPace: StateFlow<Float> =
            uiState
                .map { it.avgPace }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        val paceOverTime: StateFlow<List<PaceOverTimePoint>> =
            uiState
                .map { it.paceOverTime }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val bestStreak: StateFlow<Int> =
            uiState
                .map { it.bestStreak }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val currentStreak: StateFlow<Int> =
            uiState
                .map { it.currentStreak }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val dailyActivity: StateFlow<Map<LocalDate, Int>> =
            uiState
                .map { it.dailyActivity }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        val timePerProject: StateFlow<List<ProjectTime>> =
            uiState
                .map { it.timePerProject }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private fun buildUiState(
            sessions: List<KnitSession>,
            heatmapSessions: List<KnitSession>,
            projectList: List<CounterProject>,
            params: InsightsQueryParams,
            isPro: Boolean,
        ): InsightsUiState {
            val zone = systemDefault()
            val metrics =
                SessionMetrics.summarize(
                    sessions = sessions,
                    rangeStartMillis = params.startMillis,
                    zone = zone,
                )
            val dailyActivity =
                if (isPro) {
                    SessionMetrics.dailyActivityMinutes(
                        sessions = heatmapSessions,
                        earliestDate = heatmapEarliestDate(zone),
                        zone = zone,
                    )
                } else {
                    emptyMap()
                }

            return InsightsUiState(
                totalMinutes = metrics.totalMinutes,
                avgPace = metrics.rowsPerHour,
                completedCount = completedProjectCount(projectList, params),
                currentStreak = calculateCurrentStreak(sessions, rangeStartMillis = params.startMillis),
                bestStreak = calculateStreak(sessions, rangeStartMillis = params.startMillis),
                projects = projectList,
                selectedProjectId = params.projectId,
                timePerProject =
                    if (isPro) {
                        buildTimePerProject(
                            sessions = sessions,
                            projectList = projectList,
                            rangeStartMillis = params.startMillis,
                            zone = zone,
                        )
                    } else {
                        emptyList()
                    },
                paceOverTime =
                    if (isPro) {
                        buildPaceOverTime(
                            sessions = sessions,
                            timeRange = params.timeRange,
                            rangeStartMillis = params.startMillis,
                            zone = zone,
                        )
                    } else {
                        emptyList()
                    },
                dailyActivity = dailyActivity,
                timeRange = params.timeRange,
                hasSessionData = metrics.sessionCount > 0,
                isPro = isPro,
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

            fun buildPaceOverTime(
                sessions: List<KnitSession>,
                timeRange: TimeRange,
                rangeStartMillis: Long?,
                zone: ZoneId,
            ): List<PaceOverTimePoint> {
                val interval = paceGroupingInterval(timeRange)
                val buckets =
                    SessionMetrics.paceBuckets(
                        sessions = sessions,
                        rangeStartMillis = rangeStartMillis,
                        interval = interval,
                        zone = zone,
                    )
                if (buckets.values.none { it.totalSeconds > 0L && it.totalRows > 0 }) return emptyList()

                val sortedBucketStarts =
                    if (timeRange == TimeRange.ALL_TIME) {
                        buckets.keys.sorted()
                    } else {
                        val startDate =
                            rangeStartMillis
                                ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                                ?: buckets.keys.minOrNull()
                                ?: return emptyList()
                        bucketStartsBetween(
                            startDate = startDate,
                            endDate = LocalDate.now(zone),
                            interval = interval,
                        )
                    }

                return sortedBucketStarts.map { bucketStart ->
                    val bucket = buckets[bucketStart] ?: PaceBucketMetric(0L, 0, 0f)
                    PaceOverTimePoint(
                        bucketStart = bucketStart,
                        interval = interval,
                        rowsPerHour = bucket.rowsPerHour,
                        totalMinutes = secondsToDisplayMinutes(bucket.totalSeconds),
                        totalRows = bucket.totalRows,
                    )
                }
            }

            private fun completedProjectCount(
                projectList: List<CounterProject>,
                params: InsightsQueryParams,
            ): Int =
                projectList
                    .asSequence()
                    .filter { params.projectId == null || it.id == params.projectId }
                    .count { project ->
                        project.isCompleted && isProjectWithinTimeRange(project, params.timeRange)
                    }

            private fun buildTimePerProject(
                sessions: List<KnitSession>,
                projectList: List<CounterProject>,
                rangeStartMillis: Long?,
                zone: ZoneId,
            ): List<ProjectTime> {
                val projectNames = projectList.associate { it.id to it.name }
                return sessions
                    .groupBy { it.projectId }
                    .mapNotNull { (projectId, projectSessions) ->
                        val summary =
                            SessionMetrics.summarize(
                                sessions = projectSessions,
                                rangeStartMillis = rangeStartMillis,
                                zone = zone,
                            )
                        if (summary.sessionCount == 0) return@mapNotNull null
                        ProjectTime(
                            projectId = projectId,
                            projectName = projectNames[projectId] ?: "Project $projectId",
                            totalMinutes = summary.totalMinutes,
                            totalRows = summary.totalRows,
                            lastSessionAt = projectSessions.maxOf { it.startedAt },
                        )
                    }.sortedWith(
                        compareByDescending<ProjectTime> { it.totalMinutes }
                            .thenByDescending { it.lastSessionAt },
                    )
            }

            private fun isProjectWithinTimeRange(
                project: CounterProject,
                timeRange: TimeRange,
            ): Boolean {
                if (timeRange == TimeRange.ALL_TIME) return true
                val completedAt = project.completedAt ?: return false
                val rangeStart = rangeStartDate(timeRange) ?: return true
                val completedDate =
                    Instant
                        .ofEpochMilli(completedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                return !completedDate.isBefore(rangeStart)
            }

            private fun rangeStartDate(timeRange: TimeRange): LocalDate? {
                val today = LocalDate.now()
                return when (timeRange) {
                    TimeRange.ALL_TIME -> {
                        null
                    }

                    TimeRange.THIS_WEEK -> {
                        val firstDayOfWeek = WeekFields.of(currentInsightsLocale()).firstDayOfWeek
                        today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                    }

                    TimeRange.THIS_MONTH -> {
                        today.withDayOfMonth(1)
                    }
                }
            }

            private fun rangeStartMillis(timeRange: TimeRange): Long? =
                rangeStartDate(timeRange)
                    ?.atStartOfDay(systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli()

            private fun heatmapStartMillis(): Long {
                val zone = systemDefault()
                return heatmapEarliestDate(zone)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            }

            private fun heatmapEarliestDate(zone: ZoneId): LocalDate =
                LocalDate.now(zone).minusDays(HEATMAP_LOOKBACK_DAYS)

            private fun paceGroupingInterval(timeRange: TimeRange): PaceGroupingInterval =
                when (timeRange) {
                    TimeRange.ALL_TIME -> PaceGroupingInterval.MONTH
                    TimeRange.THIS_WEEK,
                    TimeRange.THIS_MONTH,
                    -> PaceGroupingInterval.DAY
                }

            private fun bucketStartsBetween(
                startDate: LocalDate,
                endDate: LocalDate,
                interval: PaceGroupingInterval,
            ): List<LocalDate> {
                val starts = mutableListOf<LocalDate>()
                var cursor = startDate.bucketStart(interval)
                val last = endDate.bucketStart(interval)
                while (!cursor.isAfter(last)) {
                    starts += cursor
                    cursor = cursor.nextBucketStart(interval)
                }
                return starts
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
                    earliestDate = rangeStartDate(rangeStartMillis, zone),
                    zone = zone,
                )

            private fun rangeStartDate(
                rangeStartMillis: Long?,
                zone: ZoneId,
            ): LocalDate =
                rangeStartMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    ?: LocalDate.MIN
        }
    }

private data class InsightsQueryParams(
    val projectId: Long? = null,
    val timeRange: TimeRange = TimeRange.ALL_TIME,
    val startMillis: Long? = null,
)

internal fun LocalDate.bucketStart(interval: PaceGroupingInterval): LocalDate =
    when (interval) {
        PaceGroupingInterval.DAY -> this
        PaceGroupingInterval.MONTH -> withDayOfMonth(1)
    }

internal fun LocalDate.nextBucketStart(interval: PaceGroupingInterval): LocalDate =
    when (interval) {
        PaceGroupingInterval.DAY -> plusDays(1)
        PaceGroupingInterval.MONTH -> plusMonths(1)
    }

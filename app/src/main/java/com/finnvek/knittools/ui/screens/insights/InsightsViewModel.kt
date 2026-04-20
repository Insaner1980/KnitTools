package com.finnvek.knittools.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.SessionEntity
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class ProjectTime(
    val projectId: Long,
    val projectName: String,
    val totalMinutes: Int,
    val totalRows: Int,
    val lastSessionAt: Long,
)

enum class TimeRange {
    ALL_TIME,
    THIS_WEEK,
    THIS_MONTH,
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        private val counterRepository: CounterRepository,
        private val proManager: ProManager,
    ) : ViewModel() {
        val isPro: Boolean get() = proManager.isPro()
        private val _selectedProjectId = MutableStateFlow<Long?>(null)
        private val _timeRange = MutableStateFlow(TimeRange.ALL_TIME)
        val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()
        val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

        val projects: StateFlow<List<CounterProjectEntity>> =
            counterRepository.getAllProjects().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

        // Kaikki sessiot valitulle projektille → avg pace + streak + kaaviodata
        private val allSessions: StateFlow<List<SessionEntity>> =
            _selectedProjectId
                .flatMapLatest { projectId ->
                    counterRepository.getAllSessions(projectId)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val filteredSessions: StateFlow<List<SessionEntity>> =
            combine(allSessions, timeRange) { sessions, activeTimeRange ->
                filterSessionsByTimeRange(sessions, activeTimeRange)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val hasSessionData: StateFlow<Boolean> =
            filteredSessions
                .map { it.isNotEmpty() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val completedCount: StateFlow<Int> =
            combine(projects, selectedProjectId, timeRange) { projectList, projectId, activeTimeRange ->
                projectList
                    .asSequence()
                    .filter { projectId == null || it.id == projectId }
                    .count { project ->
                        project.isCompleted && isProjectWithinTimeRange(project, activeTimeRange)
                    }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val totalMinutes: StateFlow<Int> =
            filteredSessions
                .map { sessions -> sessions.sumOf { it.durationMinutes } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val avgPace: StateFlow<Float> =
            combine(filteredSessions, totalMinutes) { sessions, minutes ->
                if (minutes <= 0) return@combine 0f
                val totalRows = sessions.sumOf { (it.endRow - it.startRow).coerceAtLeast(0) }
                val hours = minutes / 60f
                if (hours > 0f) totalRows / hours else 0f
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

        val bestStreak: StateFlow<Int> =
            filteredSessions
                .map { sessions ->
                    calculateStreak(sessions)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val currentStreak: StateFlow<Int> =
            filteredSessions
                .map { sessions ->
                    calculateCurrentStreak(sessions)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        val dailyActivity: StateFlow<Map<LocalDate, Int>> =
            filteredSessions
                .map { sessions ->
                    val cutoff = LocalDate.now().minusDays(55)
                    val zone = ZoneId.systemDefault()
                    sessions
                        .map { session ->
                            val date = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
                            date to session.durationMinutes
                        }.filter { (date, _) -> !date.isBefore(cutoff) }
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, minutes) -> minutes.sum() }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        val timePerProject: StateFlow<List<ProjectTime>> =
            combine(filteredSessions, projects) { sessions, projectList ->
                val projectMap = projectList.associateBy { it.id }
                sessions
                    .groupBy { it.projectId }
                    .map { (projectId, projectSessions) ->
                        ProjectTime(
                            projectId = projectId,
                            projectName = projectMap[projectId]?.name ?: "Project $projectId",
                            totalMinutes = projectSessions.sumOf { it.durationMinutes },
                            totalRows = projectSessions.sumOf { (it.endRow - it.startRow).coerceAtLeast(0) },
                            lastSessionAt = projectSessions.maxOf { it.startedAt },
                        )
                    }.sortedByDescending { it.totalMinutes }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            fun calculateStreak(sessions: List<SessionEntity>): Int {
                if (sessions.isEmpty()) return 0
                val days = sessions.map { sessionToDayKey(it.startedAt) }.distinct().sorted()
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

            fun calculateCurrentStreak(sessions: List<SessionEntity>): Int {
                if (sessions.isEmpty()) return 0
                val zone = ZoneId.systemDefault()
                val activeDates =
                    sessions
                        .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
                        .toSet()
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

            private fun filterSessionsByTimeRange(
                sessions: List<SessionEntity>,
                timeRange: TimeRange,
            ): List<SessionEntity> {
                val rangeStart = rangeStartDate(timeRange) ?: return sessions
                val zone = ZoneId.systemDefault()
                return sessions.filter { session ->
                    val sessionDate = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
                    !sessionDate.isBefore(rangeStart)
                }
            }

            private fun isProjectWithinTimeRange(
                project: CounterProjectEntity,
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
                        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
                        today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                    }

                    TimeRange.THIS_MONTH -> {
                        today.withDayOfMonth(1)
                    }
                }
            }

            private fun sessionToDayKey(timestamp: Long): Long =
                Instant
                    .ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay()
        }
    }

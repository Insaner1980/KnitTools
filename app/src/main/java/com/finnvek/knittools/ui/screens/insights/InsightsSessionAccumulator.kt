package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.repository.SessionInsightsFacts
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

internal fun insightsPreviousStart(params: InsightsQueryParams): LocalDate? {
    val start =
        InsightsViewModel.rangeStartDate(
            params.timeRange,
            params.currentDate,
            params.firstDayOfWeek,
        ) ?: return null
    return when (params.timeRange) {
        TimeRange.THIS_WEEK -> start.minusWeeks(1)
        TimeRange.THIS_MONTH -> start.minusMonths(1)
        TimeRange.ALL_TIME -> null
    }
}

// Päivälaskenta käyttää tallennettua vyöhykettä, myös kelvollista +18:00-siirtymää.
internal fun insightsQueryStart(params: InsightsQueryParams): Long? =
    insightsPreviousStart(params)?.atStartOfDay(ZoneOffset.MAX)?.toInstant()?.toEpochMilli()

/** Säilyttää vain koonnit, ei istuntorivejä eikä erälistaa. */
internal class InsightsSessionAccumulator(
    private val params: InsightsQueryParams,
    val facts: SessionInsightsFacts,
) {
    private val firstDay = params.firstDayOfWeek
    private val rangeDate = params.startMillis?.let { Instant.ofEpochMilli(it).atZone(params.zone).toLocalDate() }
    private val previousStart = insightsPreviousStart(params)
    private val previousSeconds = mutableMapOf<LocalDate, Long>()
    private val projects = mutableMapOf<Long, SessionMetricSummary>()
    val activeDates = InsightsActivityDates()
    val firstDate: LocalDate? = facts.firstSessionDate
    var summary = SessionMetricSummary(0, 0, 0)
        private set

    private fun millis(date: LocalDate) = date.atStartOfDay(params.zone).toInstant().toEpochMilli()

    private val axis =
        insightsChartAxis(params.timeRange, params.currentDate, firstDate?.coerceAtMost(params.currentDate), firstDay)
    private val chart =
        SessionPaceAccumulator(
            listOfNotNull(params.startMillis, axis.bucketStarts.firstOrNull()?.let(::millis)).maxOrNull(),
            axis.interval,
            params.zone,
            firstDay,
            axis.bucketStarts.lastOrNull(),
        )
    val fabric =
        SessionPaceAccumulator(
            millis(projectFabricWindowStart(params.currentDate, firstDay)),
            PaceGroupingInterval.DAY,
            params.zone,
            firstDay,
            params.currentDate,
        )

    fun add(session: KnitSession) {
        val one = listOf(session)
        val contribution = SessionMetrics.summarize(one, params.startMillis, params.zone)
        summary += contribution
        if (contribution.sessionCount > 0) {
            projects[session.projectId] = (projects[session.projectId] ?: SessionMetricSummary(0, 0, 0)) + contribution
        }
        val daily = SessionMetrics.dailyActivitySeconds(one, previousStart ?: LocalDate.MIN, params.zone)
        daily.forEach { (date, seconds) ->
            // Count dates in the displayed calendar range, even when a past instant has a later stored-zone date.
            if ((rangeDate == null || !date.isBefore(rangeDate)) &&
                !date.isAfter(params.currentDate)
            ) {
                activeDates.add(date)
            }
            if (previousStart != null && date.isBefore(requireNotNull(rangeDate))) {
                previousSeconds[date] = addSessionSeconds(previousSeconds[date] ?: 0L, seconds)
            }
        }
        chart.add(session)
        if (params.timeRange == TimeRange.ALL_TIME) fabric.add(session)
    }

    fun chart(interval: PaceGroupingInterval): SessionPaceAccumulator {
        require(interval == axis.interval)
        return chart
    }

    fun trend(): InsightsTrend? =
        previousStart?.let {
            insightsTrend(
                summary.totalSeconds,
                previousPeriodSeconds(previousSeconds, it, requireNotNull(rangeDate), params.currentDate),
            )
        }

    fun timePerProject(projectList: List<CounterProject>): List<ProjectTime> {
        val names = projectList.associate { it.id to it.name }
        val measured = facts.projects.mapNotNull { project -> projects[project.projectId]?.let { project to it } }
        val minutes = apportionDisplayMinutes(measured.map { it.second.totalSeconds })
        return measured
            .mapIndexed { index, (project, metric) ->
                ProjectTime(
                    project.projectId,
                    names[project.projectId],
                    minutes[index],
                    metric.totalRows,
                    project.lastSessionAt,
                )
            }.sortedWith(compareByDescending<ProjectTime> { it.totalMinutes }.thenByDescending { it.lastSessionAt })
    }
}

private operator fun SessionMetricSummary.plus(other: SessionMetricSummary) =
    SessionMetricSummary(
        addSessionSeconds(totalSeconds, other.totalSeconds),
        totalRows + other.totalRows,
        sessionCount + other.sessionCount,
    )

internal class SessionPaceAccumulator(
    private val start: Long?,
    private val interval: PaceGroupingInterval,
    private val zone: ZoneId,
    private val firstDay: DayOfWeek,
    private val lastBucket: LocalDate? = null,
) {
    val values = mutableMapOf<Long, MutableMap<LocalDate, PaceBucketMetric>>()

    fun add(session: KnitSession) {
        val contributions = SessionMetrics.paceBuckets(listOf(session), start, interval, zone, firstDay)
        if (contributions.isEmpty()) return
        contributions.forEach { (date, metric) ->
            if (lastBucket != null && date.isAfter(lastBucket)) return@forEach
            val project = values.getOrPut(session.projectId) { mutableMapOf() }
            val old = project[date]
            project[date] =
                PaceBucketMetric(
                    addSessionSeconds(old?.totalSeconds ?: 0L, metric.totalSeconds),
                    (old?.totalRows ?: 0L) + metric.totalRows,
                    0f,
                )
        }
    }
}

internal fun currentStreak(
    dates: Set<LocalDate>,
    today: LocalDate,
): Int {
    var date = if (today in dates) today else today.minusDays(1)
    var streak = 0
    while (date in dates) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}

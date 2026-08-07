package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class InsightsFabricDay(
    val date: LocalDate,
    val totalMinutes: Int,
    val totalRows: Int,
    val dominantProjectId: Long?,
    val level: Int,
)

data class InsightsFabricModel(
    val days: List<InsightsFabricDay>,
    val firstDayOfWeek: DayOfWeek,
    val weekCount: Int,
)

internal fun buildInsightsFabricModel(
    sessions: List<KnitSession>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    zone: ZoneId,
): InsightsFabricModel {
    val metricsByProject =
        sessions
            .groupBy { it.projectId }
            .mapValues { (_, projectSessions) ->
                SessionMetrics.dailyPaceBuckets(
                    sessions = projectSessions,
                    rangeStartMillis = null,
                    zone = zone,
                )
            }
    val contributionsByDate = mutableMapOf<LocalDate, MutableList<Pair<Long, PaceBucketMetric>>>()
    metricsByProject.forEach { (projectId, metrics) ->
        metrics.forEach { (date, metric) ->
            contributionsByDate.getOrPut(date) { mutableListOf() }.add(projectId to metric)
        }
    }
    val displayMinutesByDate =
        contributionsByDate.mapValues { (_, contributions) ->
            apportionDisplayMinutes(contributions.map { it.second.totalSeconds }).sum()
        }
    val firstSessionDate = contributionsByDate.keys.filterNot { it.isAfter(today) }.minOrNull()
    val requestedFirstDate = (firstSessionDate ?: today).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val earliestAllowedDate = currentWeekStart.minusWeeks(FABRIC_MAX_WEEKS - 1L)
    val firstDate = maxOf(requestedFirstDate, earliestAllowedDate)
    val maxDailyMinutes =
        displayMinutesByDate
            .filterKeys { date -> date in firstDate..today }
            .values
            .maxOrNull() ?: 0
    val dayCount = ChronoUnit.DAYS.between(firstDate, today).toInt() + 1
    val days =
        (0 until dayCount).map { offset ->
            val date = firstDate.plusDays(offset.toLong())
            val contributions = contributionsByDate[date].orEmpty()
            val totalMinutes = displayMinutesByDate[date] ?: 0
            InsightsFabricDay(
                date = date,
                totalMinutes = totalMinutes,
                totalRows = contributions.sumOf { it.second.totalRows },
                dominantProjectId =
                    contributions
                        .minWithOrNull(
                            compareByDescending<Pair<Long, PaceBucketMetric>> { it.second.totalSeconds }
                                .thenBy { it.first },
                        )?.first
                        ?.takeIf { totalMinutes > 0 },
                level =
                    when {
                        totalMinutes == 0 -> 0
                        totalMinutes.toLong() * 3L <= maxDailyMinutes.toLong() -> 1
                        totalMinutes.toLong() * 3L <= maxDailyMinutes.toLong() * 2L -> 2
                        else -> 3
                    },
            )
        }
    val weekCount = ChronoUnit.WEEKS.between(firstDate, currentWeekStart).toInt() + 1
    return InsightsFabricModel(days = days, firstDayOfWeek = firstDayOfWeek, weekCount = weekCount)
}

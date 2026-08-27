package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal const val PROJECT_FABRIC_WEEK_COUNT = 26

internal data class InsightsProjectFabricDay(
    val date: LocalDate,
    val projectIds: List<Long>,
)

internal data class InsightsProjectFabricModel(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val firstDayOfWeek: DayOfWeek,
    val days: List<InsightsProjectFabricDay>,
) {
    val activeDayCount: Int
        get() = days.count { it.projectIds.isNotEmpty() }
}

internal fun buildInsightsProjectFabric(
    sessions: List<KnitSession>,
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
    projectOrder: List<Long>,
): InsightsProjectFabricModel? {
    val startDate =
        today
            .with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            .minusWeeks((PROJECT_FABRIC_WEEK_COUNT - 1).toLong())
    val bucketsByProject =
        sessions
            .groupBy { it.projectId }
            .mapValues { (_, projectSessions) ->
                SessionMetrics.paceBuckets(
                    sessions = projectSessions,
                    rangeStartMillis = null,
                    interval = PaceGroupingInterval.DAY,
                    zone = zone,
                    firstDayOfWeek = firstDayOfWeek,
                )
            }
    val projectRank = projectOrder.withIndex().associate { (index, projectId) -> projectId to index }
    val orderedProjectIds =
        bucketsByProject.keys.sortedWith(
            compareBy({ projectRank[it] ?: projectOrder.size }, { it }),
        )
    val days =
        generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .map { date ->
                InsightsProjectFabricDay(
                    date = date,
                    projectIds =
                        orderedProjectIds.filter { projectId ->
                            (bucketsByProject[projectId]?.get(date)?.totalSeconds ?: 0L) > 0L
                        },
                )
            }.toList()

    if (days.none { it.projectIds.isNotEmpty() }) return null
    return InsightsProjectFabricModel(
        startDate = startDate,
        endDate = today,
        firstDayOfWeek = firstDayOfWeek,
        days = days,
    )
}

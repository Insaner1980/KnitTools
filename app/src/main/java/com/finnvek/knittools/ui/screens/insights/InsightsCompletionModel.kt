package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.ProjectCompletion
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class InsightsCompletionEvent(
    val id: Long,
    val projectId: Long,
    val date: LocalDate,
)

internal data class InsightsCompletionBucket(
    val start: LocalDate,
    val end: LocalDate,
    val count: Int,
)

internal data class InsightsCompletions(
    val events: List<InsightsCompletionEvent> = emptyList(),
    val buckets: List<InsightsCompletionBucket> = emptyList(),
)

internal fun buildInsightsCompletions(
    events: List<ProjectCompletion>,
    params: InsightsQueryParams,
    firstDayOfWeek: DayOfWeek,
): InsightsCompletions {
    val start = InsightsViewModel.rangeStartDate(params.timeRange, params.currentDate, firstDayOfWeek)
    val end =
        when (params.timeRange) {
            TimeRange.ALL_TIME -> LocalDate.MAX
            TimeRange.THIS_WEEK -> requireNotNull(start).plusDays(6)
            TimeRange.THIS_MONTH -> params.currentDate.withDayOfMonth(params.currentDate.lengthOfMonth())
        }
    val dated =
        events
            .asSequence()
            .filter { params.projectId == null || it.projectId == params.projectId }
            .map { event ->
                val zone =
                    try {
                        event.zoneId?.let(ZoneId::of) ?: params.zone
                    } catch (_: DateTimeException) {
                        params.zone
                    }
                InsightsCompletionEvent(
                    event.id,
                    event.projectId,
                    Instant.ofEpochMilli(event.completedAt).atZone(zone).toLocalDate(),
                )
            }.filter { (start == null || !it.date.isBefore(start)) && !it.date.isAfter(end) }
            .sortedWith(compareByDescending<InsightsCompletionEvent> { it.date }.thenByDescending { it.id })
            .toList()
    val axisEnd = maxOf(params.currentDate, dated.maxOfOrNull { it.date } ?: params.currentDate)
    val axis = insightsChartAxis(params.timeRange, axisEnd, dated.minOfOrNull { it.date }, firstDayOfWeek)
    val counts = dated.groupingBy { it.date.bucketStart(axis.interval, firstDayOfWeek) }.eachCount()
    return InsightsCompletions(
        events = dated,
        buckets =
            axis.bucketStarts.map {
                InsightsCompletionBucket(
                    it,
                    minOf(it.nextBucketStart(axis.interval).minusDays(1), axisEnd),
                    counts[it] ?: 0,
                )
            },
    )
}

package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.model.KnitSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal data class SessionMetricSummary(
    val totalSeconds: Long,
    val totalRows: Int,
    val sessionCount: Int,
) {
    val totalMinutes: Int
        get() = secondsToDisplayMinutes(totalSeconds)

    val rowsPerHour: Float
        get() =
            if (totalSeconds <= 0L) {
                0f
            } else {
                totalRows / (totalSeconds / 3600f)
            }
}

internal data class PaceBucketMetric(
    val totalSeconds: Long,
    val totalRows: Int,
    val rowsPerHour: Float,
)

internal object SessionMetrics {
    fun summarize(
        sessions: List<KnitSession>,
        rangeStartMillis: Long?,
        zone: ZoneId,
    ): SessionMetricSummary {
        var totalSeconds = 0L
        var totalRows = 0
        var sessionCount = 0

        sessions.forEach { session ->
            val contribution = session.contributionFrom(rangeStartMillis, zone) ?: return@forEach
            totalSeconds += contribution.seconds
            totalRows += contribution.rows
            sessionCount++
        }

        return SessionMetricSummary(
            totalSeconds = totalSeconds,
            totalRows = totalRows,
            sessionCount = sessionCount,
        )
    }

    fun dailyActivityMinutes(
        sessions: List<KnitSession>,
        earliestDate: LocalDate,
        zone: ZoneId,
    ): Map<LocalDate, Int> =
        dailyActivitySeconds(
            sessions = sessions,
            earliestDate = earliestDate,
            zone = zone,
        ).mapValues { (_, seconds) -> secondsToDisplayMinutes(seconds) }

    fun activityDates(
        sessions: List<KnitSession>,
        earliestDate: LocalDate,
        zone: ZoneId,
    ): Set<LocalDate> =
        dailyActivitySeconds(
            sessions = sessions,
            earliestDate = earliestDate,
            zone = zone,
        ).keys

    fun paceBuckets(
        sessions: List<KnitSession>,
        rangeStartMillis: Long?,
        interval: PaceGroupingInterval,
        zone: ZoneId,
    ): Map<LocalDate, PaceBucketMetric> {
        val buckets = mutableMapOf<LocalDate, MutablePaceBucket>()
        sessions.forEach { session ->
            session
                .paceBucketContributions(rangeStartMillis, interval, zone)
                .forEach { (bucketStart, contribution) ->
                    val bucket = buckets.getOrPut(bucketStart) { MutablePaceBucket() }
                    bucket.seconds += contribution.seconds
                    bucket.rows += contribution.rows
                    bucket.totalSeconds += contribution.totalSeconds
                    bucket.totalRows += contribution.totalRows
                }
        }
        return buckets.mapValues { (_, bucket) ->
            val rowsPerHour =
                if (bucket.totalSeconds <= 0L || bucket.totalRows <= 0) {
                    0f
                } else {
                    (bucket.rows / (bucket.seconds / 3600.0)).toFloat().takeIf { it.isFinite() } ?: 0f
                }
            PaceBucketMetric(
                totalSeconds = bucket.totalSeconds,
                totalRows = bucket.totalRows,
                rowsPerHour = rowsPerHour,
            )
        }
    }

    private fun dailyActivitySeconds(
        sessions: List<KnitSession>,
        earliestDate: LocalDate,
        zone: ZoneId,
    ): Map<LocalDate, Long> {
        val secondsByDate = mutableMapOf<LocalDate, Long>()
        sessions.forEach { session ->
            session
                .dailySecondContributions(earliestDate, zone)
                .forEach { (date, seconds) ->
                    secondsByDate[date] = (secondsByDate[date] ?: 0L) + seconds
                }
        }
        return secondsByDate
    }

    private fun KnitSession.contributionFrom(
        rangeStartMillis: Long?,
        zone: ZoneId,
    ): SessionContribution? {
        val activeSeconds = activeDurationSeconds()
        if (activeSeconds <= 0L) return null

        val started = startedAt
        val ended = effectiveEndedAt()
        val rangeStart =
            rangeStartMillis?.let { start ->
                Instant
                    .ofEpochMilli(start)
                    .atZone(zone)
                    .toLocalDate()
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            }
        val overlapStart = maxOf(started, rangeStart ?: started)
        val overlapEnd = maxOf(overlapStart, ended)
        if (overlapEnd <= overlapStart) return null

        val fraction = (overlapEnd - overlapStart).toDouble() / (ended - started).coerceAtLeast(1L)
        val seconds = scaledSeconds(activeSeconds, fraction)
        if (seconds <= 0L) return null
        return SessionContribution(
            seconds = seconds,
            rows = scaledRows(workedRows(), fraction),
        )
    }

    private fun KnitSession.dailySecondContributions(
        earliestDate: LocalDate,
        zone: ZoneId,
    ): Map<LocalDate, Long> {
        val activeSeconds = activeDurationSeconds()
        if (activeSeconds <= 0L) return emptyMap()

        val sessionZone = analyticsZoneOr(zone)
        val started = startedAt
        val ended = effectiveEndedAt()
        var cursor = started
        var allocatedSeconds = 0L
        val contributions = mutableMapOf<LocalDate, Long>()

        while (cursor < ended) {
            val date = Instant.ofEpochMilli(cursor).atZone(sessionZone).toLocalDate()
            val nextDayStart =
                date
                    .plusDays(1)
                    .atStartOfDay(sessionZone)
                    .toInstant()
                    .toEpochMilli()
            val segmentEnd = minOf(ended, nextDayStart)
            val cumulativeFraction = (segmentEnd - started).toDouble() / (ended - started).coerceAtLeast(1L)
            val cumulativeSeconds = scaledSeconds(activeSeconds, cumulativeFraction)
            val seconds = (cumulativeSeconds - allocatedSeconds).coerceAtLeast(0L)
            allocatedSeconds = cumulativeSeconds
            if (!date.isBefore(earliestDate) && seconds > 0L) {
                contributions[date] = (contributions[date] ?: 0L) + seconds
            }
            cursor = segmentEnd
        }

        return contributions
    }

    private fun KnitSession.paceBucketContributions(
        rangeStartMillis: Long?,
        interval: PaceGroupingInterval,
        zone: ZoneId,
    ): Map<LocalDate, PaceBucketContribution> {
        val activeSeconds = activeDurationSeconds()
        val rows = workedRows()
        if (activeSeconds <= 0L) return emptyMap()

        val sessionZone = analyticsZoneOr(zone)
        val started = startedAt
        val ended = effectiveEndedAt()
        var cursor = maxOf(started, rangeStartMillis ?: started)
        val sessionMillis = (ended - started).coerceAtLeast(1L)
        val initialFraction = (cursor - started).toDouble() / sessionMillis
        var allocatedSeconds = scaledSeconds(activeSeconds, initialFraction)
        var allocatedRows = scaledRows(rows, initialFraction)
        val contributions = mutableMapOf<LocalDate, PaceBucketContribution>()

        while (cursor < ended) {
            val bucketStart =
                Instant
                    .ofEpochMilli(cursor)
                    .atZone(sessionZone)
                    .toLocalDate()
                    .bucketStart(interval)
            val nextBucketStartMillis =
                bucketStart
                    .nextBucketStart(interval)
                    .atStartOfDay(sessionZone)
                    .toInstant()
                    .toEpochMilli()
            val segmentEnd = minOf(ended, nextBucketStartMillis)
            if (segmentEnd <= cursor) break

            val fraction = (segmentEnd - cursor).toDouble() / sessionMillis
            val seconds = activeSeconds * fraction
            val bucketRows = rows * fraction
            val cumulativeFraction = (segmentEnd - started).toDouble() / sessionMillis
            val cumulativeSeconds = scaledSeconds(activeSeconds, cumulativeFraction)
            val cumulativeRows = scaledRows(rows, cumulativeFraction)
            val segmentSeconds = (cumulativeSeconds - allocatedSeconds).coerceAtLeast(0L)
            val segmentRows = (cumulativeRows - allocatedRows).coerceAtLeast(0)
            allocatedSeconds = cumulativeSeconds
            allocatedRows = cumulativeRows
            if (seconds > 0.0 && (segmentSeconds > 0L || segmentRows > 0)) {
                val contribution = contributions.getOrPut(bucketStart) { PaceBucketContribution() }
                contribution.seconds += seconds
                contribution.rows += bucketRows
                contribution.totalSeconds += segmentSeconds
                contribution.totalRows += segmentRows
            }
            cursor = segmentEnd
        }

        return contributions
    }
}

private data class SessionContribution(
    val seconds: Long,
    val rows: Int,
)

private data class PaceBucketContribution(
    var seconds: Double = 0.0,
    var rows: Double = 0.0,
    var totalSeconds: Long = 0L,
    var totalRows: Int = 0,
)

private data class MutablePaceBucket(
    var seconds: Double = 0.0,
    var rows: Double = 0.0,
    var totalSeconds: Long = 0L,
    var totalRows: Int = 0,
)

private fun KnitSession.activeDurationSeconds(): Long =
    when {
        durationSeconds > 0L -> durationSeconds
        durationMinutes > 0 -> durationMinutes.toLong() * 60L
        workedRows() > 0 -> 1L
        else -> 0L
    }

private fun KnitSession.effectiveEndedAt(): Long =
    endedAt.coerceAtLeast(startedAt + activeDurationSeconds().coerceAtLeast(1L) * 1_000L)

private fun KnitSession.workedRows(): Int =
    when {
        rowsWorked > 0 -> rowsWorked
        endRow > startRow -> endRow - startRow
        else -> 0
    }

private fun KnitSession.analyticsZoneOr(fallback: ZoneId): ZoneId =
    zoneId
        ?.let { persistedZoneId -> runCatching { ZoneId.of(persistedZoneId) }.getOrNull() }
        ?: fallback

private fun scaledSeconds(
    activeSeconds: Long,
    fraction: Double,
): Long {
    if (activeSeconds <= 0L || fraction <= 0.0) return 0L
    return (activeSeconds * fraction).roundToLong().coerceAtLeast(0L)
}

private fun scaledRows(
    rows: Int,
    fraction: Double,
): Int {
    if (rows <= 0 || fraction <= 0.0) return 0
    return (rows * fraction).roundToInt().coerceAtLeast(0)
}

internal fun secondsToDisplayMinutes(seconds: Long): Int =
    when {
        seconds <= 0L -> 0
        else -> ((seconds + 59L) / 60L).toInt()
    }

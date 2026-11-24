package com.finnvek.knittools.ui.screens.insights

import com.finnvek.knittools.domain.calculator.evaluateActiveSessionTime
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveSessionTimeEvaluation
import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPayloadCodec
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AuditPart13NumericBoundaryTest {
    @Test fun threeHundredSixtyFiveToSevenLocalDaysPreserveDstAndBoundedContributions() {
        for (zoneName in listOf("UTC", "Europe/Helsinki", "America/New_York")) {
            val zone = ZoneId.of(zoneName)
            for (days in 365L..367L) {
                val start = LocalDate.of(2024, 1, 1)
                val end = start.plusDays(days)
                val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = end.atStartOfDay(zone).toInstant().toEpochMilli()
                val seconds = (endMillis - startMillis) / 1_000L
                val session =
                    KnitSession(
                        projectId = 1,
                        startedAt = startMillis,
                        endedAt = endMillis,
                        durationMinutes = (seconds / 60).toInt(),
                        durationSeconds = seconds,
                        startRow = 0,
                        endRow = 1,
                        rowsWorked = 1,
                        zoneId = zoneName,
                    )
                val daily = SessionMetrics.dailyActivitySeconds(listOf(session), start, ZoneId.of("Asia/Tokyo"))
                assertEquals(minOf(days, 366).toInt(), daily.size)
                val retainedStart = maxOf(start, end.minusDays(366))
                daily.forEach { (date, duration) ->
                    val expected = (
                        date.plusDays(1).atStartOfDay(zone).toEpochSecond() -
                            date.atStartOfDay(zone).toEpochSecond()
                    )
                    assertEquals("$zoneName/$days/$date", expected, duration)
                }
                assertEquals(
                    end.atStartOfDay(zone).toEpochSecond() - retainedStart.atStartOfDay(zone).toEpochSecond(),
                    daily.values.sum(),
                )
                assertEquals(seconds, SessionMetrics.summarize(listOf(session), null, zone).totalSeconds)
            }
        }
    }

    @Test fun missingBootIdentityUsesUptimeWhileChangedAndInvalidBootClocksRequireReview() {
        val anchors = ActiveSessionTimingAnchors(10_000, 10_000, 4, 120, 0)
        val now = SessionTimeSnapshot(70_000, 16_000, 4, "Europe/Helsinki")
        val beyondUptime = 10_000L + 6_000L + 5L * 60L * 1_000L + 1L
        for ((stored, current) in listOf(4L to null, null to 4L, null to null)) {
            assertEquals(
                ActiveSessionTimeEvaluation.Trusted(126, 6),
                evaluateActiveSessionTime(anchors.copy(bootCount = stored), now.copy(bootCount = current)),
            )
            assertEquals(
                ActiveSessionTimeEvaluation.NeedsReview(
                    ActiveSessionRecoveryReason.REBOOTED,
                    (beyondUptime - 10_000L) / 1_000,
                ),
                evaluateActiveSessionTime(
                    anchors.copy(bootCount = stored),
                    now.copy(bootCount = current, wallClockMillis = beyondUptime),
                ),
            )
        }
        for (wall in listOf(-1L, 9_999L, 10_000L, 70_000L, Long.MAX_VALUE)) {
            assertEquals(
                ActiveSessionTimeEvaluation.NeedsReview(
                    ActiveSessionRecoveryReason.REBOOTED,
                    if (wall < 10_000L) null else (wall - 10_000L) / 1_000,
                ),
                evaluateActiveSessionTime(anchors, now.copy(bootCount = 3, wallClockMillis = wall)),
            )
        }
        for (elapsed in listOf(86_399_999L, 86_400_000L, 86_400_001L, 86_401_000L)) {
            val evaluation =
                evaluateActiveSessionTime(
                    anchors.copy(checkpointedDurationSeconds = 0),
                    now.copy(elapsedRealtimeMillis = 10_000L + elapsed),
                )
            if (elapsed < 86_400_000L) {
                assertEquals(ActiveSessionTimeEvaluation.Trusted(86_399, 86_399), evaluation)
            } else {
                assertEquals(
                    ActiveSessionTimeEvaluation.NeedsReview(ActiveSessionRecoveryReason.LONG_RUNNING, elapsed / 1_000),
                    evaluation,
                )
            }
        }
    }

    @Test fun annotationWidthTextAndFiveDecimalCoordinatesRoundTripAtEveryEdge() {
        for (width in listOf(0.499f, 0.5f, 0.501f, 63.999f, 64f, 64.001f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val shape =
                ShapePayload(NormalizedPatternPoint(0.123456f, 0.999999f), NormalizedPatternPoint(1f, 1f), 123, width)
            val encoded = PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.LINE, shape)
            if (!width.isFinite()) {
                assertNull(encoded)
            } else {
                val decoded =
                    PatternAnnotationPayloadCodec.decode(
                        PatternAnnotationKind.LINE,
                        checkNotNull(encoded),
                    ) as ShapePayload
                assertEquals(width.coerceIn(0.5f, 64f), decoded.strokeWidth, 0.00001f)
                assertEquals(0.12346f, decoded.start.x, 0f)
                assertEquals(1f, decoded.start.y, 0f)
                assertEquals(encoded, PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.LINE, decoded))
            }
        }
        for (size in listOf(7.999f, 8f, 8.001f, 95.999f, 96f, 96.001f, Float.NaN, Float.NEGATIVE_INFINITY)) {
            val payload = TextBoxPayload(NormalizedPatternBounds(0f, 0f, 1f, 1f), "Text", size, 123)
            val encoded = PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.TEXT_BOX, payload)
            if (!size.isFinite()) {
                assertNull(encoded)
            } else {
                val decoded =
                    PatternAnnotationPayloadCodec.decode(
                        PatternAnnotationKind.TEXT_BOX,
                        checkNotNull(encoded),
                    ) as TextBoxPayload
                assertEquals(size.coerceIn(8f, 96f), decoded.textSizeSp, 0.00001f)
            }
        }
    }
}

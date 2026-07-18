package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationPayloadCodecTest {
    @Test
    fun `drawing annotation payloads round trip with their matching kind`() {
        assertRoundTrips(
            samples =
                listOf(
                    PatternAnnotationKind.FREEHAND to
                        FreehandPayload(
                            points =
                                listOf(
                                    NormalizedPatternPoint(0.1f, 0.2f, 0.4f),
                                    NormalizedPatternPoint(0.8f, 0.9f),
                                ),
                            argb = 0xFF112233.toInt(),
                            strokeWidth = 3f,
                            pressureEnabled = true,
                        ),
                    PatternAnnotationKind.HIGHLIGHTER to
                        FreehandPayload(
                            points =
                                listOf(
                                    NormalizedPatternPoint(0.2f, 0.3f),
                                    NormalizedPatternPoint(0.7f, 0.3f),
                                ),
                            argb = 0x66FFE000,
                            strokeWidth = 18f,
                        ),
                    PatternAnnotationKind.LINE to
                        ShapePayload(
                            start = NormalizedPatternPoint(0.1f, 0.2f),
                            end = NormalizedPatternPoint(0.9f, 0.8f),
                            strokeArgb = 0xFF000000.toInt(),
                            strokeWidth = 2f,
                        ),
                ),
        )
    }

    @Test
    fun `content annotation payloads round trip with their matching kind`() {
        assertRoundTrips(
            samples =
                listOf(
                    PatternAnnotationKind.TEXT_BOX to
                        TextBoxPayload(
                            bounds = NormalizedPatternBounds(0.1f, 0.2f, 0.5f, 0.4f),
                            text = "Neuletiheys 20 × 28",
                            textSizeSp = 16f,
                            textArgb = 0xFF223344.toInt(),
                        ),
                    PatternAnnotationKind.CALLOUT to
                        CalloutPayload(
                            bounds = NormalizedPatternBounds(0.2f, 0.3f, 0.4f, 0.5f),
                            symbol = PatternCalloutSymbol.STAR,
                            title = "Muista",
                            description = "Lisää silmukka",
                            argb = 0xFFAA5500.toInt(),
                        ),
                    PatternAnnotationKind.CHART_REGION to chartRegion(),
                    PatternAnnotationKind.CHART_TRACKER to
                        ChartTrackerPayload(
                            region = chartRegion(),
                            trackingMode = ChartTrackingMode.CROSSHAIR,
                            counterType = ChartCounterType.EXTRA,
                            extraCounterId = 42L,
                            counterStartValue = 5,
                            gridStartIndex = 1,
                            wrapAtEnd = true,
                            highlightArgb = 0x664285F4,
                            highlightAlpha = 0.4f,
                        ),
                ),
        )
    }

    @Test
    fun `decoder ignores fields added by a newer app`() {
        val encoded =
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(
                    PatternAnnotationKind.LINE,
                    ShapePayload(
                        start = NormalizedPatternPoint(0.1f, 0.2f),
                        end = NormalizedPatternPoint(0.3f, 0.4f),
                        strokeArgb = 1,
                        strokeWidth = 2f,
                    ),
                ),
            )
        val withUnknownField = encoded.payloadJson.replaceFirst("{", "{\"futureField\":true,")

        assertTrue(
            PatternAnnotationPayloadCodec.decode(
                PatternAnnotationKind.LINE,
                encoded.payloadVersion,
                withUnknownField,
            ) is ShapePayload,
        )
    }

    @Test
    fun `unknown payload versions and malformed payloads are skipped safely`() {
        assertNull(PatternAnnotationPayloadCodec.decode(PatternAnnotationKind.FREEHAND, 99, "{}"))
        assertNull(PatternAnnotationPayloadCodec.decode(PatternAnnotationKind.FREEHAND, 1, "not-json"))
    }

    @Test
    fun `non finite values are rejected and finite coordinates are normalized`() {
        val invalid =
            FreehandPayload(
                points = listOf(NormalizedPatternPoint(Float.NaN, 0.2f), NormalizedPatternPoint(0.3f, 0.4f)),
                argb = 1,
                strokeWidth = 2f,
            )
        val infinite =
            FreehandPayload(
                points =
                    listOf(
                        NormalizedPatternPoint(0.1f, 0.2f),
                        NormalizedPatternPoint(Float.POSITIVE_INFINITY, 0.4f),
                    ),
                argb = 1,
                strokeWidth = 2f,
            )
        val normalizable =
            FreehandPayload(
                points =
                    listOf(
                        NormalizedPatternPoint(-0.2f, 0.1234567f, 1.4f),
                        NormalizedPatternPoint(1.3f, 0.8f, -0.2f),
                    ),
                argb = 1,
                strokeWidth = 2f,
            )

        assertNull(PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.FREEHAND, invalid))
        assertNull(PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.FREEHAND, infinite))
        val normalized =
            requireNotNull(
                PatternAnnotationPayloadCodec.decode(
                    PatternAnnotationKind.FREEHAND,
                    requireNotNull(PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.FREEHAND, normalizable)),
                ),
            ) as FreehandPayload
        assertEquals(NormalizedPatternPoint(0f, 0.12346f, 1f), normalized.points.first())
        assertEquals(NormalizedPatternPoint(1f, 0.8f, 0f), normalized.points.last())
    }

    @Test
    fun `payload size and freehand point limits are enforced`() {
        val tooLargeText = "x".repeat(PatternAnnotationPayloadCodec.MAX_PAYLOAD_BYTES)
        val tooManyPoints =
            List(PatternAnnotationLimits.MAX_FREEHAND_POINTS + 1) { index ->
                NormalizedPatternPoint(index.toFloat() / PatternAnnotationLimits.MAX_FREEHAND_POINTS, 0.5f)
            }

        assertNull(
            PatternAnnotationPayloadCodec.encode(
                PatternAnnotationKind.TEXT_BOX,
                TextBoxPayload(
                    bounds = NormalizedPatternBounds(0f, 0f, 1f, 1f),
                    text = tooLargeText,
                    textSizeSp = 16f,
                    textArgb = 1,
                ),
            ),
        )
        assertNull(
            PatternAnnotationPayloadCodec.encode(
                PatternAnnotationKind.FREEHAND,
                FreehandPayload(tooManyPoints, argb = 1, strokeWidth = 2f),
            ),
        )
    }

    @Test
    fun `payload kind mismatch is rejected`() {
        assertNull(
            PatternAnnotationPayloadCodec.encode(
                PatternAnnotationKind.TEXT_BOX,
                ShapePayload(
                    start = NormalizedPatternPoint(0f, 0f),
                    end = NormalizedPatternPoint(1f, 1f),
                    strokeArgb = 1,
                    strokeWidth = 2f,
                ),
            ),
        )
    }

    private fun chartRegion() =
        ChartRegionPayload(
            bounds = NormalizedPatternBounds(0.1f, 0.2f, 0.9f, 0.8f),
            name = "Kaavio A",
            rows = 20,
            columns = 30,
            rowDirection = ChartRowDirection.BOTTOM_TO_TOP,
            columnDirection = ChartColumnDirection.ALTERNATING,
        )

    private fun assertRoundTrips(samples: List<Pair<PatternAnnotationKind, PatternAnnotationPayload>>) {
        samples.forEach { (kind, payload) ->
            val encoded = requireNotNull(PatternAnnotationPayloadCodec.encode(kind, payload))

            assertEquals(PatternAnnotationPayloadCodec.CURRENT_VERSION, encoded.payloadVersion)
            assertEquals(
                payload,
                PatternAnnotationPayloadCodec.decode(kind, encoded.payloadVersion, encoded.payloadJson),
            )
        }
    }
}

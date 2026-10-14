package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PatternAnnotationBoundaryTest {
    @Test
    fun `multibyte text is admitted through the exact UTF-8 byte limit`() {
        val template = TextBoxPayload(NormalizedPatternBounds(0f, 0f, 1f, 1f), "x", 16f, 1)
        val encodedTemplate =
            requireNotNull(PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.TEXT_BOX, template))
        val overhead = encodedTemplate.payloadJson.encodeToByteArray().size - 1
        val textBytes = PatternAnnotationPayloadCodec.MAX_PAYLOAD_BYTES - overhead
        val text = "ä".repeat(textBytes / 2) + "x".repeat(textBytes % 2)
        val belowText = "ä".repeat((textBytes - 1) / 2) + "x".repeat((textBytes - 1) % 2)
        val atLimit =
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.TEXT_BOX, template.copy(text = text)),
            )
        val belowLimit =
            requireNotNull(
                PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.TEXT_BOX, template.copy(text = belowText)),
            )

        assertEquals(PatternAnnotationPayloadCodec.MAX_PAYLOAD_BYTES, atLimit.payloadJson.encodeToByteArray().size)
        assertNotNull(PatternAnnotationPayloadCodec.decode(PatternAnnotationKind.TEXT_BOX, atLimit))
        assertEquals(
            PatternAnnotationPayloadCodec.MAX_PAYLOAD_BYTES - 1,
            belowLimit.payloadJson.encodeToByteArray().size,
        )
        assertNotNull(PatternAnnotationPayloadCodec.decode(PatternAnnotationKind.TEXT_BOX, belowLimit))
        assertNull(
            PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.TEXT_BOX, template.copy(text = text + "x")),
        )
        assertNull(PatternAnnotationPayloadCodec.decode(PatternAnnotationKind.TEXT_BOX, 1, atLimit.payloadJson + " "))
    }

    @Test
    fun `freehand admits 2047 and 2048 points but rejects 2049`() {
        listOf(2_047, 2_048, 2_049).forEach { count ->
            val payload = FreehandPayload(List(count) { NormalizedPatternPoint(0.5f, 0.5f) }, 1, 2f)
            val encoded = PatternAnnotationPayloadCodec.encode(PatternAnnotationKind.FREEHAND, payload)
            if (count <= 2_048) {
                val decoded =
                    PatternAnnotationPayloadCodec.decode(PatternAnnotationKind.FREEHAND, requireNotNull(encoded))
                assertEquals(count, (decoded as FreehandPayload).points.size)
            } else {
                assertNull(encoded)
            }
        }
    }

    @Test
    fun `chart dimensions and total cell limits are independent`() {
        val region =
            ChartRegionPayload(
                NormalizedPatternBounds(0f, 0f, 1f, 1f),
                "Grid",
                1,
                1,
                ChartRowDirection.TOP_TO_BOTTOM,
                ChartColumnDirection.LEFT_TO_RIGHT,
            )
        listOf(1 to 1, 999 to 1, 1 to 999, 99 to 101, 100 to 100).forEach { (rows, columns) ->
            assertNotNull(
                PatternAnnotationPayloadCodec.encode(
                    PatternAnnotationKind.CHART_REGION,
                    region.copy(rows = rows, columns = columns),
                ),
            )
        }
        listOf(0 to 1, -1 to 1, 1 to 0, 1_000 to 1, 1 to 1_000, 73 to 137, Int.MAX_VALUE to Int.MAX_VALUE)
            .forEach { (rows, columns) ->
                assertNull(
                    PatternAnnotationPayloadCodec.encode(
                        PatternAnnotationKind.CHART_REGION,
                        region.copy(rows = rows, columns = columns),
                    ),
                )
            }
    }
}

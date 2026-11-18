package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.ProjectSourceFiles
import com.finnvek.knittools.domain.model.CalloutPayload
import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartCounterType
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartRowDirection
import com.finnvek.knittools.domain.model.ChartTrackerPayload
import com.finnvek.knittools.domain.model.ChartTrackingMode
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import com.finnvek.knittools.domain.model.TextBoxPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationHitBudgetTest {
    private val hit = NormalizedPatternPoint(0.5f, 0.5f)
    private val miss = NormalizedPatternPoint(0.5f, 0.9f)
    private val points = List(2_048) { NormalizedPatternPoint(it / 2_047f, 0.5f) }
    private val stroke =
        PatternAnnotation(1L, 1L, 0, PatternAnnotationKind.FREEHAND, FreehandPayload(points, 0, 2f), 0L)

    @Test fun maximumStrokeHitMissAndDegenerateCases() {
        assertTrue(isPointNearStroke(hit, points, 0f))
        assertFalse(isPointNearStroke(miss, points, 0.01f))
        assertFalse(isPointNearStroke(hit, emptyList(), 1f))
        assertTrue(isPointNearStroke(hit, listOf(hit), 0f))
        assertFalse(isPointNearStroke(hit, listOf(hit), -0.01f))
        assertFalse(isPointNearStroke(hit, points, Float.NaN))
        assertEquals(stroke, topmostAnnotationAt(listOf(stroke), hit, 0f))
        assertNull(topmostAnnotationAt(List(8) { stroke.copy(id = it.toLong()) }, miss, 0.01f))
        assertThrows(PatternAnnotationPageLimitException::class.java) {
            topmostAnnotationAt(List(9) { stroke }, miss, 0.01f)
        }
    }

    @Test fun unsortedTopmostKeysAndStableEqualKeys() {
        val low = stroke.copy(zIndex = Long.MIN_VALUE, id = Long.MAX_VALUE)
        val high = stroke.copy(zIndex = Long.MAX_VALUE, id = 2L)
        val tie = high.copy(id = 3L)
        for (input in listOf(listOf(tie, low, high), listOf(high, low, tie))) {
            assertEquals(tie, topmostAnnotationAt(input, hit, 0f))
        }
        val equal = tie.copy(updatedAt = tie.updatedAt + 1L)
        assertEquals(tie, topmostAnnotationAt(listOf(tie, equal), hit, 0f))
    }

    @Test fun textCalloutAndChartBoundsKeepTheirHitBehavior() {
        val bounds = NormalizedPatternBounds(0.3f, 0.3f, 0.7f, 0.7f)
        val region =
            ChartRegionPayload(
                bounds,
                "Chart",
                10,
                10,
                ChartRowDirection.TOP_TO_BOTTOM,
                ChartColumnDirection.LEFT_TO_RIGHT,
            )
        val annotations =
            listOf(
                stroke.copy(kind = PatternAnnotationKind.TEXT_BOX, payload = TextBoxPayload(bounds, "Text", 12f, 0)),
                stroke.copy(
                    kind = PatternAnnotationKind.CALLOUT,
                    payload = CalloutPayload(bounds, PatternCalloutSymbol.NOTE, "Note", "", 0),
                ),
                stroke.copy(kind = PatternAnnotationKind.CHART_REGION, payload = region),
                stroke.copy(
                    kind = PatternAnnotationKind.CHART_TRACKER,
                    payload =
                        ChartTrackerPayload(
                            region,
                            ChartTrackingMode.ACTIVE_ROW,
                            ChartCounterType.MAIN,
                            counterStartValue = 0,
                            gridStartIndex = 0,
                            wrapAtEnd = false,
                            highlightArgb = 0,
                            highlightAlpha = 0.2f,
                        ),
                ),
            )
        for (annotation in annotations) {
            assertEquals(annotation, topmostAnnotationAt(listOf(annotation), hit, 0.01f))
            assertEquals(
                annotation,
                topmostAnnotationAt(listOf(annotation), NormalizedPatternPoint(0.29f, 0.5f), 0.02f),
            )
            assertNull(topmostAnnotationAt(listOf(annotation), miss, 0.01f))
        }
    }

    @Test fun gestureSourceDoesNotSortOrAllocateSegmentCollections() {
        val source = "app/src/main/java/com/finnvek/knittools/domain/calculator"
        val editing =
            ProjectSourceFiles
                .read("$source/PatternAnnotationEditing.kt")
                .substringAfter("fun topmostAnnotationAt(")
                .substringBefore("fun translatePatternAnnotation(")
        val geometry =
            ProjectSourceFiles
                .read("$source/PatternAnnotationGeometry.kt")
                .substringAfter("fun isPointNearStroke(")
                .substringBefore("fun isPointNearShape(")
        for (forbidden in listOf("sorted", "toList(", "zipWithNext", "map(", "filter(", "asSequence")) {
            assertFalse(forbidden, editing.contains(forbidden) || geometry.contains(forbidden))
        }
    }
}

package com.finnvek.knittools.domain.model

import org.junit.Assert.assertThrows
import org.junit.Test

class PatternAnnotationPageBudgetTest {
    private val shape = ShapePayload(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f), 0, 2f)

    @Test fun exactCountAndNextAnnotation() {
        val budget = PatternAnnotationPageBudget()
        repeat(PatternAnnotationPageBudget.MAX_ANNOTATIONS) { budget.add(shape) }
        assertThrows(PatternAnnotationPageLimitException::class.java) { budget.add(shape) }
    }

    @Test fun exactWorkAndOneAdditionalPoint() {
        val budget = PatternAnnotationPageBudget()
        val maximumStroke = FreehandPayload(List(2_048) { NormalizedPatternPoint(0f, 0f) }, 0, 2f)
        repeat(8) { budget.add(maximumStroke) }
        assertThrows(PatternAnnotationPageLimitException::class.java) {
            budget.add(maximumStroke.copy(points = listOf(NormalizedPatternPoint(0f, 0f))))
        }
    }

    @Test fun payloadAndLegacyGeometryAreBoundedToo() {
        val budget = PatternAnnotationPageBudget()
        budget.add(shape, PatternAnnotationPageBudget.MAX_PAYLOAD_BYTES)
        assertThrows(PatternAnnotationPageLimitException::class.java) { budget.add(shape, 1L) }
        val legacy = FreehandPayload(emptyList(), 0, 2f, legacyPathData = "x".repeat(16_384))
        val legacyBudget = PatternAnnotationPageBudget()
        legacyBudget.add(legacy)
        assertThrows(PatternAnnotationPageLimitException::class.java) { legacyBudget.add(shape) }
        assertThrows(PatternAnnotationPageLimitException::class.java) {
            PatternAnnotationPageBudget().add(legacy.copy(points = List(2_049) { NormalizedPatternPoint(0f, 0f) }))
        }
    }
}

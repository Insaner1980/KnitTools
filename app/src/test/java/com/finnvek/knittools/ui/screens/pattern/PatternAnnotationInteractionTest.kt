package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationInteractionTest {
    @Test
    fun `two pointers stay reserved for viewport transformation`() {
        assertFalse(
            shouldHandleAnnotationPointer(
                activeTool = PatternAnnotationTool.PEN,
                pointerType = PatternInputPointerType.TOUCH,
                activePointerCount = 2,
                stylusPresent = false,
            ),
        )
    }

    @Test
    fun `touch is rejected as palm input while stylus is present`() {
        assertFalse(
            shouldHandleAnnotationPointer(
                activeTool = PatternAnnotationTool.PEN,
                pointerType = PatternInputPointerType.TOUCH,
                activePointerCount = 1,
                stylusPresent = true,
            ),
        )
        assertTrue(
            shouldHandleAnnotationPointer(
                activeTool = PatternAnnotationTool.PEN,
                pointerType = PatternInputPointerType.STYLUS,
                activePointerCount = 1,
                stylusPresent = true,
            ),
        )
    }

    @Test
    fun `stylus eraser overrides selected drawing tool`() {
        assertEquals(
            PatternAnnotationTool.ERASER,
            resolvedAnnotationTool(PatternAnnotationTool.HIGHLIGHTER, PatternInputPointerType.ERASER),
        )
    }

    @Test
    fun `highlighter axis lock uses the dominant direction from first point`() {
        val points =
            listOf(
                NormalizedPatternPoint(0.2f, 0.3f),
                NormalizedPatternPoint(0.7f, 0.34f),
            )

        assertEquals(
            listOf(NormalizedPatternPoint(0.2f, 0.3f), NormalizedPatternPoint(0.7f, 0.3f)),
            lockHighlighterPoints(points, PatternHighlighterAxisLock.DOMINANT_AXIS),
        )
        assertEquals(
            listOf(NormalizedPatternPoint(0.2f, 0.3f), NormalizedPatternPoint(0.2f, 0.34f)),
            lockHighlighterPoints(points, PatternHighlighterAxisLock.VERTICAL),
        )
    }
}

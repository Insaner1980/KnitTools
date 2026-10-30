package com.finnvek.knittools.ui.layout

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun `width boundaries and short height select reachable navigation`() {
        for ((width, expected) in listOf(
            599 to AdaptiveWidth.Compact,
            600 to AdaptiveWidth.Medium,
            601 to AdaptiveWidth.Medium,
            839 to AdaptiveWidth.Medium,
            840 to AdaptiveWidth.Expanded,
            841 to AdaptiveWidth.Expanded,
        )) {
            val tall = AdaptiveLayoutSize(width.dp, 480.dp)
            val short = AdaptiveLayoutSize(width.dp, 479.dp)
            assertEquals(expected, tall.widthClass)
            assertEquals(width >= 600, tall.useNavigationRail)
            assertFalse(short.useNavigationRail)
            assertTrue(short.isCompactHeight)
        }
    }

    @Test
    fun `flat nonoccluding crease leaves the whole viewport available`() {
        val bounds = IntRect(0, 24, 1200, 800)
        val crease = WindowFold(IntRect(600, 0, 600, 900), true, false, false)
        assertEquals(bounds, unobstructedWindowBounds(bounds, listOf(crease), LayoutDirection.Ltr))
    }

    @Test
    fun `book hinge includes its occluded width and respects start on equal panes`() {
        val bounds = IntRect(20, 24, 1220, 800)
        val hinge = WindowFold(IntRect(610, 0, 630, 900), true, true, true)
        assertEquals(
            IntRect(20, 24, 610, 800),
            unobstructedWindowBounds(bounds, listOf(hinge), LayoutDirection.Ltr),
        )
        assertEquals(
            IntRect(630, 24, 1220, 800),
            unobstructedWindowBounds(bounds, listOf(hinge), LayoutDirection.Rtl),
        )
    }

    @Test
    fun `tabletop zero height separator selects a single unobstructed pane`() {
        val bounds = IntRect(0, 24, 1000, 900)
        val fold = WindowFold(IntRect(0, 400, 1000, 400), false, true, false)
        assertEquals(
            IntRect(0, 400, 1000, 900),
            unobstructedWindowBounds(bounds, listOf(fold), LayoutDirection.Ltr),
        )
    }

    @Test
    fun `occlusion without separation is also avoided`() {
        val bounds = IntRect(0, 0, 1000, 800)
        val fold = WindowFold(IntRect(400, 0, 420, 800), true, false, true)
        assertEquals(
            IntRect(420, 0, 1000, 800),
            unobstructedWindowBounds(bounds, listOf(fold), LayoutDirection.Ltr),
        )
    }

    @Test
    fun `fold outside the inset or resized host does not narrow it`() {
        val bounds = IntRect(0, 40, 500, 800)
        val folds =
            listOf(
                WindowFold(IntRect(600, 0, 620, 900), true, true, true),
                WindowFold(IntRect(0, 0, 1000, 30), false, true, true),
            )
        assertEquals(bounds, unobstructedWindowBounds(bounds, folds, LayoutDirection.Ltr))
    }

    @Test
    fun `multiple separators select the largest remaining pane regardless of emission order`() {
        val bounds = IntRect(0, 0, 1800, 900)
        val folds =
            listOf(
                WindowFold(IntRect(500, 0, 520, 900), true, true, true),
                WindowFold(IntRect(1000, 0, 1020, 900), true, true, true),
            )
        val expected = IntRect(1020, 0, 1800, 900)
        assertEquals(expected, unobstructedWindowBounds(bounds, folds, LayoutDirection.Ltr))
        assertEquals(expected, unobstructedWindowBounds(bounds, folds.reversed(), LayoutDirection.Ltr))
    }
}

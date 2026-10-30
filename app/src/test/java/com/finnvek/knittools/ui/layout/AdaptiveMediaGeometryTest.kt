package com.finnvek.knittools.ui.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.finnvek.knittools.ui.screens.counter.PhotoViewportState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveMediaGeometryTest {
    @Test
    fun `grid retains two compact columns and bounds all wider cells at either density`() {
        for (density in listOf(1f, 2.5f)) {
            for (width in listOf(360, 411, 599, 600, 601, 800, 839, 840, 841, 1280, 1600)) {
                with(PhotoGridCells) {
                    with(Density(density)) {
                        val available = ((width - 32) * density).toInt()
                        val spacing = (8 * density).toInt()
                        val sizes = calculateCrossAxisCellSizes(available, spacing)
                        assertEquals(available, sizes.sum() + (sizes.size - 1) * spacing)
                        assertTrue(sizes.all { it / density <= 221 })
                        if (width <= 411) assertEquals(2, sizes.size)
                    }
                }
            }
        }
    }

    @Test
    fun `photo keeps aspect fit pan bounds after resize and resets at minimum zoom`() {
        val image = Size(1200f, 800f)
        val state = PhotoViewportState().transform(3f, Offset(2000f, -2000f), Size(360f, 700f), image)
        assertEquals(3f, state.scale)
        for (viewport in listOf(Size(360f, 700f), Size(800f, 270f), Size(800f, 1150f), Size(1280f, 670f))) {
            val offset = state.offset(viewport, image)
            val fit = minOf(viewport.width / image.width, viewport.height / image.height)
            assertTrue(kotlin.math.abs(offset.x) <= ((image.width * fit * 3 - viewport.width) / 2).coerceAtLeast(0f))
            assertTrue(kotlin.math.abs(offset.y) <= ((image.height * fit * 3 - viewport.height) / 2).coerceAtLeast(0f))
        }
        assertEquals(PhotoViewportState(), state.transform(0.1f, Offset.Zero, Size(800f, 400f), image))
        assertEquals(5f, state.transform(10f, Offset.Zero, Size(800f, 400f), image).scale)
    }

    @Test
    fun `popup remains in left right top or bottom pane for either layout direction`() {
        val panes =
            listOf(
                IntRect(0, 24, 628, 776),
                IntRect(652, 24, 1280, 776),
                IntRect(0, 24, 1280, 388),
                IntRect(0, 412, 1280, 776),
            )
        for (pane in panes) {
            for (direction in LayoutDirection.entries) {
                val size = IntSize(280, minOf(600, pane.height))
                val position =
                    PaneMenuPositionProvider(pane).calculatePosition(
                        IntRect(pane.right - 48, pane.bottom - 48, pane.right, pane.bottom),
                        IntSize(1280, 800),
                        direction,
                        size,
                    )
                assertTrue(position.x >= pane.left && position.x + size.width <= pane.right)
                assertTrue(position.y >= pane.top && position.y + size.height <= pane.bottom)
            }
        }
    }
}

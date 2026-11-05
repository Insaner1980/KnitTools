package com.finnvek.knittools.ui.screens.pattern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.ui.pinchZoomIn
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Synthetic page geometry uses the production viewport and pointer-to-page transform. */
class AdaptivePdfViewportTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun normalizedGeometryZoomAndScrollSurviveResizeAndSavedStateRestoration() {
        var size by mutableStateOf(360 to 700)
        var layout: PatternViewportLayout? = null
        val page = ImageBitmap(400, 1000)
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                Box(Modifier.requiredSize(size.first.dp, size.second.dp)) {
                    PatternDocumentViewport(
                        renderedBitmapProvider = { page },
                        contentDescription = "Synthetic page",
                        modifier = Modifier.fillMaxSize().testTag("reader"),
                        overlay = { viewport ->
                            layout = viewport
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(Color.Red, 8f, Offset(this.size.width * 0.4f, this.size.height * 0.6f))
                            }
                        },
                    )
                }
            }
        }
        val reader = rule.onNodeWithTag("reader")
        reader.performTouchInput { swipeUp() }
        val scrollMatcher =
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                SemanticsProperties.VerticalScrollAxisRange,
            )
        val beforeScroll =
            rule
                .onNode(
                    scrollMatcher,
                ).fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        assertTrue(beforeScroll > 0)
        val anchor = beforeScroll / checkNotNull(layout).coordinateTransform.pageHeight
        reader.pinchZoomIn()
        rule.waitForIdle()
        val scale = checkNotNull(layout).state.scale
        assertTrue("Pinch changes the viewport", scale > 1f)
        var checkedPixels = 0
        for (next in listOf(800 to 270, 800 to 1100, 1280 to 670, 360 to 700)) {
            rule.runOnIdle { size = next }
            rule.waitForIdle()
            val viewport = checkNotNull(layout)
            assertEquals(scale, viewport.state.scale, 0.01f)
            val point = NormalizedPatternPoint(0.4f, 0.6f)
            val screen = checkNotNull(viewport.coordinateTransform.pageToScreen(point))
            val roundTrip = checkNotNull(viewport.coordinateTransform.screenToPage(screen))
            assertEquals(point.x, roundTrip.x, 0.0001f)
            assertEquals(point.y, roundTrip.y, 0.0001f)
            val range =
                rule
                    .onNode(
                        scrollMatcher,
                    ).fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange]
            val expected = (anchor * viewport.coordinateTransform.pageHeight).coerceAtMost(range.maxValue())
            assertTrue("Page anchor after $next", abs(expected - range.value()) < 3f)
            val image = reader.captureToImage()
            val node = reader.fetchSemanticsNode()
            val x = screen.x.toInt()
            val y = (screen.y - range.value()).toInt()
            val visibleBounds = rule.onRoot().fetchSemanticsNode().boundsInRoot
            // Oversized synthetic viewports include pixels outside the physical window that Android does not draw.
            val visiblePoint = node.positionInRoot + Offset(x.toFloat(), y.toFloat())
            if (x in 1 until image.width - 1 && y in 1 until image.height - 1 && visibleBounds.contains(visiblePoint)) {
                val pixel = image.toPixelMap()[x, y]
                // Device capture can map the solid red marker below 0.9; verify its position and red hue.
                assertTrue(
                    "Overlay at $next: pixel=$pixel expected=($x,$y)",
                    pixel.red > 0.8f && pixel.green < 0.1f && pixel.blue < 0.1f,
                )
                checkedPixels++
            }
        }
        assertTrue("At least one transformed marker was visually checked", checkedPixels > 0)
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertEquals(scale, checkNotNull(layout).state.scale, 0.01f)
        val range = rule.onNode(scrollMatcher).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(abs(anchor * checkNotNull(layout).coordinateTransform.pageHeight - range.value()) < 3f)
    }

    @Test
    fun resizeCancelsAnInterruptedStrokeWithoutCommittingOrApplyingTheTransformTwice() {
        var width by mutableStateOf(360)
        var layout: PatternViewportLayout? = null
        var started: NormalizedPatternPoint? = null
        var commits = 0
        var cancels = 0
        rule.setContent {
            KnitToolsTheme(isDarkTheme = true) {
                Box(Modifier.requiredSize(width.dp, 400.dp)) {
                    PatternDocumentViewport(
                        renderedBitmapProvider = { ImageBitmap(400, 400) },
                        contentDescription = "Synthetic page",
                        modifier = Modifier.fillMaxSize(),
                        interactionOverlay = {
                            layout = it
                            PatternAnnotationInputOverlay(
                                activeTool = PatternAnnotationTool.PEN,
                                coordinateTransform = it.coordinateTransform,
                                viewportScale = it.state.scale,
                                pressureEnabled = false,
                                actions =
                                    PatternAnnotationInputActions(
                                        { point -> started = point },
                                        {},
                                        { commits++ },
                                        { cancels++ },
                                        {},
                                        {},
                                    ),
                                modifier = Modifier.fillMaxSize().testTag("drawing"),
                            )
                        },
                    )
                }
            }
        }
        rule.onNodeWithTag("drawing").performTouchInput { down(percentOffset(0.4f, 0.6f)) }
        rule.runOnIdle { width = 800 }
        rule.waitForIdle()
        assertEquals(0, commits)
        assertTrue(cancels > 0)
        assertEquals(0.4f, checkNotNull(started).x, 0.01f)
        assertEquals(0.6f, checkNotNull(started).y, 0.01f)
        assertEquals(800 * rule.density.density, checkNotNull(layout).coordinateTransform.pageWidth, 1f)
        rule.onNodeWithTag("drawing").performTouchInput { up() }
        assertEquals(0, commits)
    }
}

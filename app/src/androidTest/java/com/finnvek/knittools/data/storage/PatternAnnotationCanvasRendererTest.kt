package com.finnvek.knittools.data.storage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.domain.calculator.ChartCell
import com.finnvek.knittools.domain.calculator.ChartTrackerHighlight
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
import com.finnvek.knittools.domain.model.PatternCalloutSymbol
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.domain.model.TextBoxPayload
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternAnnotationCanvasRendererTest {
    @Test
    fun rendererDrawsSupportedPayloadsOnAndroidCanvas() {
        val annotations =
            listOf(
                annotation(
                    PatternAnnotationKind.FREEHAND,
                    FreehandPayload(
                        points = listOf(NormalizedPatternPoint(0.1f, 0.1f), NormalizedPatternPoint(0.9f, 0.2f)),
                        argb = Color.BLACK,
                        strokeWidth = 8f,
                    ),
                ),
                annotation(
                    PatternAnnotationKind.RECTANGLE,
                    ShapePayload(
                        start = NormalizedPatternPoint(0.1f, 0.3f),
                        end = NormalizedPatternPoint(0.5f, 0.55f),
                        strokeArgb = Color.RED,
                        strokeWidth = 6f,
                        fillArgb = Color.YELLOW,
                        fillAlpha = 0.4f,
                    ),
                ),
                annotation(
                    PatternAnnotationKind.TEXT_BOX,
                    TextBoxPayload(
                        bounds = NormalizedPatternBounds(0.55f, 0.3f, 0.9f, 0.5f),
                        text = "Note",
                        textSizeSp = 32f,
                        textArgb = Color.BLUE,
                    ),
                ),
                annotation(
                    PatternAnnotationKind.CALLOUT,
                    CalloutPayload(
                        bounds = NormalizedPatternBounds(0.1f, 0.6f, 0.45f, 0.75f),
                        symbol = PatternCalloutSymbol.STAR,
                        title = "Check",
                        description = "Repeat",
                        argb = Color.MAGENTA,
                    ),
                ),
                annotation(
                    PatternAnnotationKind.CHART_REGION,
                    ChartRegionPayload(
                        bounds = NormalizedPatternBounds(0.5f, 0.6f, 0.9f, 0.9f),
                        name = "Chart",
                        rows = 4,
                        columns = 5,
                        rowDirection = ChartRowDirection.BOTTOM_TO_TOP,
                        columnDirection = ChartColumnDirection.LEFT_TO_RIGHT,
                    ),
                ),
                annotation(
                    kind = PatternAnnotationKind.CHART_TRACKER,
                    payload =
                        ChartTrackerPayload(
                            region =
                                ChartRegionPayload(
                                    bounds = NormalizedPatternBounds(0.5f, 0.6f, 0.9f, 0.9f),
                                    name = "Tracked chart",
                                    rows = 4,
                                    columns = 5,
                                    rowDirection = ChartRowDirection.TOP_TO_BOTTOM,
                                    columnDirection = ChartColumnDirection.LEFT_TO_RIGHT,
                                ),
                            trackingMode = ChartTrackingMode.ACTIVE_ROW,
                            counterType = ChartCounterType.MAIN,
                            counterStartValue = 0,
                            gridStartIndex = 0,
                            wrapAtEnd = false,
                            highlightArgb = Color.YELLOW,
                            highlightAlpha = 0.5f,
                        ),
                    id = TRACKER_ID,
                ),
            )

        annotations.forEach { annotation ->
            val bitmap = render(annotation)
            assertTrue(
                "${annotation.kind} did not render visible pixels",
                bitmap.nonTransparentPixels().isNotEmpty(),
            )
            bitmap.recycle()
        }
    }

    @Test
    fun textPayloadsStayInsideTheirBounds() {
        listOf(
            annotation(
                PatternAnnotationKind.TEXT_BOX,
                TextBoxPayload(
                    bounds = TEXT_BOUNDS,
                    text = "A very long annotation that must wrap inside its box",
                    textSizeSp = 48f,
                    textArgb = Color.BLUE,
                ),
            ),
            annotation(
                PatternAnnotationKind.CALLOUT,
                CalloutPayload(
                    bounds = TEXT_BOUNDS,
                    symbol = PatternCalloutSymbol.NOTE,
                    title = "A very long callout title",
                    description = "A long description that must remain inside",
                    argb = Color.MAGENTA,
                ),
            ),
        ).forEach { annotation ->
            val bitmap = render(annotation)
            val outsidePixels =
                bitmap.nonTransparentPixels().filter { (x, y) ->
                    x < TEXT_BOUNDS.left * bitmap.width ||
                        x > TEXT_BOUNDS.right * bitmap.width ||
                        y < TEXT_BOUNDS.top * bitmap.height ||
                        y > TEXT_BOUNDS.bottom * bitmap.height
                }

            assertTrue("${annotation.kind} rendered outside its bounds", outsidePixels.isEmpty())
            bitmap.recycle()
        }
    }

    private fun render(annotation: PatternAnnotation): Bitmap {
        val bitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888)
        PatternAnnotationCanvasRenderer.render(
            canvas = Canvas(bitmap),
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat(),
            annotations = listOf(annotation),
            style = TEST_STYLE,
            trackerHighlights =
                mapOf(
                    TRACKER_ID to
                        ChartTrackerHighlight(
                            cells = setOf(ChartCell(0, 0)),
                            activeCell = ChartCell(0, 0),
                            counterAvailable = true,
                        ),
                ),
        )
        return bitmap
    }

    private fun Bitmap.nonTransparentPixels(): List<Pair<Int, Int>> =
        buildList {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (Color.alpha(getPixel(x, y)) > 0) add(x to y)
                }
            }
        }

    private fun annotation(
        kind: PatternAnnotationKind,
        payload: com.finnvek.knittools.domain.model.PatternAnnotationPayload,
        id: Long = 0L,
    ) = PatternAnnotation(
        id = id,
        layerId = 1L,
        page = 0,
        kind = kind,
        payload = payload,
        zIndex = 0L,
    )

    private companion object {
        const val TRACKER_ID = 99L
        val TEXT_BOUNDS = NormalizedPatternBounds(0.1f, 0.1f, 0.35f, 0.3f)
        val TEST_STYLE =
            PatternAnnotationRenderStyle(
                referencePageSize = 1_000f,
                highlighterDefaultAlpha = 96,
                calloutBackgroundAlpha = 0.16f,
                calloutTextSize = 24f,
                calloutCornerRadius = 12f,
                chartGridArgb = Color.DKGRAY,
                chartGridWidth = 2f,
                arrowHeadMultiplier = 4f,
                arrowHeadAngle = 0.55f,
                minimumPressureScale = 0.15f,
            )
    }
}

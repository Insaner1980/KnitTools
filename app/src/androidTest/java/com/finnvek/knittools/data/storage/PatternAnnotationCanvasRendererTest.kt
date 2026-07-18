package com.finnvek.knittools.data.storage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.domain.model.CalloutPayload
import com.finnvek.knittools.domain.model.ChartColumnDirection
import com.finnvek.knittools.domain.model.ChartRegionPayload
import com.finnvek.knittools.domain.model.ChartRowDirection
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
        val bitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888)
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
            )

        PatternAnnotationCanvasRenderer.render(
            canvas = Canvas(bitmap),
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat(),
            annotations = annotations,
            style = TEST_STYLE,
        )

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue(pixels.any { Color.alpha(it) > 0 })
        bitmap.recycle()
    }

    private fun annotation(
        kind: PatternAnnotationKind,
        payload: com.finnvek.knittools.domain.model.PatternAnnotationPayload,
    ) = PatternAnnotation(
        layerId = 1L,
        page = 0,
        kind = kind,
        payload = payload,
        zIndex = 0L,
    )

    private companion object {
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

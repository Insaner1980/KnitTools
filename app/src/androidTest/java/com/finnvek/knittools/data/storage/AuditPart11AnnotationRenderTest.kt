package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.domain.calculator.PatternPageCoordinateTransform
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.math.abs

/** Pixel probes use a synthetic PDF and the same renderer as the viewer; no user PDF is opened. */
class AuditPart11AnnotationRenderTest {
    @Test
    fun penHighlighterAndErasedStateAgreeAcrossCanvasScaleAndPdfExport() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            val directory = File(context.cacheDir, "audit11-render-${UUID.randomUUID()}").apply { mkdirs() }
            try {
                val source = File(directory, "source.pdf")
                writeTestPdf(source)
                val original = source.readBytes()
                val pen = stroke(1, PatternAnnotationKind.FREEHAND, 0.25f, Color.BLACK)
                val highlighter = stroke(2, PatternAnnotationKind.HIGHLIGHTER, 0.5f, Color.YELLOW)
                val erased = stroke(3, PatternAnnotationKind.FREEHAND, 0.75f, Color.RED)
                val exporter =
                    PatternPdfExporter.createForTest(context, Dispatchers.IO, cacheSpaceProvider = { Long.MAX_VALUE })
                for (annotations in listOf(listOf(pen, highlighter, erased), listOf(pen, highlighter))) {
                    val destination = File(directory, "export-${annotations.size}.pdf")
                    exporter.export(source.toUri(), destination.toUri(), annotations, emptyMap(), style()) {}
                    PdfPageRenderer(context, destination.toUri()).use { renderer ->
                        val exported = renderer.renderPage(0, targetWidth = 600, maxBitmapDimension = 800)
                        val canvasBitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
                        try {
                            val canvas = Canvas(canvasBitmap)
                            canvas.drawColor(Color.WHITE)
                            // A page zoom of 2 and translation of 40,60 is applied once to the canvas.
                            canvas.translate(40f, 60f)
                            canvas.scale(2f, 2f)
                            PatternAnnotationCanvasRenderer.render(canvas, 300f, 400f, annotations, style())
                            val transform = PatternPageCoordinateTransform(0f, 0f, 300f, 400f, 2f, 40f, 60f)
                            for (y in listOf(0.25f, 0.5f, 0.75f)) {
                                val point = requireNotNull(transform.pageToScreen(NormalizedPatternPoint(0.5f, y)))
                                val canvasPixel = canvasBitmap.getPixel(point.x.toInt(), point.y.toInt())
                                val pdfPixel = exported.getPixel(exported.width / 2, (exported.height * y).toInt())
                                assertColorNear(canvasPixel, pdfPixel)
                                val expected =
                                    when {
                                        y == 0.25f -> Color.BLACK
                                        y == 0.5f -> Color.rgb(255, 255, 159)
                                        annotations.size == 3 -> Color.RED
                                        else -> Color.WHITE
                                    }
                                assertColorNear(expected, pdfPixel)
                            }
                            assertEquals(Color.WHITE, exported.getPixel(10, 10))
                        } finally {
                            exported.recycle()
                            canvasBitmap.recycle()
                        }
                    }
                    assertArrayEquals(original, source.readBytes())
                }
            } finally {
                directory.deleteRecursively()
            }
        }

    private fun stroke(
        id: Long,
        kind: PatternAnnotationKind,
        y: Float,
        color: Int,
    ) = PatternAnnotation(
        id = id,
        layerId = 1,
        page = 0,
        kind = kind,
        payload =
            FreehandPayload(
                listOf(NormalizedPatternPoint(0.2f, y), NormalizedPatternPoint(0.8f, y)),
                argb = color,
                strokeWidth = 20f,
                pressureEnabled = false,
            ),
        zIndex = id,
    )

    private fun assertColorNear(
        expected: Int,
        actual: Int,
    ) {
        for (shift in listOf(0, 8, 16, 24)) {
            assertTrue(
                "Pixel mismatch: $expected / $actual",
                abs((expected shr shift and 255) - (actual shr shift and 255)) <= 2,
            )
        }
    }

    private fun style() =
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

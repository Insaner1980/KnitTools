package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class AuditPart12AnnotationRenderTest {
    @Test
    fun fourShapeToolsKeepGeometryAcrossTranslatedCanvasAndRasterExport() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val directory = File(context.cacheDir, "audit12-shapes-${UUID.randomUUID()}").apply { mkdirs() }
            try {
                val source = File(directory, "source.pdf")
                writeTestPdf(source)
                val original = source.readBytes()
                val style = PatternAnnotationRenderStyle(1000f, 96, 0.16f, 24f, 12f, Color.DKGRAY, 2f, 4f, 0.55f, 0.15f)
                val exporter =
                    PatternPdfExporter.createForTest(
                        context,
                        Dispatchers.IO,
                        cacheSpaceProvider = { Long.MAX_VALUE },
                    )
                for (kind in listOf(
                    PatternAnnotationKind.LINE,
                    PatternAnnotationKind.ARROW,
                    PatternAnnotationKind.RECTANGLE,
                    PatternAnnotationKind.ELLIPSE,
                )) {
                    val annotation =
                        PatternAnnotation(
                            id = 1,
                            layerId = 1,
                            page = 0,
                            kind = kind,
                            zIndex = 1,
                            payload =
                                ShapePayload(
                                    NormalizedPatternPoint(0.2f, 0.3f),
                                    NormalizedPatternPoint(0.8f, 0.7f),
                                    Color.BLACK,
                                    20f,
                                ),
                        )
                    val output = File(directory, "$kind.pdf")
                    exporter.export(source.toUri(), output.toUri(), listOf(annotation), emptyMap(), style) {}
                    PdfPageRenderer(context, output.toUri()).use { renderer ->
                        val raster = renderer.renderPage(0, 600, 800)
                        val screen = Bitmap.createBitmap(700, 900, Bitmap.Config.ARGB_8888)
                        try {
                            val canvas = Canvas(screen)
                            canvas.drawColor(Color.WHITE)
                            canvas.translate(20f, 30f)
                            canvas.scale(2f, 2f)
                            PatternAnnotationCanvasRenderer.render(canvas, 300f, 400f, listOf(annotation), style)
                            val probes =
                                if (kind == PatternAnnotationKind.LINE || kind == PatternAnnotationKind.ARROW) {
                                    listOf(0.5f to 0.5f)
                                } else {
                                    listOf(0.2f to 0.5f, 0.8f to 0.5f, 0.5f to 0.3f, 0.5f to 0.7f)
                                }
                            for ((x, y) in probes) {
                                val px = (x * 600).toInt()
                                val py = (y * 800).toInt()
                                assertTrue("$kind PDF geometry", Color.red(raster.getPixel(px, py)) < 30)
                                assertTrue("$kind canvas geometry", Color.red(screen.getPixel(px + 20, py + 30)) < 30)
                            }
                            assertEquals(Color.WHITE, raster.getPixel(10, 10))
                            assertEquals(Color.WHITE, screen.getPixel(30, 40))
                        } finally {
                            raster.recycle()
                            screen.recycle()
                        }
                    }
                    assertArrayEquals(original, source.readBytes())
                }
            } finally {
                directory.deleteRecursively()
            }
        }
}

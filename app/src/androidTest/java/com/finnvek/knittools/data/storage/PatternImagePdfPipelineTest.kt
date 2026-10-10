package com.finnvek.knittools.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.ui.screens.pattern.PatternImageImportLimits
import com.finnvek.knittools.ui.screens.pattern.PatternImageSelection
import com.finnvek.knittools.ui.screens.pattern.StagedPatternPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.Base64
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PatternImagePdfPipelineTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val projectId = System.currentTimeMillis()
    private val storage = PatternDocumentStorage()
    private val fixtureDir = File(context.cacheDir, "pattern-image-test-${UUID.randomUUID()}").apply { mkdirs() }

    @After
    fun cleanUp() {
        fixtureDir.deleteRecursively()
        File(context.filesDir, "pattern_pdfs/$projectId").deleteRecursively()
    }

    // CPD-OFF: PDF-renderointifixture pidetaan sivujarjestystestin yhteydessa.
    @Test
    fun multiPagePdfPreservesVisiblePageOrder() {
        val red = imageFile("red.png", 40, 20, Color.RED)
        val blue = imageFile("blue.png", 20, 40, Color.BLUE)
        val pages = listOf(page(red, "red"), page(blue, "blue"))

        val result = runBlocking { storage.convertImagesToPdf(context, projectId, pages, "ordered.pdf") { _, _ -> } }

        val resultUri = result.first.toUri()
        val pdfFile = File(resultUri.path ?: "")
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                assertEquals(2, renderer.pageCount)
                assertColorNear(Color.RED, renderedCenter(renderer, 0))
                assertColorNear(Color.BLUE, renderedCenter(renderer, 1))
            }
        }
    }

    // CPD-ON

    @Test
    fun transparentInputIsPaintedOnWhitePdfPage() {
        val transparent = File(fixtureDir, "transparent.png")
        Bitmap.createBitmap(30, 20, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.setPixel(15, 10, Color.RED)
            transparent.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }

        val result =
            runBlocking {
                storage.convertImagesToPdf(context, projectId, listOf(page(transparent, "transparent")), "white.pdf") {
                    _,
                    _,
                    ->
                }
            }
        val resultUri = result.first.toUri()
        val pdfFile = File(resultUri.path ?: "")

        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(0).use { pdfPage ->
                    val rendered = Bitmap.createBitmap(pdfPage.width, pdfPage.height, Bitmap.Config.ARGB_8888)
                    pdfPage.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    assertColorNear(Color.WHITE, rendered.getPixel(1, 1))
                    rendered.recycle()
                }
            }
        }
    }

    @Test
    fun decoderBoundsLongEdgeAndAppliesExifOrientation() {
        val large = imageFile("large.jpg", 3600, 1800, Color.GREEN, Bitmap.CompressFormat.JPEG)
        val rotated = imageFile("rotated.jpg", 80, 40, Color.MAGENTA, Bitmap.CompressFormat.JPEG)
        ExifInterface(rotated.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val largeInfo = storage.inspectStagedImage(large)
        val rotatedInfo = storage.inspectStagedImage(rotated)

        assertEquals(PatternImageImportLimits.MAX_LONG_EDGE_PIXELS, maxOf(largeInfo.width, largeInfo.height))
        assertEquals(40, rotatedInfo.width)
        assertEquals(80, rotatedInfo.height)
    }

    @Test
    fun animatedInputIsRejectedBeforePdfCreation() {
        val animated = File(fixtureDir, "animated.gif")
        animated.writeBytes(Base64.getDecoder().decode(TWO_FRAME_GIF_BASE64))

        val failure =
            assertThrows(PatternImageValidationException::class.java) {
                storage.inspectStagedImage(animated)
            }

        assertEquals(PatternImageFailureReason.ANIMATED, failure.reason)
    }

    @Test
    fun cameraCaptureUsesTheSameActualByteLimitAsGalleryImport() {
        val oversized = File(fixtureDir, "oversized-camera.jpg")
        RandomAccessFile(oversized, "rw").use { file ->
            file.setLength(PatternImageImportLimits.MAX_BYTES_PER_IMAGE + 1L)
        }

        val failure =
            assertThrows(PatternImageStageException::class.java) {
                storage.inspectCameraCapture(oversized)
            }

        assertEquals(PatternImageStageFailure.IMAGE_TOO_LARGE, failure.reason)
    }

    @Test
    fun selectedImagesAreCopiedImmediatelyIntoOwnedSessionInPickerOrder() {
        val first = imageFile("first.png", 30, 20, Color.RED)
        val second = imageFile("second.png", 20, 30, Color.BLUE)

        val result =
            runBlocking {
                storage.stageSelectedImages(
                    context = context,
                    projectId = projectId,
                    sessionId = "session-a",
                    existingSelection = PatternImageSelection(),
                    sourceUris = listOf(first.toUri(), second.toUri()),
                )
            }

        assertEquals(
            listOf(first.toUri().toString(), second.toUri().toString()),
            result.pages.map { it.sourceUri },
        )
        result.pages.forEach { staged ->
            assertTrue(File(staged.stagedPath).isFile)
            assertTrue(File(staged.stagedPath).canonicalPath.startsWith(context.filesDir.canonicalPath))
        }
    }

    @Test
    fun invalidPickerBatchDoesNotLeavePartiallyStagedPages() {
        val valid = imageFile("valid.png", 30, 20, Color.RED)
        val invalid = File(fixtureDir, "invalid.jpg").apply { writeText("not an image") }
        val sessionId = "session-invalid"

        assertThrows(PatternImageStageException::class.java) {
            runBlocking {
                storage.stageSelectedImages(
                    context = context,
                    projectId = projectId,
                    sessionId = sessionId,
                    existingSelection = PatternImageSelection(),
                    sourceUris = listOf(valid.toUri(), invalid.toUri()),
                )
            }
        }

        val sessionDirectory = PatternImageStagingFiles.sessionDirectory(context.filesDir, projectId, sessionId)
        assertFalse(sessionDirectory.exists())
    }

    @Test
    fun cancellationBeforePublishLeavesNoPartialPdf() {
        val first = imageFile("first.png", 30, 20, Color.RED)
        val second = imageFile("second.png", 20, 30, Color.BLUE)

        assertThrows(CancellationException::class.java) {
            runBlocking {
                storage.convertImagesToPdf(
                    context = context,
                    projectId = projectId,
                    pages = listOf(page(first, "first"), page(second, "second")),
                    fileName = "cancelled.pdf",
                ) { current, _ ->
                    if (current == 1) throw CancellationException("test cancellation")
                }
            }
        }

        val outputDirectory = File(context.filesDir, "pattern_pdfs/$projectId")
        assertFalse(outputDirectory.listFiles()?.any { it.extension == "pdf" } == true)
    }

    @Test
    fun existingOutputNamePublishesToCollisionSafePdf() {
        val source = imageFile("page.png", 30, 20, Color.GREEN)
        val pages = listOf(page(source, "page"))

        val first = runBlocking { storage.convertImagesToPdf(context, projectId, pages, "same.pdf") { _, _ -> } }
        val second = runBlocking { storage.convertImagesToPdf(context, projectId, pages, "same.pdf") { _, _ -> } }

        assertEquals("same.pdf", first.second)
        assertEquals("same-1.pdf", second.second)
        assertTrue(File(first.first.toUri().path ?: "").isFile)
        assertTrue(File(second.first.toUri().path ?: "").isFile)
    }

    private fun page(
        file: File,
        id: String,
    ): StagedPatternPage {
        val info = storage.inspectStagedImage(file)
        return StagedPatternPage(
            id = id,
            sourceUri = "content://fixture/$id",
            stagedPath = file.absolutePath,
            byteCount = file.length(),
            width = info.width,
            height = info.height,
        )
    }

    private fun imageFile(
        name: String,
        width: Int,
        height: Int,
        color: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    ): File =
        File(fixtureDir, name).also { file ->
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(color)
                file.outputStream().use { bitmap.compress(format, 95, it) }
                bitmap.recycle()
            }
        }

    private fun renderedCenter(
        renderer: PdfRenderer,
        pageIndex: Int,
    ): Int =
        renderer.openPage(pageIndex).use { page ->
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            bitmap.recycle()
            color
        }

    private fun assertColorNear(
        expected: Int,
        actual: Int,
    ) {
        assertTrue(kotlin.math.abs(Color.red(expected) - Color.red(actual)) <= 8)
        assertTrue(kotlin.math.abs(Color.green(expected) - Color.green(actual)) <= 8)
        assertTrue(kotlin.math.abs(Color.blue(expected) - Color.blue(actual)) <= 8)
    }

    private companion object {
        const val TWO_FRAME_GIF_BASE64 =
            "R0lGODlhAgABAIEAAP8AAAAAAAAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAAACwAAAAAAgABAAAIBQABAAgIACH5BAEKAAEALAAAAAACAAEAgQAA/wAAAAAAAAAAAAgFAAEACAgAOw=="
    }
}

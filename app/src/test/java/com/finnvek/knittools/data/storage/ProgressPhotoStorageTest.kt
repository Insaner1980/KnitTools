package com.finnvek.knittools.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.io.path.createTempFile

class ProgressPhotoStorageTest {
    @Test
    fun `deletePhoto removes only a file owned by the requested project`() {
        val filesDir = Files.createTempDirectory("knittools-progress-root").toFile()
        val ownedFile = filesDir.resolve("progress_photos/7/owned.jpg")
        val otherProjectFile = filesDir.resolve("progress_photos/8/other.jpg")
        val outsideFile = filesDir.resolve("pattern_captures/7/outside.jpg")
        listOf(ownedFile, otherProjectFile, outsideFile).forEach { file ->
            checkNotNull(file.parentFile).mkdirs()
            file.writeText("photo")
        }
        val context = mockk<Context>()
        every { context.filesDir } returns filesDir
        val storage = ProgressPhotoStorage()

        withParsedFileUri(ownedFile) { ownedUri ->
            storage.deletePhoto(context, 7L, ownedUri)
        }
        withParsedFileUri(otherProjectFile) { otherUri ->
            storage.deletePhoto(context, 7L, otherUri)
        }
        withParsedFileUri(outsideFile) { outsideUri ->
            storage.deletePhoto(context, 7L, outsideUri)
        }

        assertFalse(ownedFile.exists())
        assertTrue(otherProjectFile.exists())
        assertTrue(outsideFile.exists())
    }

    @Test
    fun `compressAndSave keeps scaled dimensions positive for extreme aspect ratios`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val sourceUri = mockk<Uri>()
        val original = mockk<Bitmap>()
        val scaled = mockk<Bitmap>()
        val targetFile = createTempFile(suffix = ".jpg").toFile()

        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(byteArrayOf(1))
        every { original.width } returns 1
        every { original.height } returns 4_000
        every { original.recycle() } just runs
        every { scaled.compress(Bitmap.CompressFormat.JPEG, any(), any()) } returns true
        every { scaled.recycle() } just runs

        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)
        try {
            every { BitmapFactory.decodeStream(any()) } returns original
            every { Bitmap.createScaledBitmap(original, any(), any(), true) } answers {
                require(secondArg<Int>() > 0) { "Scaled width must be positive" }
                require(thirdArg<Int>() > 0) { "Scaled height must be positive" }
                scaled
            }

            assertTrue(ProgressPhotoStorage().compressAndSave(context, sourceUri, targetFile))
        } finally {
            unmockkStatic(Bitmap::class)
            unmockkStatic(BitmapFactory::class)
            targetFile.delete()
        }
    }

    @Test
    fun `compressAndSave reports failure when jpeg compression fails`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val sourceUri = mockk<Uri>()
        val bitmap = mockk<Bitmap>()
        val targetFile =
            createTempFile(suffix = ".jpg")
                .toFile()

        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(byteArrayOf(1))
        every { bitmap.width } returns 64
        every { bitmap.height } returns 64
        every { bitmap.compress(Bitmap.CompressFormat.JPEG, any(), any()) } returns false
        every { bitmap.recycle() } just runs

        mockkStatic(BitmapFactory::class)
        try {
            every { BitmapFactory.decodeStream(any()) } returns bitmap

            val saved = ProgressPhotoStorage().compressAndSave(context, sourceUri, targetFile)

            assertFalse(saved)
        } finally {
            unmockkStatic(BitmapFactory::class)
            targetFile.delete()
        }
    }

    private inline fun withParsedFileUri(
        file: java.io.File,
        block: (String) -> Unit,
    ) {
        val uriString = file.toURI().toString()
        val uri = mockk<Uri>()
        mockkStatic(Uri::class)
        try {
            every { Uri.parse(uriString) } returns uri
            every { uri.scheme } returns "file"
            every { uri.path } returns file.absolutePath
            block(uriString)
        } finally {
            unmockkStatic(Uri::class)
        }
    }
}

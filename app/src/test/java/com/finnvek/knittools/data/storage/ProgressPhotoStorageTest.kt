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
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.io.path.createTempFile

class ProgressPhotoStorageTest {
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
}

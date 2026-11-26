package com.finnvek.knittools.data.backup

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class ContentResolverBackupProviderIoTest {
    @get:Rule val temporary = TemporaryFolder()

    private val resolver = mockk<ContentResolver>()
    private val context = mockk<Context> { every { contentResolver } returns resolver }
    private val uri = mockk<Uri> { every { scheme } returns ContentResolver.SCHEME_CONTENT }

    @Test
    fun missingReadAssetCanBeRetriedWithoutOpeningTheRawFile() =
        runBlocking {
            val source = temporary.newFile().apply { writeText("selected bytes") }
            val target = temporary.newFile()
            val descriptor = mockk<ParcelFileDescriptor>(relaxUnitFun = true)
            val asset =
                mockk<AssetFileDescriptor> {
                    every { parcelFileDescriptor } returns descriptor
                    every { createInputStream() } answers { source.inputStream() }
                }
            every { resolver.openAssetFileDescriptor(uri, "r", any()) } returns null andThen asset
            val io = ContentResolverBackupProviderIo(context)

            val failure = runCatching { io.read(uri, target, source.length()) }.exceptionOrNull()
            assertTrue(failure is IOException)
            assertEquals(0L, target.length())

            io.read(uri, target, source.length())

            assertArrayEquals(source.readBytes(), target.readBytes())
            verify(exactly = 0) { resolver.openFileDescriptor(any(), any(), any()) }
            verify(exactly = 1) { descriptor.checkError() }
            verify(exactly = 1) { descriptor.close() }
        }

    @Test
    fun missingWriteDescriptorPreservesSourceArchive() =
        runBlocking {
            val source = temporary.newFile().apply { writeText("archive bytes") }
            every { resolver.openFileDescriptor(uri, "wt", any()) } returns null
            val io = ContentResolverBackupProviderIo(context)

            val failure = runCatching { io.write(source, uri, source.length()) }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertEquals("archive bytes", source.readText())
            verify(exactly = 0) { resolver.openAssetFileDescriptor(any(), any(), any()) }
        }
}

package com.finnvek.knittools.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class AuditPart16ProviderAliasTest {
    @Test
    fun aliasedProviderDestinationCannotTruncatePrivateArchive() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName.endsWith(".activepolicytest"))
            val source =
                File(
                    context.cacheDir,
                    "audit16-private-archive.zip",
                ).apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val before = source.readBytes()
            var opened = false
            val endpoint =
                BackupProviderEndpoint { _, _ ->
                    object : BackupProviderRequest {
                        override fun open(): BackupProviderHandle {
                            opened = true
                            val output = FileOutputStream(source)
                            return object : BackupProviderHandle {
                                override fun inputStream(): InputStream = FileInputStream(source)

                                override fun outputStream(): OutputStream = output

                                override fun close() = output.close()
                            }
                        }

                        override fun cancelOpen() = Unit

                        override fun closeDescriptor() = Unit
                    }
                }
            try {
                val failure =
                    runCatching {
                        ContentResolverBackupProviderIo(endpoint, 5_000).write(source, source.toUri(), 100)
                    }.exceptionOrNull()
                assertArrayEquals(before, source.readBytes())
                assertNotNull(failure)
                assertFalse("Reject before opening the provider in truncating mode", opened)
            } finally {
                source.delete()
            }
        }
}

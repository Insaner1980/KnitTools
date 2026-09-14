package com.finnvek.knittools.repository

import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteFullException
import android.net.Uri
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.local.KnitToolsDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryFailureTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun failedReadsAndInvalidArchivesLeaveNoPreviewOrTemporaryFiles() =
        runTest {
            val root = temporary.newFolder()
            val resolver = mockk<ContentResolver>()
            val context =
                mockk<Context> {
                    every { noBackupFilesDir } returns root
                    every { contentResolver } returns resolver
                }
            val database = mockk<KnitToolsDatabase>()
            val repository =
                BackupRepository(
                    context,
                    database,
                    PatternFileReferenceCoordinator(),
                    mockk(),
                    StandardTestDispatcher(testScheduler),
                    backgroundScope,
                )
            val uri = mockk<Uri>()
            val selection = UUID.randomUUID()
            every { resolver.openInputStream(uri) } returns null
            assertPrepareError(BackupError.READ, repository, uri, selection)
            every { resolver.openInputStream(uri) } answers { "invalid zip".byteInputStream() }
            assertPrepareError(BackupError.INVALID, repository, uri, selection)
            every { resolver.openInputStream(uri) } throws IllegalStateException("provider unavailable")
            assertPrepareError(BackupError.READ, repository, uri, selection)
            assertTrue(File(root, "manual-backup").listFiles().orEmpty().isEmpty())
            assertEquals(
                BackupError.RESTORE,
                (
                    runCatching {
                        repository.restore(selection)
                    }.exceptionOrNull() as BackupException
                ).error,
            )
            repository.cancelPreview(null)
            repository.cancelPreview(selection)
            repository.releasePreview(null)
            repository.releasePreview(selection)
            runCurrent()
            repository.recoverInterruptedOperations()
            verify(exactly = 0) { database.openHelper }
        }

    @Test fun preparationPreservesCancellationAndClassifiedFailures() =
        runTest {
            val root = temporary.newFolder()
            val resolver = mockk<ContentResolver>()
            val context =
                mockk<Context> {
                    every { noBackupFilesDir } returns root
                    every { contentResolver } returns resolver
                }
            val repository =
                BackupRepository(
                    context,
                    mockk(),
                    PatternFileReferenceCoordinator(),
                    mockk(),
                    StandardTestDispatcher(testScheduler),
                    backgroundScope,
                )
            val uri = mockk<Uri>()
            for (failure in listOf(CancellationException("cancel"), BackupException(BackupError.SPACE))) {
                every { resolver.openInputStream(uri) } throws failure
                val actual = runCatching { repository.prepare(uri, UUID.randomUUID()) }.exceptionOrNull()
                if (failure is CancellationException) {
                    assertTrue(actual is CancellationException)
                    assertEquals(failure.message, actual?.message)
                } else {
                    assertSame(failure, actual)
                }
                assertTrue(File(root, "manual-backup").listFiles().orEmpty().isEmpty())
            }
            every { resolver.openInputStream(uri) } throws mockk<SQLiteFullException>(relaxed = true)
            assertEquals(
                BackupError.SPACE,
                (
                    runCatching {
                        repository.prepare(uri, UUID.randomUUID())
                    }.exceptionOrNull() as BackupException
                ).error,
            )
        }

    private suspend fun assertPrepareError(
        expected: BackupError,
        repository: BackupRepository,
        uri: Uri,
        selection: UUID,
    ) {
        val exception = runCatching { repository.prepare(uri, selection) }.exceptionOrNull() as BackupException
        assertEquals(expected, exception.error)
    }
}

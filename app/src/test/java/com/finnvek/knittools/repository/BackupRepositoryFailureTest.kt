package com.finnvek.knittools.repository

import android.content.Context
import android.database.sqlite.SQLiteFullException
import android.net.Uri
import com.finnvek.knittools.data.backup.BackupError
import com.finnvek.knittools.data.backup.BackupException
import com.finnvek.knittools.data.backup.BackupProviderIo
import com.finnvek.knittools.data.local.KnitToolsDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
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
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryFailureTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun failedReadsAndInvalidArchivesLeaveNoPreviewOrTemporaryFiles() =
        runTest {
            val root = temporary.newFolder()
            val context =
                mockk<Context> {
                    every { noBackupFilesDir } returns root
                }
            val database = mockk<KnitToolsDatabase>()
            val providerIo = FakeProviderIo()
            val repository =
                BackupRepository(
                    context,
                    database,
                    PatternFileReferenceCoordinator(),
                    mockk(),
                    StandardTestDispatcher(testScheduler),
                    backgroundScope,
                    providerIo,
                )
            val uri = mockk<Uri>()
            val selection = UUID.randomUUID()
            providerIo.readAction = { _, _ -> throw IOException("provider no-progress timeout") }
            assertPrepareError(BackupError.READ, repository, uri, selection)
            providerIo.readAction = { target, _ -> target.writeText("invalid zip") }
            assertPrepareError(BackupError.INVALID, repository, uri, selection)
            providerIo.readAction = { _, _ -> throw IllegalStateException("provider unavailable") }
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
            val context =
                mockk<Context> {
                    every { noBackupFilesDir } returns root
                }
            val providerIo = FakeProviderIo()
            val repository =
                BackupRepository(
                    context,
                    mockk(),
                    PatternFileReferenceCoordinator(),
                    mockk(),
                    StandardTestDispatcher(testScheduler),
                    backgroundScope,
                    providerIo,
                )
            val uri = mockk<Uri>()
            for (failure in listOf(CancellationException("cancel"), BackupException(BackupError.SPACE))) {
                providerIo.readAction = { _, _ -> throw failure }
                val actual = runCatching { repository.prepare(uri, UUID.randomUUID()) }.exceptionOrNull()
                if (failure is CancellationException) {
                    assertTrue(actual is CancellationException)
                    assertEquals(failure.message, actual?.message)
                } else {
                    assertSame(failure, actual)
                }
                assertTrue(File(root, "manual-backup").listFiles().orEmpty().isEmpty())
            }
            providerIo.readAction = { _, _ -> throw mockk<SQLiteFullException>(relaxed = true) }
            assertEquals(
                BackupError.SPACE,
                (
                    runCatching {
                        repository.prepare(uri, UUID.randomUUID())
                    }.exceptionOrNull() as BackupException
                ).error,
            )
        }

    @Test fun stalledProviderDoesNotHoldOperationMutexAndCancellationCleansStaging() =
        runTest {
            val root = temporary.newFolder()
            val context = mockk<Context> { every { noBackupFilesDir } returns root }
            val providerIo = FakeProviderIo()
            val started = CompletableDeferred<Unit>()
            providerIo.readAction = { _, _ ->
                started.complete(Unit)
                awaitCancellation()
            }
            val repository =
                BackupRepository(
                    context,
                    mockk(),
                    PatternFileReferenceCoordinator(),
                    mockk(),
                    StandardTestDispatcher(testScheduler),
                    backgroundScope,
                    providerIo,
                )
            val selection = UUID.randomUUID()
            val preparation = backgroundScope.async { repository.prepare(mockk(), selection) }
            runCurrent()
            started.await()

            val cancellation = backgroundScope.async { repository.cancelPreview(selection) }
            runCurrent()

            assertTrue(cancellation.isCompleted)
            preparation.cancelAndJoin()
            runCurrent()
            assertTrue(File(root, "manual-backup").listFiles().orEmpty().isEmpty())
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

    private class FakeProviderIo : BackupProviderIo {
        var readAction: suspend (File, () -> Unit) -> Unit = { _, _ -> }

        override suspend fun read(
            source: Uri,
            target: File,
            maxBytes: Long,
            check: () -> Unit,
        ) = readAction(target, check)

        override suspend fun write(
            source: File,
            destination: Uri,
            maxBytes: Long,
            check: () -> Unit,
        ) = error("Unexpected write")
    }
}

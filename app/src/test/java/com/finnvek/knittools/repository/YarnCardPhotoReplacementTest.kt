package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.storage.YarnPhotoStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class YarnCardPhotoReplacementTest {
    @Test
    fun `cancellation during copy handoff removes the unpublished replacement`() =
        runTest {
            val filesDir = Files.createTempDirectory("knittools-photo-copy-handoff").toFile()
            try {
                val copy = File(filesDir, "yarn_photos/5/new.jpg")
                val old = File(filesDir, "yarn_photos/5/old.jpg")
                old.parentFile!!.mkdirs()
                old.writeText("old")
                val copiedUri = "file:///new.jpg"
                val sourceUri = mockk<Uri>()
                val context = mockk<Context>()
                every { context.filesDir } returns filesDir
                val dao = mockk<YarnCardDao>()
                coEvery { dao.getCard(5L) } returns YarnCardEntity(id = 5L, photoUri = "file:///old.jpg")
                val storage = mockk<YarnPhotoStorage>()
                lateinit var request: Job
                every { storage.copyPhoto(context, 5L, sourceUri) } answers {
                    copy.writeText("new")
                    request.cancel()
                    copiedUri
                }
                val repository =
                    YarnCardRepository(
                        dao,
                        mockk(),
                        context,
                        ImmediateDatabaseTransactionRunner,
                        StandardTestDispatcher(testScheduler),
                        storage,
                    )

                withParsedFileUri(copiedUri, copy.absolutePath) {
                    request = launch { repository.updatePhotoUri(5L, sourceUri) }
                    request.join()
                }

                assertTrue(request.isCancelled)
                assertEquals("old", old.readText())
                assertFalse(copy.exists())
                coVerify(exactly = 0) { dao.updatePhotoUri(any(), any()) }
            } finally {
                filesDir.deleteRecursively()
            }
        }

    @Test
    fun `cancelled photo result preserves a copy already published by the database`() =
        verifyCancelledPhotoResult(published = true)

    @Test
    fun `cancelled unpublished photo is removed`() = verifyCancelledPhotoResult(published = false)

    @Test
    fun `cancelled photo result preserves the copy when publication cannot be checked`() =
        verifyCancelledPhotoResult(published = true, failReadback = true)

    private fun verifyCancelledPhotoResult(
        published: Boolean,
        failReadback: Boolean = false,
    ) = runTest {
        val yarnDao = mockk<YarnCardDao>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val storage = mockk<YarnPhotoStorage>(relaxed = true)
        val sourceUri = mockk<Uri>()
        val filesDir = Files.createTempDirectory("knittools-photo-cancellation").toFile()
        val copiedFile =
            File(filesDir, "yarn_photos/5/published.jpg").apply {
                parentFile?.mkdirs()
                writeText("published")
            }
        val copiedUri = "file:///published.jpg"
        var stored = YarnCardEntity(id = 5L, photoUri = "")
        val committed = CompletableDeferred<Unit>()
        var failure: Throwable? = null
        every { context.filesDir } returns filesDir
        coEvery { yarnDao.getCard(5L) } coAnswers {
            if (failReadback && committed.isCompleted) error("Synthetic readback failure")
            stored
        }
        every { storage.copyPhoto(context, 5L, sourceUri) } returns copiedUri
        coEvery { yarnDao.updatePhotoUri(5L, copiedUri) } coAnswers {
            if (published) stored = stored.copy(photoUri = copiedUri)
            committed.complete(Unit)
            awaitCancellation()
        }
        val repository =
            YarnCardRepository(
                dao = yarnDao,
                counterProjectDao = mockk(relaxed = true),
                context = context,
                transactionRunner = ImmediateDatabaseTransactionRunner,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                yarnPhotoStorage = storage,
            )

        try {
            withParsedFileUri(copiedUri, copiedFile.absolutePath) {
                val job =
                    launch {
                        try {
                            repository.updatePhotoUri(5L, sourceUri)
                        } catch (cancelled: CancellationException) {
                            failure = cancelled
                            throw cancelled
                        }
                    }
                committed.await()
                job.cancel()
                job.join()
            }
            assertTrue(failure is CancellationException)
            assertEquals(if (published) copiedUri else "", stored.photoUri)
            assertEquals("Only an unpublished photo can be deleted", published || failReadback, copiedFile.exists())
        } finally {
            filesDir.deleteRecursively()
        }
    }

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `yarn card photo update deletes copied photo when database save throws`() =
        runTest {
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val storage = mockk<YarnPhotoStorage>(relaxed = true)
            val sourceUri = mockk<Uri>()
            // CPD-ON
            val filesDir = Files.createTempDirectory("knittools-files").toFile()
            val databaseFailure = IllegalStateException("database failed")
            val copiedFile =
                File(filesDir, "yarn_photos/5/new-yarn-photo.jpg").apply {
                    parentFile?.mkdirs()
                    writeText("new")
                }
            val copiedPhotoUri = "file:///new-yarn-photo.jpg"
            every { context.filesDir } returns filesDir
            coEvery { yarnDao.getCard(5L) } returns
                YarnCardEntity(
                    id = 5L,
                    photoUri = "",
                )
            every { storage.copyPhoto(context, 5L, sourceUri) } returns copiedPhotoUri
            coEvery { yarnDao.updatePhotoUri(5L, copiedPhotoUri) } throws databaseFailure
            val repository =
                YarnCardRepository(
                    dao = yarnDao,
                    counterProjectDao = projectDao,
                    context = context,
                    transactionRunner = ImmediateDatabaseTransactionRunner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                    yarnPhotoStorage = storage,
                )

            var thrown: Throwable? = null
            withParsedFileUri(copiedPhotoUri, copiedFile.absolutePath) {
                thrown = runCatching { repository.updatePhotoUri(5L, sourceUri) }.exceptionOrNull()
            }

            assertEquals(databaseFailure, thrown)
            assertFalse(copiedFile.exists())
        }

    @Test
    fun `yarn card photo cleanup prunes files not referenced by database`() =
        runTest {
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val storage = mockk<YarnPhotoStorage>(relaxed = true)
            val referencedPhotoUri = "file:///kept-yarn-photo.jpg"
            every { yarnDao.getAllCards() } returns
                flowOf(
                    listOf(
                        YarnCardEntity(
                            id = 5L,
                            photoUri = referencedPhotoUri,
                        ),
                        YarnCardEntity(
                            id = 6L,
                            photoUri = "",
                        ),
                    ),
                )
            every { storage.pruneUnreferencedPhotos(context, setOf(referencedPhotoUri)) } returns Unit
            val repository =
                YarnCardRepository(
                    dao = yarnDao,
                    counterProjectDao = projectDao,
                    context = context,
                    transactionRunner = ImmediateDatabaseTransactionRunner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                    yarnPhotoStorage = storage,
                )

            repository.pruneUnreferencedPhotoFiles()

            verify { storage.pruneUnreferencedPhotos(context, setOf(referencedPhotoUri)) }
        }

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `startup photo cleanup waits for an in-flight photo replacement`() =
        runTest {
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val storage = mockk<YarnPhotoStorage>(relaxed = true)
            val sourceUri = mockk<Uri>()
            val copiedPhotoUri = "file:///new-yarn-photo.jpg"
            val oldPhotoUri = "file:///old-yarn-photo.jpg"
            val filesDir = Files.createTempDirectory("knittools-files").toFile()
            val oldPhoto =
                File(filesDir, "yarn_photos/5/old-yarn-photo.jpg").apply {
                    parentFile?.mkdirs()
                    writeText("old")
                }
            val updateStarted = CompletableDeferred<Unit>()
            val allowUpdate = CompletableDeferred<Unit>()
            var pruneCalled = false
            val dispatcher = StandardTestDispatcher(testScheduler)
            every { context.filesDir } returns filesDir
            coEvery { yarnDao.getCard(5L) } returns YarnCardEntity(id = 5L, photoUri = oldPhotoUri)
            every { storage.copyPhoto(context, 5L, sourceUri) } returns copiedPhotoUri
            coEvery { yarnDao.updatePhotoUri(5L, copiedPhotoUri) } coAnswers {
                updateStarted.complete(Unit)
                allowUpdate.await()
                1
            }
            every { yarnDao.getAllCards() } returns flowOf(emptyList())
            every { storage.pruneUnreferencedPhotos(context, emptySet()) } answers {
                pruneCalled = true
            }
            val repository =
                YarnCardRepository(
                    dao = yarnDao,
                    counterProjectDao = projectDao,
                    context = context,
                    transactionRunner = ImmediateDatabaseTransactionRunner,
                    ioDispatcher = dispatcher,
                    yarnPhotoStorage = storage,
                )

            withParsedFileUri(oldPhotoUri, oldPhoto.absolutePath) {
                val updateJob = launch(dispatcher) { repository.updatePhotoUri(5L, sourceUri) }
                runCurrent()
                updateStarted.await()
                val pruneJob = launch(dispatcher) { repository.pruneUnreferencedPhotoFiles() }
                runCurrent()

                assertFalse(pruneCalled)
                allowUpdate.complete(Unit)
                advanceUntilIdle()
                updateJob.join()
                pruneJob.join()
                assertTrue(pruneCalled)
            }
        }
    // CPD-ON

    @Test
    fun `yarn photo storage prunes orphaned files while keeping referenced photos`() =
        runTest {
            val context = mockk<Context>(relaxed = true)
            val filesDir = Files.createTempDirectory("knittools-files").toFile()
            val keptPhoto =
                File(filesDir, "yarn_photos/5/kept.jpg").apply {
                    parentFile?.mkdirs()
                    writeText("kept")
                }
            val orphanForSameCard =
                File(filesDir, "yarn_photos/5/orphan.jpg").apply {
                    writeText("orphan")
                }
            val orphanForOtherCard =
                File(filesDir, "yarn_photos/6/orphan.jpg").apply {
                    parentFile?.mkdirs()
                    writeText("orphan")
                }
            val progressPhoto =
                File(filesDir, "progress_photos/5/photo.jpg").apply {
                    parentFile?.mkdirs()
                    writeText("progress")
                }
            val keptPhotoUri = "file:///kept-yarn-photo.jpg"
            every { context.filesDir } returns filesDir

            withParsedFileUri(keptPhotoUri, keptPhoto.absolutePath) {
                YarnPhotoStorage().pruneUnreferencedPhotos(context, setOf(keptPhotoUri))
            }

            assertTrue(keptPhoto.exists())
            assertFalse(orphanForSameCard.exists())
            assertFalse(orphanForOtherCard.exists())
            assertTrue(progressPhoto.exists())
        }
}

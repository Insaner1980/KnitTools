package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.storage.YarnPhotoStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `yarn card photo update deletes copied photo when database save throws`() =
        runTest {
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val storage = mockk<YarnPhotoStorage>(relaxed = true)
            val sourceUri = mockk<Uri>()
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

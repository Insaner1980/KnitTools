package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.ProgressPhoto
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTransactionBoundaryTest {
    @Test
    fun `yarn card project link updates both stores inside one transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            coEvery { yarnDao.getCard(5L) } returns YarnCardEntity(id = 5L, yarnName = "Sock")
            coEvery { projectDao.getAllProjectsOnce() } returns
                listOf(CounterProjectEntity(id = 7L, yarnCardIds = "1,2"))
            val repository =
                YarnCardRepository(
                    yarnDao,
                    projectDao,
                    context,
                    runner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            repository.updateLinkedProjectId(5L, 7L)

            assertEquals(1, runner.runCount)
            coVerifyOrder {
                projectDao.updateYarnCardIds(7L, "1,2,5", any())
                yarnDao.updateLinkedProjectId(5L, 7L)
            }
        }

    @Test
    fun `saved pattern delete clears project links and rows inside one transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val patternDao = mockk<SavedPatternDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            coEvery { patternDao.getByIds(listOf(4L)) } returns
                listOf(SavedPatternEntity(id = 4L, ravelryId = 4, name = "Pattern", designerName = "Designer"))
            val repository =
                SavedPatternRepository(
                    patternDao,
                    context,
                    projectDao,
                    runner,
                    UnconfinedTestDispatcher(testScheduler),
                )

            repository.deleteByIds(listOf(4L))

            assertEquals(1, runner.runCount)
            coVerifyOrder {
                projectDao.clearLinkedPatternIds(listOf(4L), any())
                patternDao.deleteByIds(listOf(4L))
            }
        }

    @Test
    fun `project delete commits database cleanup before deleting photo files`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val events = mutableListOf<String>()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)
            val photoStorage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            coEvery { yarnRepository.clearLinkedProject(7L) } coAnswers {
                events += "clear-yarn"
            }
            coEvery { projectDao.delete(7L) } coAnswers {
                events += "delete-project"
            }
            every { photoStorage.deleteProjectPhotos(context, 7L) } answers {
                events += "delete-files"
            }
            val repository =
                CounterRepository(
                    dao = projectDao,
                    sessionDao = sessionDao,
                    photoStorage = photoStorage,
                    context = context,
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = savedPatternRepository,
                    patternAnnotationRepository = annotationRepository,
                    transactionRunner = runner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            repository.deleteProject(7L)

            assertEquals(1, runner.runCount)
            assertEquals(listOf("clear-yarn", "delete-project", "delete-files"), events)
        }

    @Test
    fun `project delete dispatches photo file cleanup to IO dispatcher`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val photoStorage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val repository =
                CounterRepository(
                    dao = projectDao,
                    sessionDao = sessionDao,
                    photoStorage = photoStorage,
                    context = context,
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = mockk(relaxed = true),
                    patternAnnotationRepository = mockk(relaxed = true),
                    transactionRunner = RecordingTransactionRunner(),
                    ioDispatcher = ioDispatcher,
                )

            repository.deleteProject(7L)

            assertEquals(1, ioDispatcher.dispatchCount)
        }

    @Test
    fun `pattern attachment saves related database state inside one transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val annotationRepository = mockk<PatternAnnotationRepository>(relaxed = true)
            val repository =
                CounterRepository(
                    dao = projectDao,
                    sessionDao = sessionDao,
                    photoStorage = mockk(relaxed = true),
                    context = mockk(relaxed = true),
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = savedPatternRepository,
                    patternAnnotationRepository = annotationRepository,
                    transactionRunner = runner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            repository.attachPattern(7L, "content://pattern", "Pattern", 0, null)

            assertEquals(1, runner.runCount)
            coVerifyOrder {
                savedPatternRepository.saveImportedPatternIfMissing("content://pattern", "Pattern")
                annotationRepository.clearProject(7L)
                projectDao.updatePattern(
                    id = 7L,
                    patternUri = "content://pattern",
                    patternName = "Pattern",
                    currentPatternPage = 0,
                    patternRowMapping = null,
                    updatedAt = any(),
                )
            }
        }

    @Test
    fun `yarn card delete dispatches app owned photo cleanup to IO dispatcher`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val runner = RecordingTransactionRunner()
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val photoUri = "content://com.finnvek.knittools.fileprovider/yarn.jpg"
            every { context.packageName } returns "com.finnvek.knittools"
            every { context.contentResolver.delete(any(), null, null) } returns 1
            coEvery { yarnDao.getCards(listOf(5L)) } returns
                listOf(
                    YarnCardEntity(
                        id = 5L,
                        photoUri = photoUri,
                    ),
                )
            coEvery { projectDao.getAllProjectsOnce() } returns emptyList()
            withParsedAppUri(photoUri) {
                val repository =
                    YarnCardRepository(
                        dao = yarnDao,
                        counterProjectDao = projectDao,
                        context = context,
                        transactionRunner = runner,
                        ioDispatcher = ioDispatcher,
                    )

                repository.deleteCards(listOf(5L))
            }

            assertEquals(1, ioDispatcher.dispatchCount)
        }

    @Test
    fun `saved pattern delete dispatches unused local file cleanup to IO dispatcher`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val runner = RecordingTransactionRunner()
            val patternDao = mockk<SavedPatternDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val patternUri = "content://com.finnvek.knittools.fileprovider/pattern.pdf"
            every { context.packageName } returns "com.finnvek.knittools"
            every { context.contentResolver.delete(any(), null, null) } returns 1
            coEvery { patternDao.getByIds(listOf(4L)) } returns
                listOf(
                    SavedPatternEntity(
                        id = 4L,
                        ravelryId = 4,
                        name = "Pattern",
                        designerName = "Designer",
                        patternUrl = patternUri,
                    ),
                )
            coEvery { patternDao.getByPatternUrl(patternUri) } returns null
            coEvery { projectDao.countProjectsUsingPatternUri(patternUri) } returns 0
            withParsedAppUri(patternUri) {
                val repository =
                    SavedPatternRepository(
                        dao = patternDao,
                        context = context,
                        counterProjectDao = projectDao,
                        transactionRunner = runner,
                        ioDispatcher = ioDispatcher,
                    )

                repository.deleteByIds(listOf(4L))
            }

            assertEquals(1, ioDispatcher.dispatchCount)
        }

    @Test
    fun `progress photo delete removes database row before deleting file`() =
        runTest {
            val events = mutableListOf<String>()
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            coEvery { dao.delete(3L) } coAnswers {
                events += "delete-row"
            }
            every { storage.deletePhoto("file:///photo.jpg") } answers {
                events += "delete-file"
            }
            val repository =
                ProgressPhotoRepository(
                    dao = dao,
                    storage = storage,
                    context = mockk(relaxed = true),
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            repository.deletePhoto(
                ProgressPhoto(
                    id = 3L,
                    projectId = 7L,
                    photoUri = "file:///photo.jpg",
                    rowNumber = 12,
                ),
            )

            assertEquals(listOf("delete-row", "delete-file"), events)
        }

    @Test
    fun `ravelry project creation saves pattern and project inside one transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            coEvery { savedPatternRepository.save(any()) } returns 12L
            val repository =
                RavelryRepository(
                    api = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    counterProjectDao = projectDao,
                    transactionRunner = runner,
                )

            repository.createProjectFromPattern(PatternDetail(id = 99, name = "Cardigan", permalink = "cardigan"))

            assertEquals(1, runner.runCount)
            coVerifyOrder {
                savedPatternRepository.save(any())
                projectDao.insert(match { it.name == "Cardigan" && it.linkedPatternId == 12L })
            }
        }

    private class RecordingTransactionRunner : DatabaseTransactionRunner {
        var runCount: Int = 0

        override suspend fun <T> run(block: suspend () -> T): T {
            runCount += 1
            return block()
        }
    }

    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatchCount: Int = 0

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount += 1
            block.run()
        }
    }

    private suspend inline fun withParsedAppUri(
        uriString: String,
        block: suspend () -> Unit,
    ) {
        mockkStatic(Uri::class)
        try {
            val uri = mockk<Uri>(relaxed = true)
            every { Uri.parse(uriString) } returns uri
            every { uri.scheme } returns "content"
            every { uri.authority } returns "com.finnvek.knittools.fileprovider"
            block()
        } finally {
            unmockkStatic(Uri::class)
        }
    }
}

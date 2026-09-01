package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.local.YarnCardDao
import com.finnvek.knittools.data.local.YarnCardEntity
import com.finnvek.knittools.data.remote.PatternDetail
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.data.storage.YarnPhotoStorage
import com.finnvek.knittools.domain.model.PatternAvailability
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
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
                listOf(
                    SavedPatternEntity(
                        id = 4L,
                        source = SavedPatternSource.Ravelry.persistedValue,
                        ravelryPatternId = 4,
                        name = "Pattern",
                        designerName = "Designer",
                    ),
                )
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
    fun `project delete commits database cleanup before removing files`() =
        runTest {
            val runner = RecordingTransactionRunner()
            // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
            val events = mutableListOf<String>()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val projectDocumentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            val photoStorage = mockk<ProgressPhotoStorage>(relaxed = true)
            // CPD-ON
            val patternDocumentStorage = mockk<PatternDocumentStorage>(relaxed = true)
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
            every { patternDocumentStorage.deleteProjectCaptureImages(context, 7L) } answers {
                events += "delete-captures"
            }
            coEvery { projectDocumentRepository.getDistinctUris(7L) } coAnswers {
                events += "read-document-uris"
                listOf("file:///pattern-a.pdf", "file:///pattern-b.pdf")
            }
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L, patternUri = null)
            coEvery { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) } coAnswers {
                events += "cleanup-pattern"
            }
            val repository =
                CounterRepository(
                    dao = projectDao,
                    projectCounterDao = mockk(relaxed = true),
                    sessionDao = mockk(relaxed = true),
                    photoStorage = photoStorage,
                    patternDocumentStorage = patternDocumentStorage,
                    context = context,
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = savedPatternRepository,
                    projectDocumentRepository = projectDocumentRepository,
                    projectFolderDao = mockk(relaxed = true),
                    transactionRunner = runner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            repository.deleteProject(7L)

            assertEquals(1, runner.runCount)
            assertEquals(
                listOf(
                    "read-document-uris",
                    "clear-yarn",
                    "delete-project",
                    "delete-files",
                    "delete-captures",
                    "cleanup-pattern",
                    "cleanup-pattern",
                ),
                events,
            )
        }

    @Test
    fun `project delete dispatches photo file cleanup to IO dispatcher`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val photoStorage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val repository =
                CounterRepository(
                    dao = projectDao,
                    projectCounterDao = mockk(relaxed = true),
                    sessionDao = sessionDao,
                    photoStorage = photoStorage,
                    patternDocumentStorage = mockk(relaxed = true),
                    context = context,
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = mockk(relaxed = true),
                    projectDocumentRepository = mockk(relaxed = true),
                    projectFolderDao = mockk(relaxed = true),
                    transactionRunner = RecordingTransactionRunner(),
                    ioDispatcher = ioDispatcher,
                    // CPD-ON
                )

            repository.deleteProject(7L)

            assertEquals(1, ioDispatcher.dispatchCount)
        }

    @Test
    fun `project delete keeps committed database deletion when photo directory cleanup fails`() =
        runTest {
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val photoStorage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            every { photoStorage.deleteProjectPhotos(context, 7L) } throws IOException("delete failed")
            val repository =
                CounterRepository(
                    dao = projectDao,
                    projectCounterDao = mockk(relaxed = true),
                    sessionDao = sessionDao,
                    photoStorage = photoStorage,
                    patternDocumentStorage = mockk(relaxed = true),
                    context = context,
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = mockk(relaxed = true),
                    projectDocumentRepository = mockk(relaxed = true),
                    projectFolderDao = mockk(relaxed = true),
                    transactionRunner = RecordingTransactionRunner(),
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            val thrown = runCatching { repository.deleteProject(7L) }.exceptionOrNull()

            assertEquals(null, thrown)
            coVerify(exactly = 1) { yarnRepository.clearLinkedProject(7L) }
            coVerify(exactly = 1) { projectDao.delete(7L) }
        }

    @Test
    fun `pattern attachment saves related database state inside one transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            val repository =
                CounterRepository(
                    dao = projectDao,
                    projectCounterDao = mockk(relaxed = true),
                    sessionDao = sessionDao,
                    photoStorage = mockk(relaxed = true),
                    patternDocumentStorage = mockk(relaxed = true),
                    context = mockk(relaxed = true),
                    yarnCardRepository = yarnRepository,
                    savedPatternRepository = savedPatternRepository,
                    projectDocumentRepository = documentRepository,
                    projectFolderDao = mockk(relaxed = true),
                    transactionRunner = runner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            val document = projectDocument(isPrimary = true)
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)
            coEvery { documentRepository.addImportedPdf(7L, "content://pattern", "Pattern") } returns
                ProjectDocumentMutationResult.Added(document)
            repository.attachPattern(7L, "content://pattern", "Pattern", 0, null)

            assertEquals(1, runner.runCount)
            coVerifyOrder {
                documentRepository.addImportedPdf(7L, "content://pattern", "Pattern")
                projectDao.updatePatternInformation(
                    id = 7L,
                    linkedPatternId = document.savedPatternId,
                    patternName = "Pattern",
                    updatedAt = any(),
                )
            }
        }

    @Test
    fun `saved pattern attachment links existing saved pattern inside one transaction`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val sessionDao = mockk<SessionDao>(relaxed = true)
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            coEvery { projectDao.getProject(7L) } returns CounterProjectEntity(id = 7L)
            coEvery { savedPatternRepository.getById(12L) } returns
                SavedPattern(
                    id = 12L,
                    source = SavedPatternSource.Ravelry,
                    name = "Cardigan",
                    designerName = "Designer",
                    localPdfUri = null,
                )
            val repository =
                CounterRepository(
                    dao = projectDao,
                    projectCounterDao = mockk(relaxed = true),
                    sessionDao = sessionDao,
                    photoStorage = mockk(relaxed = true),
                    patternDocumentStorage = mockk(relaxed = true),
                    context = mockk(relaxed = true),
                    yarnCardRepository = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    projectDocumentRepository = mockk(relaxed = true),
                    projectFolderDao = mockk(relaxed = true),
                    transactionRunner = runner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            val attachedPattern = repository.attachSavedPattern(7L, 12L)

            assertEquals(12L, attachedPattern?.id)
            assertEquals(1, runner.runCount)
            coVerifyOrder {
                projectDao.getProject(7L)
                savedPatternRepository.getById(12L)
                projectDao.updatePatternInformation(
                    id = 7L,
                    linkedPatternId = 12L,
                    patternName = "Cardigan",
                    updatedAt = any(),
                )
            }
        }

    @Test
    fun `pattern detachment delegates primary removal to document repository`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
            val document = projectDocument(isPrimary = true)
            coEvery { documentRepository.getPrimary(7L) } returns document
            val repository =
                CounterRepository(
                    dao = projectDao,
                    projectCounterDao = mockk(relaxed = true),
                    sessionDao = mockk(relaxed = true),
                    photoStorage = mockk(relaxed = true),
                    patternDocumentStorage = mockk(relaxed = true),
                    context = mockk(relaxed = true),
                    yarnCardRepository = mockk(relaxed = true),
                    savedPatternRepository = mockk(relaxed = true),
                    projectDocumentRepository = documentRepository,
                    projectFolderDao = mockk(relaxed = true),
                    transactionRunner = runner,
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            repository.detachPattern(7L)

            assertEquals(0, runner.runCount)
            coVerifyOrder {
                documentRepository.getPrimary(7L)
                documentRepository.remove(7L, document.id)
            }
        }

    private fun projectDocument(isPrimary: Boolean): ProjectDocument =
        ProjectDocument(
            id = 21L,
            projectId = 7L,
            savedPatternId = 11L,
            documentKey = "saved-pattern:11",
            label = "Pattern",
            localPdfUri = "content://pattern",
            sortOrder = 0,
            isPrimary = isPrimary,
            currentPage = 0,
            rowMapping = null,
            readingLineEnabled = false,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = 1L,
            updatedAt = 1L,
        )

    // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
    @Test
    fun `yarn card delete dispatches app owned photo cleanup to IO dispatcher`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val runner = RecordingTransactionRunner()
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val photoUri = "content://com.finnvek.knittools.fileprovider/yarn_photos/yarn.jpg"
            every { context.packageName } returns "com.finnvek.knittools"
            every { context.filesDir } returns
                Files
                    .createTempDirectory("knittools-files")
                    .toFile()
            every { context.contentResolver.delete(any(), null, null) } returns 1
            coEvery { yarnDao.getCards(listOf(5L)) } returns
                // CPD-ON
                listOf(
                    YarnCardEntity(
                        id = 5L,
                        photoUri = photoUri,
                    ),
                )
            coEvery { projectDao.getAllProjectsOnce() } returns emptyList()
            withParsedAppUri(photoUri, listOf("yarn_photos", "yarn.jpg")) {
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
    fun `yarn card delete cleans app owned photos when caller is cancelled after database delete`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val runner = CancellingTransactionRunner()
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val filesDir =
                Files
                    .createTempDirectory("knittools-files")
                    .toFile()
            val photoFile = filesDir.resolve("yarn_photos/5/yarn.jpg")
            val photoUri = photoFile.toURI().toString()
            checkNotNull(photoFile.parentFile).mkdirs()
            photoFile.writeText("photo")
            every { context.filesDir } returns filesDir
            coEvery { yarnDao.getCards(listOf(5L)) } returns
                listOf(
                    YarnCardEntity(
                        id = 5L,
                        photoUri = photoUri,
                    ),
                )
            coEvery { projectDao.getAllProjectsOnce() } returns emptyList()

            withParsedFileUri(photoUri, photoFile.absolutePath) {
                val repository =
                    YarnCardRepository(
                        dao = yarnDao,
                        counterProjectDao = projectDao,
                        context = context,
                        transactionRunner = runner,
                        ioDispatcher = ioDispatcher,
                    )

                val deleteJob =
                    launch {
                        repository.deleteCards(listOf(5L))
                    }
                deleteJob.join()
            }

            assertFalse(photoFile.exists())
            assertEquals(1, ioDispatcher.dispatchCount)
        }

    @Test
    fun `yarn card photo update copies selected photo before saving uri`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val runner = RecordingTransactionRunner()
            // CPD-OFF: Testin skenaariokohtainen asetelma pidetaan paikallisena ja luettavana.
            val yarnDao = mockk<YarnCardDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val storage = mockk<YarnPhotoStorage>(relaxed = true)
            val sourceUri = mockk<Uri>()
            // CPD-ON
            coEvery { yarnDao.getCard(5L) } returns
                YarnCardEntity(
                    id = 5L,
                    photoUri = "",
                )
            every { storage.copyPhoto(context, 5L, sourceUri) } returns "file:///new-yarn-photo.jpg"
            coEvery { yarnDao.updatePhotoUri(5L, "file:///new-yarn-photo.jpg") } returns 1
            val repository =
                YarnCardRepository(
                    dao = yarnDao,
                    counterProjectDao = projectDao,
                    context = context,
                    transactionRunner = runner,
                    ioDispatcher = ioDispatcher,
                    yarnPhotoStorage = storage,
                )

            val updated = repository.updatePhotoUri(5L, sourceUri)

            assertTrue(updated)
            verify { storage.copyPhoto(context, 5L, sourceUri) }
            coVerify { yarnDao.updatePhotoUri(5L, "file:///new-yarn-photo.jpg") }
            assertEquals(2, ioDispatcher.dispatchCount)
        }

    @Test
    fun `saved pattern delete dispatches unused local file cleanup to IO dispatcher`() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            val runner = RecordingTransactionRunner()
            val patternDao = mockk<SavedPatternDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val patternUri = "content://com.finnvek.knittools.fileprovider/patterns/4/pdf/pattern.pdf"
            every { context.packageName } returns "com.finnvek.knittools"
            every { context.filesDir } returns
                Files
                    .createTempDirectory("knittools-files")
                    .toFile()
            every { context.contentResolver.delete(any(), null, null) } returns 1
            coEvery { patternDao.getByIds(listOf(4L)) } returns
                listOf(
                    SavedPatternEntity(
                        id = 4L,
                        source = SavedPatternSource.LocalFile.persistedValue,
                        ravelryPatternId = null,
                        name = "Pattern",
                        designerName = "Designer",
                        originalUrl = patternUri,
                        localPdfUri = patternUri,
                        isAvailableOffline = true,
                    ),
                )
            coEvery { patternDao.getByLocalPdfUri(patternUri) } returns null
            coEvery { projectDao.countProjectsUsingPatternUri(patternUri) } returns 0
            withParsedAppUri(patternUri, listOf("patterns", "4", "pdf", "pattern.pdf")) {
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

            assertEquals(2, ioDispatcher.dispatchCount)
        }

    @Test
    fun `saved pattern repository deletes unused local pattern file`() =
        runTest {
            val runner = RecordingTransactionRunner()
            val patternDao = mockk<SavedPatternDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val file =
                Files
                    .createTempFile("pattern", ".pdf")
                    .toFile()
            val patternUri = "file://${file.absolutePath.replace('\\', '/')}"
            every { context.filesDir } returns file.parentFile
            coEvery { patternDao.getByLocalPdfUri(patternUri) } returns null
            coEvery { projectDao.countProjectsUsingPatternUri(patternUri) } returns 0
            withParsedFileUri(patternUri, file.absolutePath) {
                val repository =
                    SavedPatternRepository(
                        dao = patternDao,
                        context = context,
                        counterProjectDao = projectDao,
                        transactionRunner = runner,
                        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                    )

                repository.deleteLocalPatternFileIfUnused(patternUri)
            }

            assertFalse(file.exists())
        }

    @Test
    fun `progress photo delete removes database row before file`() =
        runTest {
            val events = mutableListOf<String>()
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            coEvery { dao.getByIds(listOf(3L)) } returns
                listOf(ProgressPhotoEntity(id = 3L, projectId = 7L, photoUri = "file:///photo.jpg", rowNumber = 12))
            coEvery { dao.delete(3L) } coAnswers {
                events += "delete-row"
            }
            every { storage.deletePhoto(any(), 7L, "file:///photo.jpg") } answers {
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
    fun `progress photo delete keeps row deleted when file cleanup fails`() =
        runTest {
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            coEvery { dao.getByIds(listOf(3L)) } returns
                listOf(ProgressPhotoEntity(id = 3L, projectId = 7L, photoUri = "file:///photo.jpg", rowNumber = 12))
            every { storage.deletePhoto(any(), 7L, "file:///photo.jpg") } throws IOException("delete failed")
            val repository =
                ProgressPhotoRepository(
                    dao = dao,
                    storage = storage,
                    context = mockk(relaxed = true),
                    ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                )

            val thrown =
                runCatching {
                    repository.deletePhoto(
                        ProgressPhoto(
                            id = 3L,
                            projectId = 7L,
                            photoUri = "file:///photo.jpg",
                            rowNumber = 12,
                        ),
                    )
                }.exceptionOrNull()

            assertEquals(null, thrown)
            coVerify(exactly = 1) { dao.delete(3L) }
        }

    @Test
    fun `progress photo save deletes target file when compression throws`() =
        runTest {
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val sourceUri = mockk<Uri>()
            val targetFile = java.io.File("photo.jpg")
            val targetUri = mockk<Uri>()
            mockkStatic(Uri::class)
            try {
                every { Uri.fromFile(targetFile) } returns targetUri
                every { targetUri.toString() } returns "file:///photo.jpg"
                every { storage.createPhotoFile(context, 7L) } returns (targetFile to mockk<Uri>())
                every { storage.compressAndSave(context, sourceUri, targetFile) } throws IOException("write failed")
                val repository =
                    ProgressPhotoRepository(
                        dao = dao,
                        storage = storage,
                        context = context,
                        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                    )

                val thrown =
                    runCatching {
                        repository.savePhoto(7L, sourceUri, rowNumber = 12)
                    }.exceptionOrNull()

                assertTrue(thrown is IOException)
                verify { storage.deletePhoto(context, 7L, "file:///photo.jpg") }
            } finally {
                unmockkStatic(Uri::class)
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RavelryRepositoryTransactionBoundaryTest {
    @Test
    fun `ravelry project creation delegates to the atomic project writer`() =
        runTest {
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val counterRepository = mockk<CounterRepository>()
            coEvery {
                counterRepository.createProject(
                    name = "Cardigan",
                    canCreateAdditionalProjects = false,
                    linkedPattern = any(),
                )
            } returns ProjectCreationResult.Created(7L)
            val repository =
                RavelryRepository(
                    api = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    counterRepository = counterRepository,
                )

            val result =
                repository.createProjectFromPattern(
                    detail = PatternDetail(id = 99, name = "Cardigan", permalink = "cardigan"),
                    canCreateAdditionalProjects = false,
                )

            assertEquals(ProjectCreationResult.Created(7L), result)
            coVerify(exactly = 1) {
                counterRepository.createProject(
                    name = "Cardigan",
                    canCreateAdditionalProjects = false,
                    linkedPattern = match { it.ravelryPatternId == 99 && it.name == "Cardigan" },
                )
            }
        }

    @Test
    fun `ravelry save preserves backend canonical and original urls`() =
        runTest {
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            coEvery { savedPatternRepository.saveRavelryPatternIfMissing(any()) } returns 12L
            val repository =
                RavelryRepository(
                    api = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    counterRepository = mockk(relaxed = true),
                )

            repository.savePattern(
                PatternDetail(
                    id = 99,
                    name = "Cardigan",
                    permalink = "cardigan",
                    availability = PatternAvailability.Paid,
                    canonicalUrl = "https://www.ravelry.com/patterns/library/cardigan",
                    originalUrl = "https://www.ravelry.com/patterns/library/cardigan?utm_source=share",
                ),
            )

            coVerify {
                savedPatternRepository.saveRavelryPatternIfMissing(
                    match {
                        it.ravelryPatternId == 99 &&
                            it.availability == PatternAvailability.Paid &&
                            it.canonicalUrl == "https://www.ravelry.com/patterns/library/cardigan" &&
                            it.originalUrl == "https://www.ravelry.com/patterns/library/cardigan?utm_source=share"
                    },
                )
            }
        }

    @Test
    fun `ravelry save preserves every backend availability state`() =
        runTest {
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            coEvery { savedPatternRepository.saveRavelryPatternIfMissing(any()) } returns 12L
            val repository =
                RavelryRepository(
                    api = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    counterRepository = mockk(relaxed = true),
                )

            PatternAvailability.entries.forEachIndexed { index, availability ->
                repository.savePattern(
                    PatternDetail(
                        id = index + 1,
                        name = availability.persistedValue,
                        availability = availability,
                    ),
                )
            }

            PatternAvailability.entries.forEach { availability ->
                coVerify(exactly = 1) {
                    savedPatternRepository.saveRavelryPatternIfMissing(
                        match { it.availability == availability },
                    )
                }
            }
        }

    @Test
    fun `ravelry saved pattern multi delete delegates batch ids`() =
        runTest {
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val repository =
                RavelryRepository(
                    api = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    counterRepository = mockk(relaxed = true),
                )

            repository.deleteSavedPatterns(listOf(4L, 5L))

            coVerify(exactly = 1) { savedPatternRepository.deleteByIds(listOf(4L, 5L)) }
        }

    @Test
    fun `ravelry project creation preserves the backend urls for the atomic writer`() =
        runTest {
            val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
            val counterRepository = mockk<CounterRepository>(relaxed = true)
            val repository =
                RavelryRepository(
                    api = mockk(relaxed = true),
                    savedPatternRepository = savedPatternRepository,
                    counterRepository = counterRepository,
                )

            repository.createProjectFromPattern(
                detail =
                    PatternDetail(
                        id = 99,
                        name = "Cardigan",
                        permalink = "cardigan",
                        availability = PatternAvailability.Unknown,
                        canonicalUrl = "https://www.ravelry.com/patterns/library/cardigan",
                        originalUrl = "https://example.com/cardigan",
                    ),
                canCreateAdditionalProjects = true,
            )

            coVerify {
                counterRepository.createProject(
                    name = "Cardigan",
                    canCreateAdditionalProjects = true,
                    linkedPattern =
                        match {
                            it.canonicalUrl == "https://www.ravelry.com/patterns/library/cardigan" &&
                                it.originalUrl == "https://example.com/cardigan" &&
                                it.availability == PatternAvailability.Unknown
                        },
                )
            }
        }
}

private class RecordingTransactionRunner : DatabaseTransactionRunner {
    var runCount: Int = 0

    override suspend fun <T> run(block: suspend () -> T): T {
        runCount += 1
        return block()
    }
}

private class CancellingTransactionRunner : DatabaseTransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T {
        val result = block()
        currentCoroutineContext()[Job]?.cancel()
        return result
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

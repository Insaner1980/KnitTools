package com.finnvek.knittools.repository

import android.content.Context
import com.finnvek.knittools.data.local.ActiveSessionEntity
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.ProgressPhotoDao
import com.finnvek.knittools.data.local.ProgressPhotoEntity
import com.finnvek.knittools.data.local.ProjectCounterDao
import com.finnvek.knittools.data.local.ProjectDocumentDao
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.SessionDao
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.ProgressPhoto
import com.finnvek.knittools.domain.model.SavedPatternSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class DeletionCleanupOrderingTest {
    @Test
    fun `ordinary project deletion commits before every physical cleanup`() =
        runTest {
            val fixture = projectFixture()

            fixture.repository.deleteProject(PROJECT_ID)

            fixture.assertDatabaseDeletePrecedesCleanup()
            coVerify(exactly = 1) { fixture.savedPatternRepository.deleteLocalPatternFileIfUnused(PATTERN_A) }
            coVerify(exactly = 1) { fixture.savedPatternRepository.deleteLocalPatternFileIfUnused(PATTERN_B) }
        }

    @Test
    fun `ordinary project database failure leaves every physical file untouched`() =
        runTest {
            val fixture = projectFixture(databaseFailure = IOException("database failed"))

            val thrown = runCatching { fixture.repository.deleteProject(PROJECT_ID) }.exceptionOrNull()

            assertTrue(thrown is IOException)
            fixture.assertNoPhysicalCleanup()
        }

    @Test
    fun `ordinary project cancellation before commit leaves every physical file untouched`() =
        runTest {
            val fixture = projectFixture(databaseFailure = CancellationException("cancelled"))

            val thrown = runCatching { fixture.repository.deleteProject(PROJECT_ID) }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            fixture.assertNoPhysicalCleanup()
        }

    @Test
    fun `active session aware database failure leaves every physical file untouched`() =
        runTest {
            val fixture =
                projectFixture(
                    activeSession = activeSession(),
                    databaseFailure = IOException("database failed"),
                )

            val result = fixture.repository.deleteProjectResolvingActiveSession(PROJECT_ID, discardActiveSession = true)

            assertEquals(ProjectDeletionResult.PersistenceFailure, result)
            fixture.assertNoPhysicalCleanup()
        }

    @Test
    fun `active session aware successful deletion cleans files after commit`() =
        runTest {
            val fixture = projectFixture(activeSession = activeSession())

            val result = fixture.repository.deleteProjectResolvingActiveSession(PROJECT_ID, discardActiveSession = true)

            assertEquals(ProjectDeletionResult.Deleted, result)
            fixture.assertDatabaseDeletePrecedesCleanup()
        }

    @Test
    fun `project photo cleanup failure does not change committed deletion`() =
        runTest {
            assertCleanupFailureIsBestEffort(ProjectCleanupFailure.PROGRESS_PHOTOS)
        }

    @Test
    fun `project capture cleanup failure does not change committed deletion`() =
        runTest {
            assertCleanupFailureIsBestEffort(ProjectCleanupFailure.CAPTURES)
        }

    @Test
    fun `project pattern cleanup failure does not change committed deletion`() =
        runTest {
            assertCleanupFailureIsBestEffort(ProjectCleanupFailure.PATTERN_PDF)
        }

    @Test
    fun `individual photo deletion uses stored ownership and database first ordering`() =
        runTest {
            val events = mutableListOf<String>()
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            coEvery { dao.getByIds(listOf(PHOTO_ID)) } returns listOf(photoEntity())
            coEvery { dao.delete(PHOTO_ID) } answers { events += "delete-row" }
            every { storage.deletePhoto(context, PROJECT_ID, PHOTO_URI) } answers { events += "delete-file" }
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            repository.deletePhoto(photo())

            assertEquals(listOf("delete-row", "delete-file"), events)
        }

    @Test
    fun `individual photo database failure retains physical file`() =
        runTest {
            val fixture = progressPhotoFixture(databaseFailure = IOException("database failed"))

            val thrown = runCatching { fixture.repository.deletePhoto(photo()) }.exceptionOrNull()

            assertTrue(thrown is IOException)
            verify(exactly = 0) { fixture.storage.deletePhoto(any(), any(), any()) }
        }

    @Test
    fun `individual photo cancellation before commit retains physical file`() =
        runTest {
            val fixture = progressPhotoFixture(databaseFailure = CancellationException("cancelled"))

            val thrown = runCatching { fixture.repository.deletePhoto(photo()) }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            verify(exactly = 0) { fixture.storage.deletePhoto(any(), any(), any()) }
        }

    @Test
    fun `individual photo cleanup failure leaves database row deleted`() =
        runTest {
            val fixture = progressPhotoFixture(cleanupFailure = IOException("cleanup failed"))

            val thrown = runCatching { fixture.repository.deletePhoto(photo()) }.exceptionOrNull()

            assertEquals(null, thrown)
            coVerify(exactly = 1) { fixture.dao.delete(PHOTO_ID) }
        }

    @Test
    fun `individual photo from another project is rejected`() =
        runTest {
            val fixture = progressPhotoFixture()

            fixture.repository.deletePhoto(photo().copy(projectId = OTHER_PROJECT_ID))

            coVerify(exactly = 0) { fixture.dao.delete(PHOTO_ID) }
            verify(exactly = 0) { fixture.storage.deletePhoto(any(), any(), any()) }
        }

    @Test
    fun `bulk photo deletion commits all rows before physical cleanup`() =
        runTest {
            val events = mutableListOf<String>()
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val photos = listOf(photoEntity(), photoEntity(id = 4L, uri = SECOND_PHOTO_URI))
            coEvery { dao.getByIds(listOf(PHOTO_ID, 4L)) } returns photos
            coEvery { dao.deleteByIds(listOf(PHOTO_ID, 4L)) } answers { events += "delete-rows" }
            every { storage.deletePhoto(context, PROJECT_ID, PHOTO_URI) } answers { events += "delete-first-file" }
            every { storage.deletePhoto(context, PROJECT_ID, SECOND_PHOTO_URI) } answers {
                events += "delete-second-file"
            }
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            repository.deletePhotos(listOf(PHOTO_ID, 4L))

            assertEquals(listOf("delete-rows", "delete-first-file", "delete-second-file"), events)
        }

    @Test
    fun `bulk photo database failure leaves all physical files untouched`() =
        runTest {
            val dao = mockk<ProgressPhotoDao>(relaxed = true)
            val storage = mockk<ProgressPhotoStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            coEvery { dao.getByIds(listOf(PHOTO_ID, 4L)) } returns
                listOf(photoEntity(), photoEntity(id = 4L, uri = SECOND_PHOTO_URI))
            coEvery { dao.deleteByIds(listOf(PHOTO_ID, 4L)) } throws IOException("database failed")
            val repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher(testScheduler))

            val thrown = runCatching { repository.deletePhotos(listOf(PHOTO_ID, 4L)) }.exceptionOrNull()

            assertTrue(thrown is IOException)
            verify(exactly = 0) { storage.deletePhoto(any(), any(), any()) }
        }

    @Test
    fun `canonical pattern cleanup retains a PDF referenced by another project document`() =
        runTest {
            assertCanonicalPatternFileIsRetained(libraryReference = false, projectDocumentReference = true)
        }

    @Test
    fun `canonical pattern cleanup retains a PDF in the pattern library`() =
        runTest {
            assertCanonicalPatternFileIsRetained(libraryReference = true, projectDocumentReference = false)
        }

    private suspend fun assertCleanupFailureIsBestEffort(failure: ProjectCleanupFailure) {
        val fixture = projectFixture(activeSession = activeSession(), cleanupFailure = failure)

        val result = fixture.repository.deleteProjectResolvingActiveSession(PROJECT_ID, discardActiveSession = true)

        assertEquals(ProjectDeletionResult.Deleted, result)
        coVerify(exactly = 1) { fixture.projectDao.delete(PROJECT_ID) }
    }

    private suspend fun assertCanonicalPatternFileIsRetained(
        libraryReference: Boolean,
        projectDocumentReference: Boolean,
    ) {
        val filesDir = Files.createTempDirectory("knittools-pattern-retention").toFile()
        val patternFile =
            filesDir.resolve("pattern_pdfs/shared.pdf").apply {
                checkNotNull(parentFile).mkdirs()
                writeText("pdf")
            }
        val patternUri = patternFile.toURI().toString()
        val patternDao = mockk<SavedPatternDao>(relaxed = true)
        val projectDao = mockk<CounterProjectDao>(relaxed = true)
        val documentDao = mockk<ProjectDocumentDao>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        every { context.filesDir } returns filesDir
        coEvery { patternDao.getByLocalPdfUri(patternUri) } returns
            if (libraryReference) {
                SavedPatternEntity(
                    id = 9L,
                    source = SavedPatternSource.LocalFile.persistedValue,
                    name = "Shared pattern",
                    designerName = "Designer",
                    localPdfUri = patternUri,
                    isAvailableOffline = true,
                )
            } else {
                null
            }
        coEvery { projectDao.countProjectsUsingPatternUri(patternUri) } returns 0
        coEvery { documentDao.isUriReferenced(patternUri) } returns projectDocumentReference
        val repository =
            SavedPatternRepository(
                dao = patternDao,
                context = context,
                counterProjectDao = projectDao,
                transactionRunner = EventTransactionRunner(mutableListOf()),
                ioDispatcher = UnconfinedTestDispatcher(),
                projectDocumentDao = documentDao,
            )

        withParsedFileUri(patternUri, patternFile.absolutePath) {
            repository.deleteLocalPatternFileIfUnused(patternUri)
        }

        assertTrue(patternFile.exists())
    }

    private fun projectFixture(
        activeSession: ActiveSessionEntity? = null,
        databaseFailure: Throwable? = null,
        cleanupFailure: ProjectCleanupFailure? = null,
    ): ProjectDeletionFixture {
        val events = mutableListOf<String>()
        val projectDao = mockk<CounterProjectDao>(relaxed = true)
        val sessionDao = mockk<SessionDao>(relaxed = true)
        val photoStorage = mockk<ProgressPhotoStorage>(relaxed = true)
        val captureStorage = mockk<PatternDocumentStorage>(relaxed = true)
        val yarnRepository = mockk<YarnCardRepository>(relaxed = true)
        val savedPatternRepository = mockk<SavedPatternRepository>(relaxed = true)
        val documentRepository = mockk<ProjectDocumentRepository>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val runner = EventTransactionRunner(events)
        coEvery { sessionDao.getActiveSession() } returns activeSession
        coEvery { projectDao.getProject(PROJECT_ID) } returns
            CounterProjectEntity(id = PROJECT_ID, patternUri = PATTERN_A)
        coEvery { documentRepository.getDistinctUris(PROJECT_ID) } returns listOf(PATTERN_A, PATTERN_B, PATTERN_A)
        coEvery { yarnRepository.clearLinkedProject(PROJECT_ID) } answers { events += "clear-yarn" }
        coEvery { projectDao.delete(PROJECT_ID) } answers {
            events += "delete-project"
            databaseFailure?.let { throw it }
        }
        every { photoStorage.deleteProjectPhotos(context, PROJECT_ID) } answers {
            events += "delete-progress-photos"
            cleanupFailure.raiseIf(ProjectCleanupFailure.PROGRESS_PHOTOS, "photo cleanup failed")
        }
        every { captureStorage.deleteProjectCaptureImages(context, PROJECT_ID) } answers {
            events += "delete-captures"
            cleanupFailure.raiseIf(ProjectCleanupFailure.CAPTURES, "capture cleanup failed")
        }
        coEvery { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) } answers {
            val uri = firstArg<String>()
            events += "delete-pattern:$uri"
            cleanupFailure.raiseIf(ProjectCleanupFailure.PATTERN_PDF, "pattern cleanup failed")
        }
        val repository =
            CounterRepository(
                dao = projectDao,
                projectCounterDao = mockk<ProjectCounterDao>(relaxed = true),
                sessionDao = sessionDao,
                photoStorage = photoStorage,
                patternDocumentStorage = captureStorage,
                context = context,
                yarnCardRepository = yarnRepository,
                savedPatternRepository = savedPatternRepository,
                projectDocumentRepository = documentRepository,
                projectFolderDao = mockk(relaxed = true),
                transactionRunner = runner,
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        return ProjectDeletionFixture(
            repository = repository,
            projectDao = projectDao,
            photoStorage = photoStorage,
            captureStorage = captureStorage,
            savedPatternRepository = savedPatternRepository,
            context = context,
            events = events,
        )
    }

    private fun progressPhotoFixture(
        databaseFailure: Throwable? = null,
        cleanupFailure: Throwable? = null,
    ): ProgressPhotoFixture {
        val dao = mockk<ProgressPhotoDao>(relaxed = true)
        val storage = mockk<ProgressPhotoStorage>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        coEvery { dao.getByIds(listOf(PHOTO_ID)) } returns listOf(photoEntity())
        coEvery { dao.delete(PHOTO_ID) } answers { databaseFailure?.let { throw it } }
        every { storage.deletePhoto(context, PROJECT_ID, PHOTO_URI) } answers {
            cleanupFailure?.let { throw it }
        }
        return ProgressPhotoFixture(
            repository = ProgressPhotoRepository(dao, storage, context, UnconfinedTestDispatcher()),
            dao = dao,
            storage = storage,
        )
    }

    private fun photo() =
        ProgressPhoto(
            id = PHOTO_ID,
            projectId = PROJECT_ID,
            photoUri = PHOTO_URI,
            rowNumber = 12,
        )

    private fun photoEntity(
        id: Long = PHOTO_ID,
        uri: String = PHOTO_URI,
    ) = ProgressPhotoEntity(
        id = id,
        projectId = PROJECT_ID,
        photoUri = uri,
        rowNumber = 12,
    )

    private fun activeSession() =
        mockk<ActiveSessionEntity>(relaxed = true) {
            every { projectId } returns PROJECT_ID
            every { sessionToken } returns "session-token"
        }

    private data class ProjectDeletionFixture(
        val repository: CounterRepository,
        val projectDao: CounterProjectDao,
        val photoStorage: ProgressPhotoStorage,
        val captureStorage: PatternDocumentStorage,
        val savedPatternRepository: SavedPatternRepository,
        val context: Context,
        val events: List<String>,
    ) {
        fun assertDatabaseDeletePrecedesCleanup() {
            val databaseDelete = events.indexOf("delete-project")
            assertTrue(databaseDelete >= 0)
            listOf(
                "delete-progress-photos",
                "delete-captures",
                "delete-pattern:$PATTERN_A",
                "delete-pattern:$PATTERN_B",
            ).forEach { cleanup ->
                assertTrue(
                    "$cleanup must occur after database deletion",
                    events.indexOf(cleanup) > databaseDelete,
                )
            }
        }

        fun assertNoPhysicalCleanup() {
            verify(exactly = 0) { photoStorage.deleteProjectPhotos(context, PROJECT_ID) }
            verify(exactly = 0) { captureStorage.deleteProjectCaptureImages(context, PROJECT_ID) }
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }
        }
    }

    private data class ProgressPhotoFixture(
        val repository: ProgressPhotoRepository,
        val dao: ProgressPhotoDao,
        val storage: ProgressPhotoStorage,
    )

    private class EventTransactionRunner(
        private val events: MutableList<String>,
    ) : DatabaseTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T {
            events += "transaction-start"
            return try {
                block().also { events += "transaction-commit" }
            } catch (throwable: Throwable) {
                events += "transaction-failed"
                throw throwable
            }
        }
    }

    private enum class ProjectCleanupFailure {
        PROGRESS_PHOTOS,
        CAPTURES,
        PATTERN_PDF,
    }

    private fun ProjectCleanupFailure?.raiseIf(
        expected: ProjectCleanupFailure,
        message: String,
    ) {
        if (this == expected) throw IOException(message)
    }

    private companion object {
        const val PROJECT_ID = 7L
        const val OTHER_PROJECT_ID = 8L
        const val PHOTO_ID = 3L
        const val PHOTO_URI = "file:///progress_photos/7/photo.jpg"
        const val SECOND_PHOTO_URI = "file:///progress_photos/7/second.jpg"
        const val PATTERN_A = "file:///pattern-a.pdf"
        const val PATTERN_B = "file:///pattern-b.pdf"
    }
}

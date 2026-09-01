package com.finnvek.knittools.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternDao
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.SavedPatternSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class WebPatternRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: SavedPatternRepository
    private lateinit var projectDocumentRepository: ProjectDocumentRepository
    private lateinit var counterRepository: CounterRepository
    private val createdPdfFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        repository = createRepository(database.counterProjectDao())
        projectDocumentRepository = createProjectDocumentRepository()
        counterRepository = createCounterRepository()
    }

    @After
    fun tearDown() {
        database.close()
        createdPdfFiles.forEach { file -> file.delete() }
    }

    // CPD-OFF: Room-testien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun simultaneousCreateActionsInsertOnlyOneCanonicalUrl() =
        runTest {
            val start = CompletableDeferred<Unit>()
            val input = WebPatternInput("Pattern", "", "HTTPS://EXAMPLE.COM:443/pattern")
            val results =
                coroutineScope {
                    List(2) {
                        async(Dispatchers.Default) {
                            start.await()
                            repository.createWebPattern(input)
                        }
                    }.also { start.complete(Unit) }.awaitAll()
                }

            val created = results.filterIsInstance<WebPatternMutationResult.Created>().single()
            val duplicate = results.filterIsInstance<WebPatternMutationResult.Duplicate>().single()
            assertEquals(created.patternId, duplicate.existingPatternId)
            assertEquals(1, database.savedPatternDao().getAllOnce().size)
        }

    @Test
    fun linkedTitleUpdateRollsBackWhenProjectPropagationFails() =
        runTest {
            val patternId = database.savedPatternDao().insert(webEntity(name = "Old title", updatedAt = 50L))
            database.counterProjectDao().insert(
                CounterProjectEntity(
                    name = "Project",
                    linkedPatternId = patternId,
                    patternName = "Old title",
                ),
            )
            repository = createRepository(failingTitlePropagationDao(database.counterProjectDao()))

            val result =
                repository.updateWebPattern(
                    patternId = patternId,
                    expectedUpdatedAt = 50L,
                    input = WebPatternInput("New title", "", "https://example.com/new"),
                )

            assertEquals(WebPatternMutationResult.PersistenceFailure, result)
            assertEquals("Old title", database.savedPatternDao().getById(patternId)?.name)
            assertEquals(
                "Old title",
                database
                    .counterProjectDao()
                    .getAllProjectsOnce()
                    .single()
                    .patternName,
            )
        }

    @Test
    fun webPatternDeletionClearsCopiedMetadataAndPreservesProjectDocuments() =
        runTest {
            val patternId = createWebPattern("Web pattern", "https://example.com/delete")
            val projectId =
                database.counterProjectDao().insert(
                    CounterProjectEntity(name = "Project"),
                )
            assertEquals(
                SavedPatternMetadataMutationResult.Attached(patternId),
                counterRepository.attachSavedPatternMetadata(projectId, patternId),
            )
            val firstUri = createReadablePdf("delete-first")
            val secondUri = createReadablePdf("delete-second")
            counterRepository.attachPattern(projectId, firstUri, "First PDF", 3, null)
            counterRepository.attachPattern(projectId, secondUri, "Second PDF", 0, null)
            val documentsBefore = projectDocumentRepository.getDocuments(projectId)

            val result = repository.deleteWebPattern(patternId)

            assertEquals(SavedPatternDeleteResult.Deleted, result)
            val project = requireNotNull(database.counterProjectDao().getProject(projectId))
            assertNull(project.linkedPatternId)
            assertNull(project.patternName)
            assertNull(database.savedPatternDao().getById(patternId))
            val documentsAfter = projectDocumentRepository.getDocuments(projectId)
            assertEquals(documentsBefore.map { it.id }, documentsAfter.map { it.id })
            assertEquals(documentsBefore.map { it.savedPatternId }, documentsAfter.map { it.savedPatternId })
            assertEquals(listOf(0, 1), documentsAfter.map { it.sortOrder })
            assertEquals(listOf(true, false), documentsAfter.map { it.isPrimary })
            assertEquals(listOf(firstUri, secondUri), documentsAfter.map { it.localPdfUri })
            assertEquals(3, documentsAfter.first().currentPage)
            assertTrue(createdPdfFiles.all(File::exists))
        }

    @Test
    fun webPatternDeletionRollsBackProjectCleanupWhenRowDeletionFails() =
        runTest {
            val patternId = database.savedPatternDao().insert(webEntity(name = "Web pattern"))
            val projectId =
                database.counterProjectDao().insert(
                    CounterProjectEntity(
                        name = "Project",
                        linkedPatternId = patternId,
                        patternName = "Web pattern",
                    ),
                )
            val documentId =
                database.projectDocumentDao().insert(readableDocument(projectId, "rollback"))
            repository =
                createRepository(
                    counterProjectDao = database.counterProjectDao(),
                    savedPatternDao = failingDeleteDao(database.savedPatternDao()),
                )

            val result = repository.deleteWebPattern(patternId)

            assertEquals(SavedPatternDeleteResult.PersistenceFailure, result)
            val project = requireNotNull(database.counterProjectDao().getProject(projectId))
            assertEquals(patternId, project.linkedPatternId)
            assertEquals("Web pattern", project.patternName)
            assertEquals(patternId, database.savedPatternDao().getById(patternId)?.id)
            assertEquals(documentId, database.projectDocumentDao().getPrimary(projectId)?.id)
        }

    @Test
    fun webMetadataThenMultiplePdfsPreservesLinkPrimaryOrderAndUnlinkPreservesDocuments() =
        runTest {
            val patternId = createWebPattern("Web pattern", "https://example.com/web-first")
            val projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Web first"))
            assertEquals(
                SavedPatternMetadataMutationResult.Attached(patternId),
                counterRepository.attachSavedPatternMetadata(projectId, patternId),
            )
            val firstUri = createReadablePdf("web-first-one")
            val secondUri = createReadablePdf("web-first-two")

            counterRepository.attachPattern(projectId, firstUri, "First PDF", 0, null)
            counterRepository.attachPattern(projectId, secondUri, "Second PDF", 0, null)

            val linkedProject = requireNotNull(database.counterProjectDao().getProject(projectId))
            assertEquals(patternId, linkedProject.linkedPatternId)
            assertEquals("Web pattern", linkedProject.patternName)
            val documentsBefore = projectDocumentRepository.getDocuments(projectId)
            assertEquals(listOf(0, 1), documentsBefore.map { it.sortOrder })
            assertEquals(listOf(true, false), documentsBefore.map { it.isPrimary })
            assertEquals(listOf(firstUri, secondUri), documentsBefore.map { it.localPdfUri })

            assertEquals(
                SavedPatternMetadataMutationResult.Unlinked,
                counterRepository.unlinkSavedPatternMetadata(projectId, patternId),
            )

            val unlinkedProject = requireNotNull(database.counterProjectDao().getProject(projectId))
            assertNull(unlinkedProject.linkedPatternId)
            assertNull(unlinkedProject.patternName)
            val documentsAfter = projectDocumentRepository.getDocuments(projectId)
            assertEquals(documentsBefore.map { it.id }, documentsAfter.map { it.id })
            assertEquals(documentsBefore.map { it.savedPatternId }, documentsAfter.map { it.savedPatternId })
            assertEquals(documentsBefore.map { it.sortOrder }, documentsAfter.map { it.sortOrder })
            assertEquals(documentsBefore.map { it.isPrimary }, documentsAfter.map { it.isPrimary })
        }

    @Test
    fun multiplePdfsThenWebMetadataPreservesDocumentsAndReplacesOnlyCopiedLink() =
        runTest {
            val patternId = createWebPattern("Web pattern", "https://example.com/pdf-first")
            val projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "PDF first"))
            val firstUri = createReadablePdf("pdf-first-one")
            val secondUri = createReadablePdf("pdf-first-two")
            counterRepository.attachPattern(projectId, firstUri, "First PDF", 0, null)
            counterRepository.attachPattern(projectId, secondUri, "Second PDF", 0, null)
            val linkedPdfPatternId =
                requireNotNull(database.counterProjectDao().getProject(projectId)?.linkedPatternId)
            val documentsBefore = projectDocumentRepository.getDocuments(projectId)

            assertEquals(
                SavedPatternMetadataMutationResult.Attached(patternId),
                counterRepository.attachSavedPatternMetadata(
                    projectId = projectId,
                    savedPatternId = patternId,
                    expectedExistingSavedPatternId = linkedPdfPatternId,
                ),
            )

            val project = requireNotNull(database.counterProjectDao().getProject(projectId))
            assertEquals(patternId, project.linkedPatternId)
            assertEquals("Web pattern", project.patternName)
            val documentsAfter = projectDocumentRepository.getDocuments(projectId)
            assertEquals(documentsBefore.map { it.id }, documentsAfter.map { it.id })
            assertEquals(documentsBefore.map { it.savedPatternId }, documentsAfter.map { it.savedPatternId })
            assertEquals(listOf(0, 1), documentsAfter.map { it.sortOrder })
            assertEquals(listOf(true, false), documentsAfter.map { it.isPrimary })
            assertEquals(listOf(firstUri, secondUri), documentsAfter.map { it.localPdfUri })
        }

    @Test
    fun projectDeletionPreservesReusableWebPatternAndImportedPdfRecords() =
        runTest {
            val patternId = createWebPattern("Reusable web pattern", "https://example.com/reusable")
            val projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Disposable project"))
            assertEquals(
                SavedPatternMetadataMutationResult.Attached(patternId),
                counterRepository.attachSavedPatternMetadata(projectId, patternId),
            )
            val firstUri = createReadablePdf("project-delete-one")
            val secondUri = createReadablePdf("project-delete-two")
            counterRepository.attachPattern(projectId, firstUri, "First PDF", 0, null)
            counterRepository.attachPattern(projectId, secondUri, "Second PDF", 0, null)
            val savedPatternIdsBefore =
                database
                    .savedPatternDao()
                    .getAllOnce()
                    .map { it.id }
                    .toSet()

            counterRepository.deleteProject(projectId)

            assertNull(database.counterProjectDao().getProject(projectId))
            assertTrue(projectDocumentRepository.getDocuments(projectId).isEmpty())
            assertEquals(
                savedPatternIdsBefore,
                database
                    .savedPatternDao()
                    .getAllOnce()
                    .map { it.id }
                    .toSet(),
            )
            assertEquals("Reusable web pattern", repository.getById(patternId)?.name)
            assertTrue(createdPdfFiles.all { file -> file.exists() })
        }

    // CPD-ON

    private fun createRepository(
        counterProjectDao: CounterProjectDao,
        savedPatternDao: SavedPatternDao = database.savedPatternDao(),
    ): SavedPatternRepository =
        SavedPatternRepository(
            dao = savedPatternDao,
            context = context,
            counterProjectDao = counterProjectDao,
            transactionRunner = RoomDatabaseTransactionRunner(database),
            ioDispatcher = Dispatchers.IO,
            projectDocumentDao = database.projectDocumentDao(),
        )

    private fun createProjectDocumentRepository(): ProjectDocumentRepository {
        val transactionRunner = RoomDatabaseTransactionRunner(database)
        return ProjectDocumentRepository(
            documentDao = database.projectDocumentDao(),
            projectDao = database.counterProjectDao(),
            savedPatternRepository = repository,
            layerRepository =
                PatternAnnotationLayerRepository(
                    database.patternAnnotationLayerDao(),
                    transactionRunner,
                ),
            transactionRunner = transactionRunner,
            fileAvailability = ProjectDocumentFileAvailability(context, Dispatchers.IO),
        )
    }

    private fun createCounterRepository(): CounterRepository {
        val transactionRunner = RoomDatabaseTransactionRunner(database)
        return CounterRepository(
            dao = database.counterProjectDao(),
            projectCounterDao = database.projectCounterDao(),
            sessionDao = database.sessionDao(),
            photoStorage = ProgressPhotoStorage(),
            patternDocumentStorage = PatternDocumentStorage(),
            context = context,
            yarnCardRepository =
                YarnCardRepository(
                    dao = database.yarnCardDao(),
                    counterProjectDao = database.counterProjectDao(),
                    context = context,
                    transactionRunner = transactionRunner,
                    ioDispatcher = Dispatchers.IO,
                ),
            savedPatternRepository = repository,
            projectDocumentRepository = projectDocumentRepository,
            projectFolderDao = database.projectFolderDao(),
            transactionRunner = transactionRunner,
            ioDispatcher = Dispatchers.IO,
        )
    }

    private suspend fun createWebPattern(
        name: String,
        url: String,
    ): Long =
        (repository.createWebPattern(WebPatternInput(name, "", url)) as WebPatternMutationResult.Created).patternId

    private fun createReadablePdf(suffix: String): String {
        val directory = File(context.filesDir, "pattern_pdfs/web-pattern-room-tests").apply(File::mkdirs)
        val file = File(directory, "$suffix-${System.nanoTime()}.pdf")
        file.writeBytes("%PDF-1.4\n%%EOF".toByteArray())
        createdPdfFiles += file
        return Uri.fromFile(file).toString()
    }

    private fun webEntity(
        name: String,
        updatedAt: Long = 10L,
    ): SavedPatternEntity =
        SavedPatternEntity(
            source = SavedPatternSource.WebLink.persistedValue,
            name = name,
            designerName = "",
            originalUrl = "https://example.com/pattern",
            canonicalUrl = "https://example.com/pattern",
            savedAt = 10L,
            updatedAt = updatedAt,
        )

    private fun readableDocument(
        projectId: Long,
        suffix: String,
    ): ProjectDocumentEntity =
        ProjectDocumentEntity(
            projectId = projectId,
            savedPatternId = null,
            documentKey = "project:$projectId:$suffix",
            label = "Readable PDF",
            localPdfUri = "content://$suffix.pdf",
            sortOrder = 0,
            isPrimary = true,
            currentPage = 0,
            rowMapping = null,
            readingLineEnabled = false,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = 10L,
            updatedAt = 10L,
        )

    private fun failingTitlePropagationDao(delegate: CounterProjectDao): CounterProjectDao =
        Proxy.newProxyInstance(
            CounterProjectDao::class.java.classLoader,
            arrayOf(CounterProjectDao::class.java),
        ) { _, method, arguments ->
            if (method.name == "updateLinkedPatternName") {
                error("forced project metadata failure")
            }
            try {
                method.invoke(delegate, *(arguments ?: emptyArray()))
            } catch (error: InvocationTargetException) {
                throw error.targetException
            }
        } as CounterProjectDao

    private fun failingDeleteDao(delegate: SavedPatternDao): SavedPatternDao =
        Proxy.newProxyInstance(
            SavedPatternDao::class.java.classLoader,
            arrayOf(SavedPatternDao::class.java),
        ) { _, method, arguments ->
            if (method.name == "deleteById") {
                error("forced saved pattern deletion failure")
            }
            try {
                method.invoke(delegate, *(arguments ?: emptyArray()))
            } catch (error: InvocationTargetException) {
                throw error.targetException
            }
        } as SavedPatternDao
}

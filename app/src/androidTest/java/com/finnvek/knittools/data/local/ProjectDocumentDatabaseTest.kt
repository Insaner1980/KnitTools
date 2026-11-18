package com.finnvek.knittools.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDocumentDatabaseTest {
    private lateinit var database: KnitToolsDatabase
    private lateinit var dao: ProjectDocumentDao

    // CPD-OFF: Room-testin tietokanta-alustus pidetaan skeematestin yhteydessa.
    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    KnitToolsDatabase::class.java,
                ).addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        dao = database.projectDocumentDao()
    }

    // CPD-ON

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun daoOrdersDocumentsAndRejectsDuplicateIdentityAndSecondPrimary() =
        runTest {
            val projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Cardigan"))
            val savedPatternId =
                database.savedPatternDao().insert(
                    SavedPatternEntity(
                        source = "LOCAL_FILE",
                        name = "Chart",
                        designerName = "",
                        localPdfUri = "file:///chart.pdf",
                    ),
                )
            dao.insert(document(projectId, id = 0, order = 1, key = "b", uri = "file:///b.pdf"))
            dao.insert(
                document(
                    projectId,
                    id = 0,
                    order = 0,
                    key = "a",
                    uri = "file:///a.pdf",
                    primary = true,
                    savedPatternId = savedPatternId,
                ),
            )

            assertEquals(listOf("a", "b"), dao.observeForProject(projectId).first().map { it.documentKey })
            assertEquals("a", dao.observePrimary(projectId).first()?.documentKey)
            assertEquals(true, dao.isUriReferenced("file:///a.pdf"))

            assertConstraintFailure {
                dao.insert(document(projectId, order = 2, key = "a", uri = "file:///other.pdf"))
            }
            assertConstraintFailure {
                dao.insert(document(projectId, order = 2, key = "other", uri = "file:///a.pdf"))
            }
            assertConstraintFailure {
                dao.insert(
                    document(
                        projectId,
                        order = 2,
                        key = "saved-duplicate",
                        uri = "file:///saved-duplicate.pdf",
                        savedPatternId = savedPatternId,
                    ),
                )
            }
            assertConstraintFailure {
                dao.insert(
                    document(
                        projectId,
                        order = 2,
                        key = "primary-2",
                        uri = "file:///primary-2.pdf",
                        primary = true,
                    ),
                )
            }
        }

    @Test
    fun savedPatternDeletionSetsNullAndProjectDeletionCascades() =
        runTest {
            val projectId = database.counterProjectDao().insert(CounterProjectEntity(name = "Socks"))
            val savedPatternId =
                database.savedPatternDao().insert(
                    SavedPatternEntity(
                        source = "LOCAL_FILE",
                        name = "Sock chart",
                        designerName = "",
                        localPdfUri = "file:///sock.pdf",
                    ),
                )
            val documentId =
                dao.insert(
                    document(
                        projectId = projectId,
                        key = "sock",
                        uri = "file:///sock.pdf",
                        primary = true,
                        savedPatternId = savedPatternId,
                    ),
                )

            database.savedPatternDao().deleteById(savedPatternId)

            assertNull(dao.getById(documentId)?.savedPatternId)
            assertEquals("file:///sock.pdf", dao.getById(documentId)?.localPdfUri)

            database.counterProjectDao().delete(projectId)

            assertNull(dao.getById(documentId))
        }

    @Test
    fun bulkQueriesNullableLinksUriReferencesAndOrderTiesUseStableIds() =
        runTest {
            val firstProject = database.counterProjectDao().insert(CounterProjectEntity(name = "First"))
            val secondProject = database.counterProjectDao().insert(CounterProjectEntity(name = "Second"))
            val firstId = dao.insert(document(firstProject, order = 0, key = "first", uri = "file:///first.pdf"))
            val tiedId = dao.insert(document(firstProject, order = 0, key = "tied", uri = "file:///tied.pdf"))
            val secondId =
                dao.insert(
                    document(
                        secondProject,
                        order = 4,
                        key = "second",
                        uri = "file:///second.pdf",
                        primary = true,
                    ),
                )

            assertEquals(listOf(firstId, tiedId), dao.getForProject(firstProject).map { it.id })
            assertTrue(dao.getForProject(firstProject).all { it.savedPatternId == null })
            assertEquals(2, dao.countForProject(firstProject))
            assertEquals(4, dao.getHighestSortOrder(secondProject))
            assertEquals(firstId, dao.getByDocumentKey(firstProject, "first")?.id)
            assertEquals(tiedId, dao.getByUri(firstProject, "file:///tied.pdf")?.id)
            assertEquals(secondId, dao.getPrimary(secondProject)?.id)
            assertEquals(
                setOf(firstId, tiedId, secondId),
                dao.getForProjects(listOf(firstProject, secondProject)).mapTo(mutableSetOf()) { it.id },
            )
            assertEquals(
                setOf("file:///first.pdf", "file:///tied.pdf", "file:///second.pdf"),
                dao.getDistinctUris(listOf(firstProject, secondProject)).toSet(),
            )
            assertEquals(false, dao.isUriReferenced("file:///missing.pdf"))
        }

    // CPD-OFF: Dokumenttientiteetin testidata pidetaan skeematestin yhteydessa.
    private fun document(
        projectId: Long,
        id: Long = 0,
        order: Int = 0,
        key: String,
        uri: String,
        primary: Boolean = false,
        savedPatternId: Long? = null,
    ) = ProjectDocumentEntity(
        id = id,
        projectId = projectId,
        savedPatternId = savedPatternId,
        documentKey = key,
        label = key,
        localPdfUri = uri,
        sortOrder = order,
        isPrimary = primary,
        currentPage = 0,
        rowMapping = null,
        readingLineEnabled = false,
        readingLineYFraction = 0.5f,
        readingLineFollowCurrentRow = true,
        verticalReadingGuideEnabled = false,
        verticalReadingGuideXFraction = 0.5f,
        createdAt = 1,
        updatedAt = 1,
    )

    // CPD-ON

    private suspend fun assertConstraintFailure(block: suspend () -> Unit) {
        try {
            block()
            fail("SQLite constraint accepted an invalid project document")
        } catch (_: SQLiteConstraintException) {
            // Skeemaraja hylkäsi virheellisen tilan odotetusti.
        }
    }
}

package com.finnvek.knittools.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.repository.PatternBookmarkMutationResult
import com.finnvek.knittools.repository.PatternBookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternBookmarkLifecycleTest {
    private lateinit var database: KnitToolsDatabase
    private lateinit var repository: PatternBookmarkRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .allowMainThreadQueries()
                .build()
        repository =
            PatternBookmarkRepository(
                bookmarkDao = database.patternBookmarkDao(),
                annotationLayerDao = database.patternAnnotationLayerDao(),
                projectDao = database.counterProjectDao(),
                transactionRunner = RoomDatabaseTransactionRunner(database),
                ioDispatcher = Dispatchers.Unconfined,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bookmarksFollowProjectAndActiveDocumentLifecycle() =
        runTest {
            val projectDao = database.counterProjectDao()
            val layerDao = database.patternAnnotationLayerDao()
            val bookmarkDao = database.patternBookmarkDao()
            val savedPatternDao = database.savedPatternDao()
            projectDao.insert(CounterProjectEntity(id = 1, name = "First", linkedPatternId = 91))
            projectDao.insert(CounterProjectEntity(id = 2, name = "Second", linkedPatternId = 91))
            savedPatternDao.insert(
                SavedPatternEntity(
                    id = 91,
                    source = "LOCAL_FILE",
                    name = "Pattern",
                    designerName = "Designer",
                ),
            )
            val firstLayerId = layerDao.insert(projectLayer(projectId = 1, documentKey = ORIGINAL_KEY, active = true))
            layerDao.insert(projectLayer(projectId = 2, documentKey = ORIGINAL_KEY, active = true))

            assertTrue(repository.add(1, ORIGINAL_KEY, "Body", 1, 0.4f) is PatternBookmarkMutationResult.Success)
            assertTrue(repository.add(2, ORIGINAL_KEY, "Sleeve", 2, 0.6f) is PatternBookmarkMutationResult.Success)
            assertEquals(
                listOf("Body"),
                repository
                    .observeActiveBookmarks(1)
                    .first()
                    .bookmarks
                    .map { it.name },
            )
            assertEquals(
                listOf("Sleeve"),
                repository
                    .observeActiveBookmarks(2)
                    .first()
                    .bookmarks
                    .map { it.name },
            )

            layerDao.deactivateProjectLayers(1, updatedAt = 10)
            assertTrue(
                repository
                    .observeActiveBookmarks(1)
                    .first()
                    .bookmarks
                    .isEmpty(),
            )
            layerDao.setActive(firstLayerId, isActive = true, updatedAt = 11)
            assertEquals(
                listOf("Body"),
                repository
                    .observeActiveBookmarks(1)
                    .first()
                    .bookmarks
                    .map { it.name },
            )

            layerDao.deactivateProjectLayers(1, updatedAt = 12)
            layerDao.insert(projectLayer(projectId = 1, documentKey = REPLACEMENT_KEY, active = true))
            assertTrue(
                repository
                    .observeActiveBookmarks(1)
                    .first()
                    .bookmarks
                    .isEmpty(),
            )
            layerDao.deactivateProjectLayers(1, updatedAt = 13)
            layerDao.setActive(firstLayerId, isActive = true, updatedAt = 14)
            assertEquals(
                listOf("Body"),
                repository
                    .observeActiveBookmarks(1)
                    .first()
                    .bookmarks
                    .map { it.name },
            )

            savedPatternDao.deleteById(91)
            assertEquals(1, bookmarkDao.observeForProjectDocument(1, ORIGINAL_KEY).first().size)

            projectDao.delete(1)
            assertTrue(bookmarkDao.observeForProjectDocument(1, ORIGINAL_KEY).first().isEmpty())
            assertEquals(1, bookmarkDao.observeForProjectDocument(2, ORIGINAL_KEY).first().size)
        }

    @Test
    fun bookmarkOrderIsDeterministicAndProjectCompletionPreservesRows() =
        runTest {
            val projectDao = database.counterProjectDao()
            val bookmarkDao = database.patternBookmarkDao()
            projectDao.insert(CounterProjectEntity(id = 3, name = "Ordered"))
            database.patternAnnotationLayerDao().insert(
                projectLayer(projectId = 3, documentKey = ORIGINAL_KEY, active = true),
            )
            val ids =
                listOf(
                    bookmarkDao.insert(bookmark(projectId = 3, page = 2, y = 0.1f, createdAt = 1)),
                    bookmarkDao.insert(bookmark(projectId = 3, page = 0, y = 0.7f, createdAt = 2)),
                    bookmarkDao.insert(bookmark(projectId = 3, page = 0, y = 0.2f, createdAt = 3)),
                    bookmarkDao.insert(bookmark(projectId = 3, page = 0, y = 0.2f, createdAt = 1)),
                )

            assertEquals(
                listOf(ids[3], ids[2], ids[1], ids[0]),
                bookmarkDao.observeForProjectDocument(3, ORIGINAL_KEY).first().map { it.id },
            )

            projectDao.archiveProject(id = 3, totalRows = 12, completedAt = 20, updatedAt = 20)
            assertEquals(4, bookmarkDao.observeForProjectDocument(3, ORIGINAL_KEY).first().size)
            projectDao.reactivateProject(id = 3, updatedAt = 21)
            assertEquals(4, bookmarkDao.observeForProjectDocument(3, ORIGINAL_KEY).first().size)
        }

    private fun projectLayer(
        projectId: Long,
        documentKey: String,
        active: Boolean,
    ) = PatternAnnotationLayerEntity(
        projectId = projectId,
        savedPatternId = null,
        documentKey = documentKey,
        isActive = active,
        createdAt = projectId,
        updatedAt = projectId,
    )

    private fun bookmark(
        projectId: Long,
        page: Int,
        y: Float,
        createdAt: Long,
    ) = PatternBookmarkEntity(
        projectId = projectId,
        documentKey = ORIGINAL_KEY,
        name = "Bookmark",
        pageIndex = page,
        yFraction = y,
        createdAt = createdAt,
    )

    private companion object {
        private const val ORIGINAL_KEY = "saved:91:v1"
        private const val REPLACEMENT_KEY = "saved:92:v1"
    }
}

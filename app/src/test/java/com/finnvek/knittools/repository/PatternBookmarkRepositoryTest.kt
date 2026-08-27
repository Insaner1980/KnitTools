package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.CounterProjectDao
import com.finnvek.knittools.data.local.ImmediateDatabaseTransactionRunner
import com.finnvek.knittools.data.local.PatternAnnotationLayerDao
import com.finnvek.knittools.data.local.PatternAnnotationLayerEntity
import com.finnvek.knittools.data.local.PatternBookmarkDao
import com.finnvek.knittools.data.local.PatternBookmarkEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PatternBookmarkRepositoryTest {
    @Test
    fun `active document bookmarks are observed in DAO order`() =
        runTest {
            val bookmarkDao = mockk<PatternBookmarkDao>()
            val layerDao = mockk<PatternAnnotationLayerDao>()
            every { layerDao.observeProjectLayers(7L) } returns flowOf(listOf(activeLayer()))
            every { bookmarkDao.observeForProjectDocument(7L, DOCUMENT_KEY) } returns
                flowOf(
                    listOf(
                        bookmark(id = 1, name = "First", page = 0, y = 0.2f),
                        bookmark(id = 2, name = "Second", page = 1, y = 0.1f),
                    ),
                )
            val repository = repository(bookmarkDao, layerDao)

            val state = repository.observeActiveBookmarks(7L).first()

            assertEquals(DOCUMENT_KEY, state.documentKey)
            assertEquals(listOf("First", "Second"), state.bookmarks.map { it.name })
        }

    @Test
    fun `add trims names and permits duplicates`() =
        runTest {
            val bookmarkDao = mockk<PatternBookmarkDao>(relaxed = true)
            val layerDao = activeLayerDao()
            coEvery { bookmarkDao.insert(any()) } returnsMany listOf(10L, 11L)
            val repository = repository(bookmarkDao, layerDao)

            val first = repository.add(7L, DOCUMENT_KEY, "  Sleeve  ", 2, 0.4f)
            val second = repository.add(7L, DOCUMENT_KEY, "Sleeve", 2, 0.4f)

            assertTrue(first is PatternBookmarkMutationResult.Success)
            assertTrue(second is PatternBookmarkMutationResult.Success)
            coVerify(exactly = 2) {
                bookmarkDao.insert(match { it.name == "Sleeve" && it.pageIndex == 2 && it.yFraction == 0.4f })
            }
        }

    @Test
    fun `invalid names fail before persistence`() =
        runTest {
            val bookmarkDao = mockk<PatternBookmarkDao>(relaxed = true)
            val repository = repository(bookmarkDao, activeLayerDao())

            assertEquals(PatternBookmarkMutationResult.EmptyName, repository.add(7L, DOCUMENT_KEY, "   ", 0, 0.5f))
            assertEquals(
                PatternBookmarkMutationResult.NameTooLong,
                repository.add(7L, DOCUMENT_KEY, "x".repeat(51), 0, 0.5f),
            )
            coVerify(exactly = 0) { bookmarkDao.insert(any()) }
        }

    @Test
    fun `stale document identity fails closed`() =
        runTest {
            val bookmarkDao = mockk<PatternBookmarkDao>(relaxed = true)
            val layerDao = activeLayerDao(documentKey = "saved:other:v1")
            val repository = repository(bookmarkDao, layerDao)

            val result = repository.add(7L, DOCUMENT_KEY, "Body", 0, 0.5f)

            assertEquals(PatternBookmarkMutationResult.StaleDocument, result)
            coVerify(exactly = 0) { bookmarkDao.insert(any()) }
        }

    @Test
    fun `rename delete and jump validate ownership`() =
        runTest {
            val bookmarkDao = mockk<PatternBookmarkDao>(relaxed = true)
            val projectDao = mockk<CounterProjectDao>(relaxed = true)
            val entity = bookmark(id = 8, name = "Old", page = 3, y = 0.8f)
            coEvery { bookmarkDao.getById(8L) } returns entity
            val repository = repository(bookmarkDao, activeLayerDao(), projectDao)

            val renamed = repository.rename(7L, DOCUMENT_KEY, 8L, " New name ")
            val jumped = repository.jumpTo(7L, DOCUMENT_KEY, 8L)
            val deleted = repository.delete(7L, DOCUMENT_KEY, 8L)

            assertEquals("New name", (renamed as PatternBookmarkMutationResult.Success).bookmark.name)
            assertTrue(jumped is PatternBookmarkMutationResult.Success)
            assertTrue(deleted is PatternBookmarkMutationResult.Success)
            coVerify { bookmarkDao.updateName(8L, "New name") }
            coVerify { projectDao.updatePatternViewerLocation(7L, 3, 0.8f, false, any()) }
            coVerify { bookmarkDao.deleteById(8L) }
        }

    @Test
    fun `cancellation from transaction runner propagates`() =
        runTest {
            val repository =
                PatternBookmarkRepository(
                    bookmarkDao = mockk(relaxed = true),
                    annotationLayerDao = mockk(relaxed = true),
                    projectDao = mockk(relaxed = true),
                    transactionRunner =
                        object : com.finnvek.knittools.data.local.DatabaseTransactionRunner {
                            override suspend fun <T> run(block: suspend () -> T): T =
                                throw CancellationException("cancelled")
                        },
                    ioDispatcher = Dispatchers.Unconfined,
                )

            try {
                repository.add(7L, DOCUMENT_KEY, "Body", 0, 0.5f)
                fail("CancellationException expected")
            } catch (_: CancellationException) {
                // Peruutus kuuluu välittää kutsujalle muuttamatta sitä tavalliseksi virheeksi.
            }
        }

    private fun repository(
        bookmarkDao: PatternBookmarkDao,
        layerDao: PatternAnnotationLayerDao,
        projectDao: CounterProjectDao = mockk(relaxed = true),
    ) = PatternBookmarkRepository(
        bookmarkDao = bookmarkDao,
        annotationLayerDao = layerDao,
        projectDao = projectDao,
        transactionRunner = ImmediateDatabaseTransactionRunner,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun activeLayerDao(documentKey: String = DOCUMENT_KEY): PatternAnnotationLayerDao =
        mockk<PatternAnnotationLayerDao>(relaxed = true).also { dao ->
            coEvery { dao.getActiveProjectLayer(7L) } returns activeLayer(documentKey)
        }

    private fun activeLayer(documentKey: String = DOCUMENT_KEY) =
        PatternAnnotationLayerEntity(
            id = 5,
            projectId = 7,
            savedPatternId = null,
            documentKey = documentKey,
            isActive = true,
            createdAt = 1,
            updatedAt = 1,
        )

    private fun bookmark(
        id: Long,
        name: String,
        page: Int,
        y: Float,
    ) = PatternBookmarkEntity(
        id = id,
        projectId = 7,
        documentKey = DOCUMENT_KEY,
        name = name,
        pageIndex = page,
        yFraction = y,
        createdAt = id,
    )

    private companion object {
        private const val DOCUMENT_KEY = "saved:91:v1"
    }
}

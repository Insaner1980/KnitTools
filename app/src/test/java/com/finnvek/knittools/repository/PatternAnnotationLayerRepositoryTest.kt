package com.finnvek.knittools.repository

import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.PatternAnnotationLayerDao
import com.finnvek.knittools.data.local.PatternAnnotationLayerEntity
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationLayerRepositoryTest {
    @Test
    fun `master layer is created once inside a transaction`() =
        runTest {
            val dao = mockk<PatternAnnotationLayerDao>(relaxed = true)
            val runner = CountingTransactionRunner()
            val repository = PatternAnnotationLayerRepository(dao, runner)
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            coEvery { dao.getSavedPatternLayer(12L, documentKey) } returns null
            coEvery { dao.insert(any()) } returns 41L

            val layer = repository.getOrCreateMasterLayer(12L, documentKey)

            assertEquals(1, runner.runCount)
            assertEquals(41L, layer.id)
            assertEquals(PatternAnnotationOwner.SavedPattern(12L, documentKey), layer.owner)
            assertTrue(layer.isActive)
            coVerify(exactly = 1) {
                dao.insert(
                    match {
                        it.savedPatternId == 12L &&
                            it.projectId == null &&
                            it.documentKey == documentKey &&
                            it.isActive
                    },
                )
            }
        }

    @Test
    fun `reattach activates the existing project layer without replacing annotations`() =
        runTest {
            val dao = mockk<PatternAnnotationLayerDao>(relaxed = true)
            val runner = CountingTransactionRunner()
            val repository = PatternAnnotationLayerRepository(dao, runner)
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val existing = projectLayer(id = 51L, projectId = 7L, documentKey = documentKey, isActive = false)
            coEvery { dao.getProjectLayer(7L, documentKey) } returns existing

            val layer = repository.activateProjectLayer(7L, documentKey)

            assertEquals(1, runner.runCount)
            assertEquals(51L, layer.id)
            assertTrue(layer.isActive)
            coVerifyOrder {
                dao.deactivateProjectLayers(7L, any())
                dao.getProjectLayer(7L, documentKey)
                dao.setActive(51L, true, any())
            }
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    @Test
    fun `pattern switch deactivates old layer and creates a separate active layer`() =
        runTest {
            val dao = mockk<PatternAnnotationLayerDao>(relaxed = true)
            val runner = CountingTransactionRunner()
            val repository = PatternAnnotationLayerRepository(dao, runner)
            val documentKey = PatternAnnotationDocumentKey.savedPattern(13L)
            coEvery { dao.getProjectLayer(7L, documentKey) } returns null
            coEvery { dao.insert(any()) } returns 52L

            val layer = repository.activateProjectLayer(7L, documentKey)

            assertEquals(1, runner.runCount)
            assertEquals(52L, layer.id)
            coVerifyOrder {
                dao.deactivateProjectLayers(7L, any())
                dao.getProjectLayer(7L, documentKey)
                dao.insert(
                    match {
                        it.projectId == 7L &&
                            it.savedPatternId == null &&
                            it.documentKey == documentKey &&
                            it.isActive
                    },
                )
            }
        }

    @Test
    fun `detach only deactivates project layers inside a transaction`() =
        runTest {
            val dao = mockk<PatternAnnotationLayerDao>(relaxed = true)
            val runner = CountingTransactionRunner()
            val repository = PatternAnnotationLayerRepository(dao, runner)

            repository.deactivateProjectLayers(7L)

            assertEquals(1, runner.runCount)
            coVerify(exactly = 1) { dao.deactivateProjectLayers(7L, any()) }
        }

    private fun projectLayer(
        id: Long,
        projectId: Long,
        documentKey: String,
        isActive: Boolean,
    ) = PatternAnnotationLayerEntity(
        id = id,
        projectId = projectId,
        savedPatternId = null,
        documentKey = documentKey,
        isActive = isActive,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )
}

private class CountingTransactionRunner : DatabaseTransactionRunner {
    var runCount = 0

    override suspend fun <T> run(block: suspend () -> T): T {
        runCount += 1
        return block()
    }
}

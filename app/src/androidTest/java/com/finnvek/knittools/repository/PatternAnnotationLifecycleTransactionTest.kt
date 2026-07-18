package com.finnvek.knittools.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.domain.model.PatternAnnotationDocumentKey
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PatternAnnotationLifecycleTransactionTest {
    private lateinit var database: KnitToolsDatabase
    private lateinit var transactionRunner: RoomDatabaseTransactionRunner
    private lateinit var layerRepository: PatternAnnotationLayerRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    KnitToolsDatabase::class.java,
                ).addCallback(PatternAnnotationSchemaConstraints.callback)
                .build()
        transactionRunner = RoomDatabaseTransactionRunner(database)
        layerRepository = PatternAnnotationLayerRepository(database.patternAnnotationLayerDao(), transactionRunner)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun failedAttachmentRollsBackLayerActivationAndProjectAttachment() =
        runTest {
            database.counterProjectDao().insert(CounterProjectEntity(id = 7L, name = "Project"))
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)

            val failure =
                runCatching {
                    transactionRunner.run {
                        layerRepository.activateProjectLayerInTransaction(7L, documentKey)
                        database.counterProjectDao().updatePatternAttachment(
                            id = 7L,
                            linkedPatternId = 12L,
                            patternUri = "content://pattern.pdf",
                            patternName = "Pattern",
                            currentPatternPage = 0,
                            patternRowMapping = null,
                            updatedAt = 2_000L,
                        )
                        throw IOException("test rollback")
                    }
                }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertNull(layerRepository.getActiveProjectLayer(7L))
            assertNull(database.counterProjectDao().getProject(7L)?.patternUri)
        }

    @Test
    fun failedDetachmentRestoresActiveLayerAndProjectAttachment() =
        runTest {
            database.counterProjectDao().insert(
                CounterProjectEntity(
                    id = 7L,
                    name = "Project",
                    linkedPatternId = 12L,
                    patternUri = "content://pattern.pdf",
                    patternName = "Pattern",
                ),
            )
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            layerRepository.activateProjectLayer(7L, documentKey)

            val failure =
                runCatching {
                    transactionRunner.run {
                        layerRepository.deactivateProjectLayersInTransaction(7L)
                        database.counterProjectDao().updatePatternAttachment(
                            id = 7L,
                            linkedPatternId = null,
                            patternUri = null,
                            patternName = null,
                            currentPatternPage = 0,
                            patternRowMapping = null,
                            updatedAt = 3_000L,
                        )
                        throw IOException("test rollback")
                    }
                }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertTrue(layerRepository.getActiveProjectLayer(7L)?.isActive == true)
            assertEquals("content://pattern.pdf", database.counterProjectDao().getProject(7L)?.patternUri)
        }

    @Test
    fun savedPatternDeleteRemovesMasterButPreservesProjectLayerUntilProjectDelete() =
        runTest {
            database.counterProjectDao().insert(CounterProjectEntity(id = 7L, name = "Project"))
            database.savedPatternDao().insert(
                SavedPatternEntity(
                    id = 12L,
                    source = "LOCAL_FILE",
                    name = "Pattern",
                    designerName = "Imported",
                ),
            )
            val documentKey = PatternAnnotationDocumentKey.savedPattern(12L)
            val master = layerRepository.getOrCreateMasterLayer(12L, documentKey)
            val project = layerRepository.activateProjectLayer(7L, documentKey)

            database.savedPatternDao().deleteById(12L)

            val projectLayersAfterSavedDelete =
                database.patternAnnotationLayerDao().getProjectLayer(7L, documentKey)
            assertNull(database.patternAnnotationLayerDao().getSavedPatternLayer(12L, documentKey))
            assertEquals(master.owner.documentKey, projectLayersAfterSavedDelete?.documentKey)
            assertFalse(master.id == project.id)

            database.counterProjectDao().delete(7L)

            assertNull(database.patternAnnotationLayerDao().getProjectLayer(7L, documentKey))
        }
}

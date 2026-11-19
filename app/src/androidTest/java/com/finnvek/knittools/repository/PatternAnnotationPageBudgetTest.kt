package com.finnvek.knittools.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationPageLimitException
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Modifier

@RunWith(AndroidJUnit4::class)
class PatternAnnotationPageBudgetTest {
    private lateinit var db: KnitToolsDatabase
    private lateinit var dao: PatternAnnotationDao
    private lateinit var repository: PatternAnnotationRepository
    private lateinit var layers: PatternAnnotationLayerRepository
    private var layerId = 0L

    @Before fun setUp() =
        runBlocking {
            db =
                Room
                    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KnitToolsDatabase::class.java)
                    .addCallback(PatternAnnotationSchemaConstraints.callback)
                    .build()
            dao = db.patternAnnotationDao()
            repository = PatternAnnotationRepository(dao)
            layers = PatternAnnotationLayerRepository(db.patternAnnotationLayerDao(), RoomDatabaseTransactionRunner(db))
            db.counterProjectDao().insert(CounterProjectEntity(id = 1L, name = "Project"))
            layerId = layers.activateProjectLayer(1L, "document").id
        }

    @After fun tearDown() = db.close()

    @Test fun exactCountRepositoryInsertAndDirectDaoCannotExceedIt() =
        runBlocking {
            repository.restoreBatch((1L..255L).map(::shape))
            repository.insertAnnotation(shape(256L))
            rejected { repository.insertAnnotation(shape(257L)) }
            rejected { dao.insert(shape(258L).toEntity()) }
            rejected { dao.restoreBatch(listOf(shape(259L).toEntity())) }
            assertEquals(256, repository.observePage(layerId, 0).first().size)
            assertEquals(256L, count())
            assertTrue(
                PatternAnnotationDao::class.java.declaredMethods
                    .filter { "Unchecked" in it.name }
                    .all { Modifier.isProtected(it.modifiers) },
            )
        }

    @Test fun exactWorkAndOnePointUpdateAndInsertAreAtomic() =
        runBlocking {
            repository.restoreBatch((1L..8L).map { stroke(it, 2_048) })
            rejected { repository.insertAnnotation(stroke(9L, 1)) }
            assertEquals(8L, count())
            repository.updateAnnotation(stroke(8L, 2_047))
            repository.insertAnnotation(stroke(9L, 1))
            rejected { dao.update(stroke(8L, 2_048).toEntity()) }
            assertEquals(
                2_047,
                (
                    repository
                        .observePage(layerId, 0)
                        .first()
                        .first {
                            it.id == 8L
                        }.payload as FreehandPayload
                ).points.size,
            )
        }

    @Test fun fullDestinationMoveAndBatchReplacementPreserveFinalState() =
        runBlocking {
            repository.restoreBatch((1L..256L).map(::shape))
            val moved = shape(300L).copy(page = 1)
            repository.insertAnnotation(moved)
            rejected { repository.updateAnnotation(moved.copy(page = 0)) }
            assertEquals(1, repository.observePage(layerId, 1).first().size)
            repository.restoreBatch(listOf(shape(1L).copy(page = 1), moved.copy(page = 0)))
            assertEquals(256, repository.observePage(layerId, 0).first().size)
            repository.restoreBatch(listOf(shape(2L).copy(page = 1), shape(2L)))
            assertEquals(256, repository.observePage(layerId, 0).first().size)
            rejected { repository.restoreBatch(listOf(shape(0L).copy(page = 2), shape(0L))) }
            assertEquals(0, repository.observePage(layerId, 2).first().size)
            val otherLayer = layers.activateProjectLayer(1L, "other").id
            val other = shape(400L).copy(layerId = otherLayer)
            repository.insertAnnotation(other)
            layers.activateProjectLayer(1L, "document")
            rejected { dao.update(other.copy(layerId = layerId).toEntity()) }
            assertEquals(1, repository.observePage(otherLayer, 0).first().size)
        }

    @Test fun concurrentInsertsSerializeTheBudgetCheck() =
        runBlocking {
            repository.restoreBatch((1L..255L).map(::shape))
            val results =
                (256L..263L)
                    .map { id ->
                        async(Dispatchers.IO) { runCatching { repository.insertAnnotation(shape(id)) } }
                    }.awaitAll()
            assertEquals(1, results.count { it.isSuccess })
            assertTrue(
                results.filter { it.isFailure }.all { it.exceptionOrNull() is PatternAnnotationPageLimitException },
            )
            assertEquals(256L, count())
        }

    @Test fun legacyOversizedPageFailsWithoutDeletingOrRenderingPrefix() =
        runBlocking {
            repository.restoreBatch((1L..256L).map(::shape))
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO pattern_annotations(layerId,page,kind,payloadVersion,payloadJson," +
                    "zIndex,createdAt,updatedAt) " +
                    "SELECT layerId,page,kind,payloadVersion,payloadJson,zIndex,createdAt,updatedAt " +
                    "FROM pattern_annotations WHERE id=1",
            )
            rejected { repository.observePage(layerId, 0).first() }
            rejected { repository.getForLayers(listOf(layerId)) }
            assertEquals(257L, count())
            repository.insertAnnotation(shape(300L).copy(page = 1))
            assertEquals(1, repository.observePage(layerId, 1).first().size)
            assertEquals(258L, count())
        }

    @Test fun undoRedoDetachReattachAndMasterReuseRemainFunctional() =
        runBlocking {
            val annotation = shape(1L)
            val undo = PatternAnnotationCommand.Insert(annotation).apply(repository)
            val redo = undo.apply(repository)
            redo.apply(repository)
            layers.deactivateProjectLayers(1L)
            assertEquals(layerId, layers.activateProjectLayer(1L, "document").id)
            assertEquals(listOf(annotation), repository.getForLayers(listOf(layerId)))
            db.savedPatternDao().insert(
                SavedPatternEntity(id = 12L, source = "LOCAL_FILE", name = "Pattern", designerName = "Imported"),
            )
            val master = layers.getOrCreateMasterLayer(12L, "saved:12:v1")
            repository.insertAnnotation(shape(2L).copy(layerId = master.id))
            assertEquals(master.id, layers.getOrCreateMasterLayer(12L, "saved:12:v1").id)
            assertEquals(2, repository.getForLayers(listOf(master.id, layerId)).size)
        }

    @Test fun generatedIdentityKeepsNewAnnotationAboveTiedMaximumRank() =
        runBlocking {
            val existing = shape(10L).copy(zIndex = Long.MAX_VALUE)
            repository.insertAnnotation(existing)
            val insertedId = repository.insertAnnotation(shape(0L).copy(zIndex = Long.MAX_VALUE))

            assertTrue(insertedId > existing.id)
            assertEquals(listOf(existing.id, insertedId), repository.observePage(layerId, 0).first().map { it.id })
        }

    @Test fun rankRebaseUndoAndRedoPreserveTheExactRows() =
        runBlocking {
            val before = listOf(shape(1L).copy(zIndex = Long.MIN_VALUE), shape(2L).copy(zIndex = Long.MAX_VALUE))
            repository.restoreBatch(before)
            val after = listOf(before[1].copy(zIndex = 0L), before[0].copy(zIndex = 1L, updatedAt = 2L))

            val undo = PatternAnnotationCommand.UpdateBatch(before, after).apply(repository)
            assertEquals(after, repository.observePage(layerId, 0).first())
            val redo = undo.apply(repository)
            assertEquals(before, repository.observePage(layerId, 0).first())
            redo.apply(repository)
            assertEquals(after, repository.observePage(layerId, 0).first())
        }

    @Test fun detachedLayerRejectsDelayedWritesUntilReattached() =
        runBlocking {
            val original = shape(1L)
            repository.insertAnnotation(original)
            layers.deactivateProjectLayers(1L)
            val activeLayerId = layers.activateProjectLayer(1L, "other-document").id
            val moved = original.copy(layerId = activeLayerId)
            val writes: List<suspend () -> Unit> =
                listOf(
                    { repository.insertAnnotation(shape(2L)) },
                    { repository.updateAnnotation(original.copy(zIndex = 2L)) },
                    { repository.restoreBatch(listOf(original.copy(zIndex = 2L))) },
                    { repository.updateAnnotation(moved) },
                    { repository.restoreBatch(listOf(moved)) },
                    { repository.clearPage(layerId, 0) },
                    { repository.deleteAnnotation(original.id) },
                )
            writes.forEachIndexed { index, write ->
                assertTrue(
                    "Detached write $index must fail",
                    runCatching { write() }.exceptionOrNull() is IllegalStateException,
                )
                assertEquals(listOf(original), repository.getForLayers(listOf(layerId)))
                assertTrue(repository.getForLayers(listOf(activeLayerId)).isEmpty())
            }
            assertEquals(layerId, layers.activateProjectLayer(1L, "document").id)
            val updated = original.copy(zIndex = 2L)
            repository.updateAnnotation(updated)
            assertEquals(listOf(updated), repository.getForLayers(listOf(layerId)))
        }

    private fun shape(id: Long) =
        PatternAnnotation(
            id,
            layerId,
            0,
            PatternAnnotationKind.LINE,
            ShapePayload(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f), 0, 2f),
            id,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun stroke(
        id: Long,
        points: Int,
    ) = shape(id).copy(
        kind = PatternAnnotationKind.FREEHAND,
        payload = FreehandPayload(List(points) { NormalizedPatternPoint(it / 2_048f, 0.5f) }, 0, 2f),
    )

    private fun count(): Long =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM pattern_annotations").use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private suspend fun rejected(block: suspend () -> Unit) {
        assertTrue(runCatching { block() }.exceptionOrNull() is PatternAnnotationPageLimitException)
    }
}

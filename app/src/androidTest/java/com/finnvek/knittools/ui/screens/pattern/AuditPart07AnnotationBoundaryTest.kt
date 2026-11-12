package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationDao
import com.finnvek.knittools.data.local.PatternAnnotationEntity
import com.finnvek.knittools.data.local.PatternAnnotationExportStats
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.ShapePayload
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.RoomCounterTestFixture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** Device-only command boundaries backed by an isolated Room database and the actual ViewModel. */
@RunWith(AndroidJUnit4::class)
class AuditPart07AnnotationBoundaryTest {
    private lateinit var db: KnitToolsDatabase
    private lateinit var fixture: RoomCounterTestFixture
    private lateinit var layers: PatternAnnotationLayerRepository
    private lateinit var repository: PatternAnnotationRepository
    private lateinit var port: AnnotationBoundaryDao
    private lateinit var vm: PatternAnnotationViewModel
    private var layer = 0L
    private var master = 0L
    private var nextLayer = 0L
    private val point = NormalizedPatternPoint(0.5f, 0.5f)

    private enum class Operation { STROKE, ERASE, DUPLICATE, UNDO, REDO }

    @Before fun setUp() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            check(context.packageName == "com.finnvek.knittools.activepolicytest")
            db =
                Room
                    .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                    .addCallback(PatternAnnotationSchemaConstraints.callback)
                    .addCallback(ProjectDocumentSchemaConstraints.callback)
                    .build()
            fixture = RoomCounterTestFixture(db, context)
            layers = PatternAnnotationLayerRepository(db.patternAnnotationLayerDao(), RoomDatabaseTransactionRunner(db))
            db.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Annotation audit"))
            db.savedPatternDao().insert(
                SavedPatternEntity(id = 12, source = "LOCAL_FILE", name = "Master", designerName = "Test"),
            )
            db.projectDocumentDao().insert(document(1, "saved:12:v1", 12, true))
            db.projectDocumentDao().insert(document(2, "next-document", null, false))
            master = layers.getOrCreateMasterLayer(12, "saved:12:v1").id
            nextLayer = layers.activateProjectLayer(1, "next-document").id
            layer = layers.activateProjectLayer(1, "saved:12:v1").id
            port = AnnotationBoundaryDao(db)
            repository = PatternAnnotationRepository(port)
            repository.insertAnnotation(shape(0, master).copy(zIndex = 100))
            startViewModel()
        }

    @After fun tearDown() =
        runBlocking {
            if (::vm.isInitialized) vm.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
            if (::db.isInitialized) db.close()
        }

    @Test fun prompt_1035_documentChangeBeforePointerUpDropsDraft() =
        runBlocking {
            draft()
            assertEquals(1, rows().size)
            main {
                vm.setCurrentPage(1)
                vm.commitStroke(0f)
            }
            awaitState { it.currentPage == 1 && it.draftStroke == null }
            assertEquals(1, rows().size)
            main { vm.setCurrentPage(0) }
            awaitState { it.currentPage == 0 }
            draft()
            switchDocument()
            main { vm.commitStroke(0f) }
            main { yield() }
            assertEquals(1, rows().size)
            assertNull(vm.uiState.value.draftStroke)
            assertFalse(vm.uiState.value.canUndo)
        }

    @Test fun prompt_1036_gestureFailureRollsBack() = failure(Operation.STROKE)

    @Test fun prompt_1037_repeatedPointerUpWritesOnce() = overlap(Operation.STROKE)

    @Test fun prompt_1038_gestureCancellationAndLateContext() = cancellation(Operation.STROKE)

    @Test fun prompt_1043_eraseKeepsMasterAndNewTopmostCompetitor() =
        runBlocking {
            prepare(Operation.ERASE)
            val before = rows()
            val target = before.single { it.layerId == layer }
            val gate = port.arm(AnnotationBoundaryPhase.BEFORE_WRITE)
            invoke(Operation.ERASE)
            gate.entered.awaitBounded()
            val competingId = db.patternAnnotationDao().insert(shape(0, layer).copy(zIndex = 50).toEntity())
            gate.release.complete(Unit)
            awaitState { !it.isSaving && it.canUndo && it.projectAnnotations.size == 1 }
            assertEquals(before.filter { it.id != target.id } + rows().single { it.id == competingId }, rows())
            assertEquals(before.filter { it.layerId == master }, rows().filter { it.layerId == master })
        }

    @Test fun prompt_1044_eraseFailureRollsBack() = failure(Operation.ERASE)

    @Test fun prompt_1045_overlappingEraseHasOneHistoryEntry() = overlap(Operation.ERASE)

    @Test fun prompt_1046_eraseCancellationAndLateContext() = cancellation(Operation.ERASE)

    @Test fun prompt_1047_duplicateRechecksBudgetAfterCompetingGesture() =
        runBlocking {
            prepare(Operation.DUPLICATE)
            db.patternAnnotationDao().restoreBatch((100L..353L).map { shape(it, layer).toEntity() })
            awaitState { it.projectAnnotations.size == 255 }
            val gate = port.arm(AnnotationBoundaryPhase.BEFORE_WRITE)
            invoke(Operation.DUPLICATE)
            gate.entered.awaitBounded()
            db.patternAnnotationDao().insert(shape(354, layer).toEntity())
            val before = rows()
            gate.release.complete(Unit)
            awaitState { !it.isSaving && it.writeError == PatternAnnotationWriteError.WRITE_FAILED }
            assertEquals(before, rows())
            assertFalse(vm.uiState.value.canUndo)
        }

    @Test fun prompt_1048_duplicateFailureRollsBack() = failure(Operation.DUPLICATE)

    @Test fun prompt_1049_overlappingDuplicateHasOneNewIdentity() = overlap(Operation.DUPLICATE)

    @Test fun prompt_1050_duplicateCancellationAndLateContext() = cancellation(Operation.DUPLICATE)

    @Test fun prompt_1051_documentSwitchInvalidatesUndo() =
        runBlocking {
            prepare(Operation.UNDO)
            val before = rows()
            switchDocument()
            main { vm.undo() }
            main { yield() }
            assertEquals(before, rows())
            assertFalse(vm.uiState.value.canUndo)
            assertFalse(vm.uiState.value.canRedo)
        }

    @Test fun prompt_1052_undoFailureKeepsUndoEntry() = failure(Operation.UNDO)

    @Test fun prompt_1053_overlappingUndoRunsOnce() = overlap(Operation.UNDO)

    @Test fun prompt_1054_undoCancellationAndLateContext() = cancellation(Operation.UNDO)

    @Test fun prompt_1055_newCommandInvalidatesRedo() =
        runBlocking {
            prepare(Operation.REDO)
            main { vm.addTextBox("Intervening command") }
            awaitState { !it.isSaving && it.canUndo && !it.canRedo && it.projectAnnotations.size == 1 }
            val before = rows()
            main { vm.redo() }
            main { yield() }
            assertEquals(before, rows())
            assertFalse(vm.uiState.value.canRedo)
        }

    @Test fun prompt_1056_redoFailureKeepsRedoEntry() = failure(Operation.REDO)

    @Test fun prompt_1057_overlappingRedoRunsOnce() = overlap(Operation.REDO)

    @Test fun prompt_1058_redoCancellationAndLateContext() = cancellation(Operation.REDO)

    private fun failure(operation: Operation) =
        runBlocking {
            prepare(operation)
            val before = rows()
            val history = vm.uiState.value.let { it.canUndo to it.canRedo }
            val gate = port.arm(AnnotationBoundaryPhase.LAST_PRECOMMIT, fail = true)
            invoke(operation)
            gate.entered.awaitBounded()
            gate.release.complete(Unit)
            awaitState { !it.isSaving && it.writeError == PatternAnnotationWriteError.WRITE_FAILED }
            assertEquals(before, rows())
            assertEquals(history, vm.uiState.value.let { it.canUndo to it.canRedo })
            invoke(operation)
            awaitSuccessful(operation, before)
            assertMasterUnchanged(before)
        }

    private fun overlap(operation: Operation) =
        runBlocking {
            prepare(operation)
            val before = rows()
            val gate = port.arm(AnnotationBoundaryPhase.BEFORE_WRITE)
            val calls = port.writes
            invoke(operation)
            gate.entered.awaitBounded()
            awaitState { it.isSaving }
            invoke(operation)
            main { yield() }
            assertEquals(calls + 1, port.writes)
            assertEquals(before, rows())
            gate.release.complete(Unit)
            awaitSuccessful(operation, before)
            assertEquals(calls + 1, port.writes)
            assertMasterUnchanged(before)
            if (operation == Operation.DUPLICATE) {
                val original = before.single { it.layerId == layer }
                val duplicate = rows().single { it.layerId == layer && it.id != original.id }
                assertTrue(duplicate.id != original.id)
                assertTrue(duplicate.zIndex > original.zIndex)
                assertEquals(original.page, duplicate.page)
            }
            if (operation == Operation.UNDO) {
                assertFalse(vm.uiState.value.canUndo)
                assertTrue(vm.uiState.value.canRedo)
            } else {
                main { vm.undo() }
                awaitState { !it.isSaving && !it.canUndo && it.canRedo }
                assertEquals(before, rows())
            }
        }

    private fun cancellation(operation: Operation) =
        runBlocking {
            prepare(operation)
            val before = rows()
            val gate = port.arm(AnnotationBoundaryPhase.LAST_PRECOMMIT)
            invoke(operation)
            gate.entered.awaitBounded()
            withTimeout(10_000) { checkNotNull(vm.viewModelScope.coroutineContext[Job]).cancelAndJoin() }
            assertEquals(before, rows())
            assertMasterUnchanged(before)
            startViewModel()
            prepare(operation)
            val beforeLate = rows()
            val late = port.arm(AnnotationBoundaryPhase.AFTER_COMMIT)
            invoke(operation)
            late.entered.awaitBounded()
            val committed = rows()
            assertEquals(expectedCount(operation, beforeLate), committed.count { it.layerId == layer })
            switchDocument()
            late.release.complete(Unit)
            awaitState { !it.isSaving && it.editableLayerId == nextLayer }
            assertEquals(committed, rows())
            assertTrue(
                vm.uiState.value.projectAnnotations
                    .isEmpty(),
            )
            assertFalse(vm.uiState.value.canUndo)
            assertFalse(vm.uiState.value.canRedo)
            assertEquals(PatternAnnotationWriteError.NONE, vm.uiState.value.writeError)
            assertMasterUnchanged(beforeLate)
        }

    private suspend fun prepare(operation: Operation) {
        when (operation) {
            Operation.STROKE -> draft()
            Operation.ERASE, Operation.DUPLICATE -> {
                if (rows().none { it.layerId == layer }) repository.insertAnnotation(shape(0, layer))
                awaitState { it.projectAnnotations.isNotEmpty() }
                main { vm.selectAnnotationAt(point) }
                awaitState { it.selectedAnnotationIsEditable }
            }
            Operation.UNDO, Operation.REDO -> {
                main { vm.addTextBox("History payload") }
                awaitState { !it.isSaving && it.canUndo }
                if (operation == Operation.REDO) {
                    main { vm.undo() }
                    awaitState { !it.isSaving && it.canRedo && !it.canUndo }
                }
            }
        }
    }

    private suspend fun invoke(operation: Operation) =
        main {
            when (operation) {
                Operation.STROKE -> vm.commitStroke(0f)
                Operation.ERASE -> vm.eraseStrokeAt(point)
                Operation.DUPLICATE -> vm.duplicateSelected()
                Operation.UNDO -> vm.undo()
                Operation.REDO -> vm.redo()
            }
        }

    private suspend fun draft() {
        main {
            vm.setActiveTool(PatternAnnotationTool.PEN)
            vm.beginStroke(NormalizedPatternPoint(0.1f, 0.1f))
            repeat(32) { vm.appendStrokePoint(NormalizedPatternPoint(0.2f + it / 100f, 0.5f)) }
        }
        awaitState { it.draftStroke != null }
    }

    private fun expectedCount(
        operation: Operation,
        before: List<PatternAnnotation>,
    ) = before.count { it.layerId == layer } +
        if (operation == Operation.ERASE || operation == Operation.UNDO) -1 else 1

    private suspend fun awaitSuccessful(
        operation: Operation,
        before: List<PatternAnnotation>,
    ) {
        val count = expectedCount(operation, before)
        awaitState {
            !it.isSaving &&
                it.writeError == PatternAnnotationWriteError.NONE &&
                it.projectAnnotations.size == count &&
                if (operation == Operation.UNDO) it.canRedo else it.canUndo
        }
        assertEquals(count, rows().count { it.layerId == layer })
    }

    private suspend fun switchDocument() {
        layers.activateProjectLayer(1, "next-document")
        awaitState { it.editableLayerId == nextLayer && it.projectAnnotations.isEmpty() }
    }

    private suspend fun startViewModel() {
        main {
            vm =
                PatternAnnotationViewModel(
                    SavedStateHandle(mapOf("projectId" to 1L)),
                    fixture.counterRepository(),
                    layers,
                    repository,
                    fixture.projectDocumentRepository,
                )
        }
        awaitState { it.editableLayerId == layer && it.masterAnnotations.size == 1 }
    }

    private suspend fun awaitState(predicate: (PatternAnnotationUiState) -> Boolean) =
        withTimeout(10_000) { vm.uiState.first(predicate) }

    private suspend fun main(action: suspend () -> Unit) = withContext(Dispatchers.Main) { action() }

    private suspend fun CompletableDeferred<Unit>.awaitBounded() = withTimeout(10_000) { await() }

    private suspend fun rows() = repository.getForLayers(listOf(master, layer, nextLayer))

    private suspend fun assertMasterUnchanged(before: List<PatternAnnotation>) =
        assertEquals(before.filter { it.layerId == master }, rows().filter { it.layerId == master })

    private fun shape(
        id: Long,
        owner: Long,
    ) = PatternAnnotation(
        id,
        owner,
        0,
        PatternAnnotationKind.LINE,
        ShapePayload(NormalizedPatternPoint(0.1f, 0.1f), NormalizedPatternPoint(0.9f, 0.9f), 0, 2f),
        id,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun document(
        id: Long,
        key: String,
        saved: Long?,
        primary: Boolean,
    ) = ProjectDocumentEntity(
        id,
        1,
        saved,
        key,
        "Audit $id",
        "file:///annotation-audit-$id.pdf",
        (id - 1).toInt(),
        primary,
        0,
        null,
        false,
        0.5f,
        false,
        false,
        0.5f,
        1,
        1,
    )
}

private enum class AnnotationBoundaryPhase { BEFORE_WRITE, LAST_PRECOMMIT, AFTER_COMMIT }

private class AnnotationBoundaryGate(
    val phase: AnnotationBoundaryPhase,
    val fail: Boolean,
) {
    val entered = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()

    suspend fun pause() {
        entered.complete(Unit)
        release.await()
        if (fail) throw IOException("Injected final annotation statement failure")
    }
}

/** Delegates the public DAO contract to Room; the outer transaction exposes its final rollback boundary. */
private class AnnotationBoundaryDao(
    private val db: KnitToolsDatabase,
) : PatternAnnotationDao() {
    override suspend fun getTextEditLayer(layerId: Long) = db.patternAnnotationDao().getTextEditLayer(layerId)

    private val real = db.patternAnnotationDao()
    private var gate: AnnotationBoundaryGate? = null
    var writes = 0
        private set

    fun arm(
        phase: AnnotationBoundaryPhase,
        fail: Boolean = false,
    ) = AnnotationBoundaryGate(phase, fail).also {
        check(gate == null)
        gate = it
    }

    private suspend fun <T> write(block: suspend () -> T): T {
        writes++
        val boundary = gate.also { gate = null }
        if (boundary?.phase == AnnotationBoundaryPhase.BEFORE_WRITE) boundary.pause()
        val result =
            db.withTransaction {
                val value = block()
                if (boundary?.phase == AnnotationBoundaryPhase.LAST_PRECOMMIT) boundary.pause()
                currentCoroutineContext().ensureActive()
                value
            }
        if (boundary?.phase == AnnotationBoundaryPhase.AFTER_COMMIT) boundary.pause()
        return result
    }

    override suspend fun insert(annotation: PatternAnnotationEntity) = write { real.insert(annotation) }

    override suspend fun update(annotation: PatternAnnotationEntity) = write { real.update(annotation) }

    override suspend fun restoreBatch(annotations: List<PatternAnnotationEntity>) =
        write { real.restoreBatch(annotations) }

    override suspend fun deleteById(id: Long) = write { real.deleteById(id) }

    override suspend fun deleteForPage(
        layerId: Long,
        page: Int,
    ) = write { real.deleteForPage(layerId, page) }

    override fun observePageChanges(
        layerId: Long,
        page: Int,
    ): Flow<Boolean> =
        real.observePage(layerId, page).map {
            it.isNotEmpty()
        }

    override suspend fun getPageBounded(
        layerId: Long,
        page: Int,
    ) = real.getPageBounded(layerId, page)

    override suspend fun getForLayersBounded(
        layerIds: List<Long>,
        maxAnnotations: Int,
        maxPayloadBytes: Long,
    ) = real.getForLayersBounded(layerIds, maxAnnotations, maxPayloadBytes)

    override suspend fun getPageStats(
        layerId: Long,
        page: Int,
    ): PatternAnnotationExportStats = error("Public read delegates to Room")

    override suspend fun getPageRows(
        layerId: Long,
        page: Int,
    ): List<PatternAnnotationEntity> = error("Public read delegates to Room")

    override suspend fun getForLayersLimited(
        layerIds: List<Long>,
        limit: Int,
    ): List<PatternAnnotationEntity> = error("Public read delegates to Room")

    override suspend fun getExportStats(layerIds: List<Long>): PatternAnnotationExportStats =
        error("Public read delegates to Room")

    override suspend fun insertUnchecked(annotation: PatternAnnotationEntity): Long =
        error("Public write delegates to Room")

    override suspend fun restoreUnchecked(annotations: List<PatternAnnotationEntity>): Unit =
        error("Public write delegates to Room")

    override suspend fun updateUnchecked(annotation: PatternAnnotationEntity): Unit =
        error("Public write delegates to Room")

    override suspend fun deleteForPageUnchecked(
        layerId: Long,
        page: Int,
    ): Unit = error("Public write delegates to Room")

    override suspend fun deleteByIdUnchecked(id: Long): Unit = error("Public write delegates to Room")

    override suspend fun getAnnotationLayerId(id: Long): Long? = error("Public write delegates to Room")
}

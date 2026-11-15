package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentEntity
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.local.SavedPatternEntity
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.local.toEntity
import com.finnvek.knittools.domain.model.NormalizedPatternBounds
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.TextBoxPayload
import com.finnvek.knittools.repository.PatternAnnotationLayerRepository
import com.finnvek.knittools.repository.PatternAnnotationRepository
import com.finnvek.knittools.repository.RoomCounterTestFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

internal class AnnotationTextTestFixture {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "annotation-text-${UUID.randomUUID()}.db"
    lateinit var db: KnitToolsDatabase
    lateinit var repository: PatternAnnotationRepository
    lateinit var layers: PatternAnnotationLayerRepository
    lateinit var vm: PatternAnnotationViewModel
    var layer = 0L
    var master = 0L
    var nextLayer = 0L
    var id = 0L
    val owner = PatternAnnotationOwner.Project(1, "saved:12:v1")

    suspend fun start() {
        check(context.packageName == "com.finnvek.knittools.activepolicytest")
        openDatabase()
        db.counterProjectDao().insert(CounterProjectEntity(id = 1, name = "Synthetic text editing"))
        db.savedPatternDao().insert(
            SavedPatternEntity(id = 12, source = "LOCAL_FILE", name = "Master", designerName = "Test"),
        )
        db.projectDocumentDao().insert(document(1, owner.documentKey, 12, true))
        db.projectDocumentDao().insert(document(2, "next-document", null, false))
        master = layers.getOrCreateMasterLayer(12, owner.documentKey).id
        nextLayer = layers.activateProjectLayer(1, "next-document").id
        layer = layers.activateProjectLayer(1, owner.documentKey).id
        val original = textAnnotation(layer).toEntity()
        id =
            db.patternAnnotationDao().insert(
                original.copy(
                    payloadJson =
                        original.payloadJson.dropLast(1) + ",\"future\":{\"keep\":42}}",
                ),
            )
        repository.insertAnnotation(textAnnotation(master))
        startViewModel()
    }

    private fun openDatabase() {
        db =
            Room
                .databaseBuilder(context, KnitToolsDatabase::class.java, databaseName)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        repository = PatternAnnotationRepository(db.patternAnnotationDao())
        layers = PatternAnnotationLayerRepository(db.patternAnnotationLayerDao(), RoomDatabaseTransactionRunner(db))
    }

    suspend fun reopen() {
        vm.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
        db.close()
        openDatabase()
        startViewModel()
    }

    private suspend fun startViewModel() {
        val fixture = RoomCounterTestFixture(db, context)
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
        awaitState { it.editableLayerId == layer && it.projectAnnotations.size == 1 && it.masterAnnotations.size == 1 }
    }

    suspend fun edit(): PatternAnnotationTextEdit {
        main {
            vm.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.3f))
            vm.editSelectedText()
        }
        return checkNotNull(awaitState { it.textEdit != null }.textEdit)
    }

    suspend fun awaitState(predicate: (PatternAnnotationUiState) -> Boolean): PatternAnnotationUiState =
        withTimeout(10_000) { vm.uiState.first(predicate) }

    suspend fun row() = db.patternAnnotationDao().getPageBounded(layer, 0).single()

    suspend fun annotation() = checkNotNull(row().toDomain())

    suspend fun close() {
        if (!::db.isInitialized) return
        if (::vm.isInitialized) vm.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
        db.close()
        context.deleteDatabase(databaseName)
    }

    private fun textAnnotation(layerId: Long) =
        PatternAnnotation(
            layerId = layerId,
            page = 0,
            kind = PatternAnnotationKind.TEXT_BOX,
            payload =
                TextBoxPayload(
                    NormalizedPatternBounds(0.2f, 0.2f, 0.8f, 0.4f),
                    "Original text",
                    18f,
                    0xff123456.toInt(),
                    0xffeeeeee.toInt(),
                    0.3f,
                ),
            zIndex = 7,
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
        "Synthetic $id",
        "file:///synthetic-$id.pdf",
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

internal suspend fun main(action: suspend () -> Unit) = withContext(Dispatchers.Main) { action() }

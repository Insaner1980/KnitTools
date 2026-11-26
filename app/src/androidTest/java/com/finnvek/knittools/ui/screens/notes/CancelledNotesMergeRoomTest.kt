package com.finnvek.knittools.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.DatabaseTransactionRunner
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.storage.NotesDraftStorage
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.RoomCounterTestFixture
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class CancelledNotesMergeRoomTest {
    @Test fun cancelledAfterMergedCommit() = exercise(cancelAfterCommit = true)

    @Test fun cancelledBeforeMergedCommit() = exercise(cancelAfterCommit = false)

    private fun exercise(cancelAfterCommit: Boolean) =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val db = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
            val store = ViewModelStore()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            try {
                withTimeout(20_000) {
                    val dao = db.counterProjectDao()
                    val id = dao.insert(CounterProjectEntity(name = "Two editors", notes = "Base", notesCreated = true))
                    val fixture = RoomCounterTestFixture(db, context)
                    val pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
                    val drafts = NotesDraftStorage(context, Dispatchers.IO)

                    fun editor(
                        key: String,
                        repo: CounterRepository,
                    ): NotesEditorViewModel {
                        val handle = SavedStateHandle(mapOf("projectId" to id))
                        val viewModel = NotesEditorViewModel(repo, pro, scope, handle, drafts)
                        store.put(key, viewModel)
                        return viewModel
                    }

                    val allowFirstWrite = CompletableDeferred<Unit>()
                    val atBoundary = CompletableDeferred<Unit>()
                    val runner =
                        object : DatabaseTransactionRunner {
                            var firstWrite = true

                            override suspend fun <T> run(block: suspend () -> T): T {
                                if (!firstWrite) {
                                    if (!cancelAfterCommit) assertEquals("Base independent", dao.getProject(id)?.notes)
                                    return fixture.transactionRunner.run(block)
                                }
                                firstWrite = false
                                allowFirstWrite.await()
                                fixture.transactionRunner.run {
                                    val value = block()
                                    if (!cancelAfterCommit) {
                                        atBoundary.complete(Unit)
                                        awaitCancellation()
                                    }
                                    value
                                }
                                atBoundary.complete(Unit)
                                awaitCancellation()
                            }
                        }
                    val first =
                        withContext(Dispatchers.Main) {
                            editor("first", fixture.counterRepository(transactions = runner))
                        }
                    val other = withContext(Dispatchers.Main) { editor("other", fixture.counterRepository()) }
                    first.uiState.first { it.isLoaded }
                    other.uiState.first { it.isLoaded }
                    withContext(Dispatchers.Main) {
                        first.onNotesChanged("Base red")
                        first.saveImmediately()
                        other.onNotesChanged("Base independent")
                        other.saveImmediately(exitAfterSave = true)
                    }
                    other.uiState.first { it.exitRequested }
                    assertEquals("Base independent", dao.getProject(id)?.notes)
                    allowFirstWrite.complete(Unit)
                    atBoundary.await()
                    if (cancelAfterCommit) assertEquals("Base independent red", dao.getProject(id)?.notes)
                    withContext(Dispatchers.Main) {
                        first.onNotesChanged("Base blue")
                        first.saveImmediately(exitAfterSave = true)
                    }
                    first.uiState.first { it.exitRequested }
                    assertEquals("Base independent blue", dao.getProject(id)?.notes)
                    assertEquals("Base independent blue", first.uiState.value.notes)
                }
            } finally {
                withContext(Dispatchers.Main) { store.clear() }
                scope.cancel()
                db.close()
            }
        }
}

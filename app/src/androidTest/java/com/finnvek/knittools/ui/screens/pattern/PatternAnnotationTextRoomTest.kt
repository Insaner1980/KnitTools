package com.finnvek.knittools.ui.screens.pattern

import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.TextBoxPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PatternAnnotationTextRoomTest {
    private val fixture = AnnotationTextTestFixture()

    @Before fun setUp() = runBlocking { fixture.start() }

    @After fun tearDown() = runBlocking { fixture.close() }

    @Test fun editPreservesIdentityPayloadAndPersistsAfterDatabaseReopens() =
        runBlocking {
            val before = fixture.row()
            val edit = fixture.edit()
            assertEquals("Original text", (edit.annotation.payload as TextBoxPayload).text)
            main { fixture.vm.saveTextEdit(edit, "Updated text\nwith another line") }
            fixture.awaitState { !it.isSaving && it.canUndo && it.textEdit == null }
            val after = fixture.row()
            assertEquals(before.copy(payloadJson = after.payloadJson, updatedAt = after.updatedAt), after)
            assertEquals(
                Json.parseToJsonElement(before.payloadJson).jsonObject - "text",
                Json.parseToJsonElement(after.payloadJson).jsonObject - "text",
            )
            assertEquals(
                "Updated text\nwith another line",
                (checkNotNull(after.toDomain()).payload as TextBoxPayload).text,
            )
            fixture.reopen()
            assertEquals(after, fixture.row())
            assertEquals("Updated text\nwith another line", (fixture.edit().annotation.payload as TextBoxPayload).text)
        }

    @Test fun cancelAndLateCancelledCallbackDoNotWrite() =
        runBlocking {
            val before = fixture.row()
            val edit = fixture.edit()
            main {
                fixture.vm.cancelTextEdit(edit)
                fixture.vm.saveTextEdit(edit, "Late")
            }
            fixture.awaitState { it.textEdit == null }
            assertFalse(fixture.vm.uiState.value.canUndo)
            assertEquals(before, fixture.row())
        }

    @Test fun undoRedoUseSameIdentityAndKeepUnknownFields() =
        runBlocking {
            val before = fixture.row()
            val edit = fixture.edit()
            main { fixture.vm.saveTextEdit(edit, "Edited") }
            fixture.awaitState { !it.isSaving && it.canUndo }
            main { fixture.vm.undo() }
            fixture.awaitState { !it.isSaving && it.canRedo && !it.canUndo }
            val undone = fixture.row()
            assertEquals(Json.parseToJsonElement(before.payloadJson), Json.parseToJsonElement(undone.payloadJson))
            assertEquals(before.id, undone.id)
            main { fixture.vm.redo() }
            fixture.awaitState { !it.isSaving && it.canUndo && !it.canRedo }
            assertEquals("Edited", (fixture.annotation().payload as TextBoxPayload).text)
            assertEquals(
                Json.parseToJsonElement(before.payloadJson).jsonObject["future"],
                Json.parseToJsonElement(fixture.row().payloadJson).jsonObject["future"],
            )
        }

    @Test fun successiveEditsCanUndoAndRedoAcrossUpdatedTimestamps() =
        runBlocking {
            for (text in listOf("Second", "Third")) {
                val edit = fixture.edit()
                main { fixture.vm.saveTextEdit(edit, text) }
                fixture.awaitState {
                    !it.isSaving &&
                        it.textEdit == null &&
                        (it.projectAnnotations.single().payload as TextBoxPayload).text == text
                }
            }
            for (text in listOf("Second", "Original text")) {
                main { fixture.vm.undo() }
                assertPersistedText(text)
            }
            assertFalse(fixture.vm.uiState.value.canUndo)
            for (text in listOf("Second", "Third")) {
                main { fixture.vm.redo() }
                assertPersistedText(text)
            }
            assertFalse(fixture.vm.uiState.value.canRedo)
        }

    @Test fun deleteAndRestoreDoNotBreakTextEditHistory() =
        runBlocking {
            val edit = fixture.edit()
            main { fixture.vm.saveTextEdit(edit, "Edited") }
            fixture.awaitState {
                !it.isSaving &&
                    it.textEdit == null &&
                    (it.projectAnnotations.single().payload as TextBoxPayload).text == "Edited"
            }
            main { fixture.vm.deleteSelected() }
            fixture.awaitState { !it.isSaving && it.projectAnnotations.isEmpty() }
            main { fixture.vm.undo() }
            fixture.awaitState { !it.isSaving && it.projectAnnotations.size == 1 }
            main { fixture.vm.undo() }
            fixture.awaitState { !it.isSaving && !it.canUndo }
            assertEquals("Original text", (fixture.annotation().payload as TextBoxPayload).text)
            main { fixture.vm.redo() }
            fixture.awaitState { !it.isSaving && it.canUndo }
            assertEquals("Edited", (fixture.annotation().payload as TextBoxPayload).text)
        }

    @Test fun staleUndoCannotOverwriteCompetingChange() =
        runBlocking {
            val edit = fixture.edit()
            main { fixture.vm.saveTextEdit(edit, "Edited") }
            fixture.awaitState { !it.isSaving && it.canUndo }
            val competitor = fixture.repository.updateText(fixture.owner, fixture.annotation(), "Competitor")
            main { fixture.vm.undo() }
            fixture.awaitState { !it.isSaving && it.writeError == PatternAnnotationWriteError.WRITE_FAILED }
            assertEquals(competitor, fixture.annotation())
            assertTrue(fixture.vm.uiState.value.canUndo)
        }

    @Test fun blankAndOversizedTextAreRejectedWithoutMutation() =
        runBlocking {
            val before = fixture.row()
            for (text in listOf(" \n\t", "x".repeat(256 * 1024))) {
                val result = runCatching { fixture.repository.updateText(fixture.owner, fixture.annotation(), text) }
                assertTrue(result.exceptionOrNull() is IllegalArgumentException)
                assertEquals(before, fixture.row())
            }
        }

    @Test fun masterIsReadOnlyInProjectButEditableInLibrary() =
        runBlocking {
            val master = fixture.repository.getForLayers(listOf(fixture.master)).single()
            val result = runCatching { fixture.repository.updateText(fixture.owner, master, "Forbidden") }
            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertEquals(master, fixture.repository.getForLayers(listOf(fixture.master)).single())
            val saved =
                fixture.repository.updateText(
                    PatternAnnotationOwner.SavedPattern(12, fixture.owner.documentKey),
                    master,
                    "Library edit",
                )
            assertEquals("Library edit", (saved.payload as TextBoxPayload).text)
            assertEquals(saved, fixture.repository.getForLayers(listOf(fixture.master)).single())
        }

    @Test fun pageSwitchAndReturnInvalidateOriginalEditor() =
        runBlocking {
            val before = fixture.row()
            val edit = fixture.edit()
            main {
                fixture.vm.setCurrentPage(1)
                fixture.vm.setCurrentPage(0)
                fixture.vm.saveTextEdit(edit, "Late")
            }
            fixture.awaitState { it.textEdit == null && it.currentPage == 0 }
            assertEquals(before, fixture.row())
            assertFalse(fixture.vm.uiState.value.canUndo)
        }

    @Test fun documentSwitchAndPermissionLossRejectOldEditorAndRepositoryWrite() =
        runBlocking {
            val before = fixture.row()
            val edit = fixture.edit()
            fixture.layers.activateProjectLayer(1, "next-document")
            fixture.awaitState { it.editableLayerId == fixture.nextLayer && it.textEdit == null }
            assertLateSaveRejected(edit)
            assertEquals(before, fixture.row())
            assertTrue(fixture.repository.getForLayers(listOf(fixture.nextLayer)).isEmpty())
        }

    @Test fun deletedAnnotationCannotBeRecreatedByLateSave() =
        runBlocking {
            val edit = fixture.edit()
            fixture.repository.deleteAnnotation(edit.annotation.id)
            fixture.awaitState { it.projectAnnotations.isEmpty() && it.textEdit == null }
            assertLateSaveRejected(edit)
            assertTrue(fixture.repository.getForLayers(listOf(fixture.layer)).isEmpty())
        }

    @Test fun selectionChangeDoesNotRedirectOpenEditor() =
        runBlocking {
            val edit = fixture.edit()
            val otherId = fixture.repository.insertAnnotation(edit.annotation.copy(id = 0, zIndex = 100))
            fixture.awaitState { it.projectAnnotations.size == 2 }
            main { fixture.vm.selectAnnotationAt(NormalizedPatternPoint(0.5f, 0.3f)) }
            fixture.awaitState { it.selectedAnnotationId == otherId }
            main { fixture.vm.saveTextEdit(edit, "Only original") }
            fixture.awaitState { !it.isSaving && it.canUndo }
            val rows = fixture.repository.getForLayers(listOf(fixture.layer))
            assertEquals("Only original", (rows.single { it.id == edit.annotation.id }.payload as TextBoxPayload).text)
            assertEquals("Original text", (rows.single { it.id == otherId }.payload as TextBoxPayload).text)
        }

    @Test fun competingGeometryAndTextChangesWhileOpenAreNotOverwritten() =
        runBlocking {
            val edit = fixture.edit()
            val payload = edit.annotation.payload as TextBoxPayload
            val competitor =
                edit.annotation.copy(
                    payload =
                        payload.copy(
                            text = "Competitor",
                            textSizeSp = 32f,
                            bounds = payload.bounds.copy(left = 0.1f),
                        ),
                )
            fixture.repository.updateAnnotation(competitor)
            main { fixture.vm.saveTextEdit(edit, "Mine") }
            fixture.awaitState { !it.isSaving && it.writeError == PatternAnnotationWriteError.WRITE_FAILED }
            assertEquals(competitor, fixture.annotation())
            assertFalse(fixture.vm.uiState.value.canUndo)
            assertEquals(edit, fixture.vm.uiState.value.textEdit)
        }

    @Test fun failureAtFinalPrecommitBoundaryRollsBack() =
        runBlocking {
            val before = fixture.row()
            val result =
                runCatching {
                    fixture.db.withTransaction {
                        val updated = fixture.repository.updateText(fixture.owner, fixture.annotation(), "Uncommitted")
                        assertEquals("Uncommitted", (updated.payload as TextBoxPayload).text)
                        throw IOException("Injected failure before commit")
                    }
                }
            assertTrue(result.exceptionOrNull() is IOException)
            assertEquals(before, fixture.row())
        }

    @Test fun cancellationBeforeCommitRollsBack() = cancellation(afterCommit = false)

    @Test fun cancellationAfterCommitLeavesCommittedEdit() = cancellation(afterCommit = true)

    private fun cancellation(afterCommit: Boolean) =
        runBlocking {
            val before = fixture.row()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val outcome = CompletableDeferred<Throwable?>()
            val job =
                launch {
                    val result =
                        runCatching {
                            fixture.db.withTransaction {
                                fixture.repository.updateText(
                                    fixture.owner,
                                    fixture.annotation(),
                                    "Committed only if allowed",
                                )
                                if (!afterCommit) {
                                    entered.complete(Unit)
                                    release.await()
                                }
                            }
                            if (afterCommit) {
                                entered.complete(Unit)
                                release.await()
                            }
                        }
                    outcome.complete(result.exceptionOrNull())
                }
            withTimeout(10_000) { entered.await() }
            job.cancelAndJoin()
            assertTrue(outcome.await() is CancellationException)
            if (afterCommit) {
                assertNotEquals(before, fixture.row())
                assertEquals("Committed only if allowed", (fixture.annotation().payload as TextBoxPayload).text)
                fixture.reopen()
                assertEquals("Committed only if allowed", (fixture.annotation().payload as TextBoxPayload).text)
            } else {
                assertEquals(before, fixture.row())
            }
            assertNull(fixture.vm.uiState.value.textEdit)
        }

    private suspend fun assertPersistedText(text: String) {
        fixture.awaitState {
            !it.isSaving && (it.projectAnnotations.single().payload as TextBoxPayload).text == text
        }
        assertEquals(text, (fixture.annotation().payload as TextBoxPayload).text)
    }

    private suspend fun assertLateSaveRejected(edit: PatternAnnotationTextEdit) {
        main { fixture.vm.saveTextEdit(edit, "Late") }
        val result =
            runCatching { fixture.repository.updateText(edit.owner, edit.annotation, "Late repository save") }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}

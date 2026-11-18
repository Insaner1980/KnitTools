package com.finnvek.knittools.ui.screens.pattern

import com.finnvek.knittools.domain.model.FreehandPayload
import com.finnvek.knittools.domain.model.NormalizedPatternPoint
import com.finnvek.knittools.domain.model.PatternAnnotation
import com.finnvek.knittools.domain.model.PatternAnnotationKind
import com.finnvek.knittools.repository.PatternAnnotationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAnnotationCommandTest {
    @Test
    fun `insert delete and restore each return a working inverse`() =
        runTest {
            val repository = mockk<PatternAnnotationRepository>(relaxed = true)
            val original = annotation(id = 0L)
            coEvery { repository.insertAnnotation(original) } returns 42L

            val delete = PatternAnnotationCommand.Insert(original).apply(repository)
            val restore = delete.apply(repository)
            val deleteAgain = restore.apply(repository)
            deleteAgain.apply(repository)

            assertTrue(delete is PatternAnnotationCommand.Delete)
            assertTrue(restore is PatternAnnotationCommand.Restore)
            assertTrue(deleteAgain is PatternAnnotationCommand.Delete)
            coVerify(exactly = 2) { repository.deleteAnnotation(42L) }
            coVerify(exactly = 1) { repository.restoreBatch(listOf(original.copy(id = 42L))) }
        }

    @Test
    fun `update forward undo and redo preserve both snapshots`() =
        runTest {
            val repository = mockk<PatternAnnotationRepository>(relaxed = true)
            val before = annotation(id = 7L, zIndex = 1L)
            val after = before.copy(zIndex = 9L)

            val undo = PatternAnnotationCommand.Update(before, after).apply(repository)
            val redo = undo.apply(repository)
            redo.apply(repository)

            coVerify(exactly = 2) { repository.updateAnnotation(after) }
            coVerify(exactly = 1) { repository.updateAnnotation(before) }
        }

    @Test
    fun `clear page forward undo and redo restore the complete batch`() =
        runTest {
            val repository = mockk<PatternAnnotationRepository>(relaxed = true)
            val removed = listOf(annotation(1L), annotation(2L))

            val undo = PatternAnnotationCommand.ClearPage(3L, 4, removed).apply(repository)
            val redo = undo.apply(repository)
            val undoAgain = redo.apply(repository)

            assertTrue(undo is PatternAnnotationCommand.RestorePage)
            assertTrue(redo is PatternAnnotationCommand.ClearPage)
            assertTrue(undoAgain is PatternAnnotationCommand.RestorePage)
            coVerify(exactly = 2) { repository.clearPage(3L, 4) }
            coVerify(exactly = 1) { repository.restoreBatch(removed) }
        }

    // CPD-OFF: Testidatan annotaatiorakenne pidetaan testin yhteydessa.
    private fun annotation(
        id: Long,
        zIndex: Long = 0L,
    ) = PatternAnnotation(
        id = id,
        layerId = 3L,
        page = 4,
        kind = PatternAnnotationKind.FREEHAND,
        payload =
            FreehandPayload(
                points = listOf(NormalizedPatternPoint(0f, 0f), NormalizedPatternPoint(1f, 1f)),
                argb = 0xFF000000.toInt(),
                strokeWidth = 2f,
            ),
        zIndex = zIndex,
    )
    // CPD-ON
}

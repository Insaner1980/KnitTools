package com.finnvek.knittools.ui.screens.pattern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternImageImportStateTest {
    @Test
    fun `append keeps first exact URI occurrence and preserves order`() {
        val existing = PatternImageSelection(listOf(page("a", 10L)))

        val result =
            existing.append(
                listOf(
                    page("a", 10L, id = "duplicate-existing"),
                    page("b", 20L),
                    page("b", 20L, id = "duplicate-batch"),
                    page("c", 30L),
                ),
            )

        assertTrue(result is PatternImageAppendResult.Accepted)
        result as PatternImageAppendResult.Accepted
        assertEquals(listOf("a", "b", "c"), result.selection.pages.map { it.sourceUri })
        assertEquals(2, result.duplicatesIgnored)
    }

    @Test
    fun `append rejects whole batch when page limit would be exceeded`() {
        val existingPages =
            (1 until PatternImageImportLimits.MAX_PAGES).map {
                page("existing-$it", 1L)
            }
        val existing = PatternImageSelection(existingPages)

        val result = existing.append(listOf(page("new-a", 1L), page("new-b", 1L)))

        assertEquals(PatternImageAppendResult.PageLimitExceeded, result)
        assertEquals(PatternImageImportLimits.MAX_PAGES - 1, existing.pages.size)
    }

    @Test
    fun `append rejects whole batch when total staged bytes would be exceeded`() {
        val existing = PatternImageSelection(listOf(page("existing", PatternImageImportLimits.MAX_TOTAL_BYTES - 5L)))

        val result = existing.append(listOf(page("new", 6L)))

        assertEquals(PatternImageAppendResult.TotalBytesExceeded, result)
        assertEquals(listOf("existing"), existing.pages.map { it.sourceUri })
    }

    @Test
    fun `move and remove keep an explicit stable page order`() {
        val selection = PatternImageSelection(listOf(page("a", 1L), page("b", 1L), page("c", 1L)))

        assertFalse(selection.canMoveEarlier(0))
        assertFalse(selection.canMoveLater(2))
        assertEquals(listOf("b", "a", "c"), selection.moveEarlier(1).pages.map { it.sourceUri })
        assertEquals(listOf("a", "c", "b"), selection.moveLater(1).pages.map { it.sourceUri })
        assertEquals(listOf("a", "c"), selection.remove("b").pages.map { it.sourceUri })
    }

    @Test
    fun `stable staged references survive serialization without bitmap state`() {
        val pages = listOf(page("content://provider/a", 42L, stagedPath = "pattern_captures/7/session/page.img"))

        val restored = decodeStagedPatternPages(encodeStagedPatternPages(pages))

        assertEquals(pages, restored)
    }

    @Test
    fun `conversion starts once and reports bounded progress`() {
        val ready = PatternImageImportUiState(selection = PatternImageSelection(listOf(page("a", 1L))))

        val converting = ready.beginConversion()

        assertEquals(PatternImageImportPhase.CONVERTING, converting?.phase)
        assertNull(converting?.beginConversion())
        assertEquals(PatternImageProgress(currentPage = 1, totalPages = 1), converting?.withProgress(1, 1)?.progress)
    }

    private fun page(
        sourceUri: String,
        bytes: Long,
        id: String = sourceUri,
        stagedPath: String = "pattern_captures/7/session/$id.img",
    ) = StagedPatternPage(
        id = id,
        sourceUri = sourceUri,
        stagedPath = stagedPath,
        byteCount = bytes,
        width = 100,
        height = 200,
    )
}

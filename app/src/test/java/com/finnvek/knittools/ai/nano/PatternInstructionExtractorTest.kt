package com.finnvek.knittools.ai.nano

import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatternInstructionExtractorTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `normalizeInstruction strips row prefix`() {
        val extractor = PatternInstructionExtractor()

        val result = extractor.normalizeInstruction(7, "Row 7: K2, P2 across")

        assertEquals("K2, P2 across", result)
    }

    @Test
    fun `normalizeInstruction drops not found values`() {
        val extractor = PatternInstructionExtractor()

        assertNull(extractor.normalizeInstruction(12, "NOT_FOUND"))
        assertNull(extractor.normalizeInstruction(12, "instruction not found on this page"))
    }

    @Test
    fun `same row uses cache`() =
        runTest {
            mockkObject(NanoAvailability)
            coEvery { NanoAvailability.isUsable() } returns true
            var calls = 0
            val extractor =
                PatternInstructionExtractor { _, _ ->
                    calls += 1
                    "Knit across"
                }

            val first = extractor.getInstruction("pattern://one", 0, 1, "Row 1: Knit across")
            val second = extractor.getInstruction("pattern://one", 0, 1, "Row 1: Knit across")

            assertEquals("Knit across", first)
            assertEquals(first, second)
            assertEquals(1, calls)
        }

    @Test
    fun `clearPattern invalidates cached rows only for matching pattern`() =
        runTest {
            mockkObject(NanoAvailability)
            coEvery { NanoAvailability.isUsable() } returns true
            var calls = 0
            val extractor =
                PatternInstructionExtractor { _, row ->
                    calls += 1
                    "Row $row call $calls"
                }

            extractor.getInstruction("pattern://one", 0, 1, "Row 1")
            extractor.getInstruction("pattern://two", 0, 1, "Row 1")
            extractor.clearPattern("pattern://one")

            val refreshed = extractor.getInstruction("pattern://one", 0, 1, "Row 1")
            val untouched = extractor.getInstruction("pattern://two", 0, 1, "Row 1")

            assertEquals("Row 1 call 3", refreshed)
            assertEquals("Row 1 call 2", untouched)
            assertEquals(3, calls)
        }

    @Test
    fun `pageContainsRow returns true when row number exists`() {
        val extractor = PatternInstructionExtractor()
        val pageText = "Row 1: Knit.\nRow 2: K3, YO, K2.\nRow 3: K3, YO, K3."

        assertEquals(true, extractor.pageContainsRow(pageText, 1))
        assertEquals(true, extractor.pageContainsRow(pageText, 2))
        assertEquals(true, extractor.pageContainsRow(pageText, 3))
    }

    @Test
    fun `pageContainsRow returns false when row number does not exist`() {
        val extractor = PatternInstructionExtractor()
        val pageText = "Row 1: Knit.\nRow 2: K3, YO, K2.\nNext row: K2tog, K2tog, K1.\nCast off"

        assertEquals(false, extractor.pageContainsRow(pageText, 20))
        assertEquals(false, extractor.pageContainsRow(pageText, 3))
    }

    @Test
    fun `row not on page returns null without calling runner`() =
        runTest {
            mockkObject(NanoAvailability)
            coEvery { NanoAvailability.isUsable() } returns true
            var calls = 0
            val extractor =
                PatternInstructionExtractor { _, _ ->
                    calls += 1
                    "Should not be used"
                }

            val result = extractor.getInstruction("pattern://one", 0, 22, "Row 1: Knit.\nNext row: K2tog.")

            assertNull(result)
            assertEquals(0, calls)
        }

    @Test
    fun `nano unavailable returns null without calling runner`() =
        runTest {
            mockkObject(NanoAvailability)
            coEvery { NanoAvailability.isUsable() } returns false
            var calls = 0
            val extractor =
                PatternInstructionExtractor { _, _ ->
                    calls += 1
                    "Should not be used"
                }

            val result = extractor.getInstruction("pattern://one", 0, 5, "Row 5: Knit")

            assertNull(result)
            assertEquals(0, calls)
        }
}

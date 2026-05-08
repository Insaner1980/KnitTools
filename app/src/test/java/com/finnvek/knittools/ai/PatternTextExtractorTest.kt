package com.finnvek.knittools.ai

import android.graphics.Bitmap
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatternTextExtractorTest {
    @Test
    fun `same page uses cache`() =
        runTest {
            var calls = 0
            val extractor =
                PatternTextExtractor { _ ->
                    calls += 1
                    "Row 1: Knit across"
                }
            val bitmap = mockk<Bitmap>()

            val first = extractor.getPageText("pattern://one", 0, bitmap)
            val second = extractor.getPageText("pattern://one", 0, bitmap)

            assertEquals("Row 1: Knit across", first)
            assertEquals(first, second)
            assertEquals(1, calls)
        }

    @Test
    fun `clearPattern removes only matching cache entries`() =
        runTest {
            var calls = 0
            val extractor =
                PatternTextExtractor { _ ->
                    calls += 1
                    "Text $calls"
                }
            val bitmap = mockk<Bitmap>()

            extractor.getPageText("pattern://one", 0, bitmap)
            extractor.getPageText("pattern://two", 0, bitmap)
            extractor.clearPattern("pattern://one")
            val refreshed = extractor.getPageText("pattern://one", 0, bitmap)
            val untouched = extractor.getPageText("pattern://two", 0, bitmap)

            assertEquals("Text 3", refreshed)
            assertEquals("Text 2", untouched)
            assertEquals(3, calls)
        }
}

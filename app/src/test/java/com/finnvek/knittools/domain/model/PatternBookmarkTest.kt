package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternBookmarkTest {
    @Test
    fun `name validation rejects empty and whitespace-only names`() {
        assertEquals(PatternBookmarkNameValidation.Empty, validatePatternBookmarkName(""))
        assertEquals(PatternBookmarkNameValidation.Empty, validatePatternBookmarkName("   \n"))
    }

    @Test
    fun `name validation trims and accepts the 50 character boundary`() {
        val boundaryName = "x".repeat(PATTERN_BOOKMARK_NAME_MAX_LENGTH)

        assertEquals(
            PatternBookmarkNameValidation.Valid("Sleeve"),
            validatePatternBookmarkName("  Sleeve  "),
        )
        assertEquals(
            PatternBookmarkNameValidation.Valid(boundaryName),
            validatePatternBookmarkName(boundaryName),
        )
    }

    @Test
    fun `name validation rejects over-limit names and does not reserve duplicate names`() {
        assertEquals(
            PatternBookmarkNameValidation.TooLong,
            validatePatternBookmarkName("x".repeat(PATTERN_BOOKMARK_NAME_MAX_LENGTH + 1)),
        )
        assertTrue(validatePatternBookmarkName("Body") is PatternBookmarkNameValidation.Valid)
        assertTrue(validatePatternBookmarkName("Body") is PatternBookmarkNameValidation.Valid)
    }
}

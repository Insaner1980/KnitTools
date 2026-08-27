package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternAvailabilityTest {
    @Test
    fun `stable values round trip through backend and persistence parsers`() {
        PatternAvailability.entries.forEach { availability ->
            assertEquals(
                availability,
                PatternAvailability.fromBackendValue(availability.persistedValue),
            )
            assertEquals(
                availability,
                PatternAvailability.fromPersistedValue(availability.persistedValue),
            )
        }
    }

    @Test
    fun `missing or malformed values safely become unknown`() {
        listOf(null, "", " ", "PAID", "subscription").forEach { value ->
            assertEquals(PatternAvailability.Unknown, PatternAvailability.fromBackendValue(value))
            assertEquals(PatternAvailability.Unknown, PatternAvailability.fromPersistedValue(value))
        }
    }

    @Test
    fun `is free is derived only from the free state`() {
        assertTrue(PatternAvailability.Free.isFree)
        assertFalse(PatternAvailability.Paid.isFree)
        assertFalse(PatternAvailability.Unknown.isFree)
    }
}

package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelCoverageTest {
    @Test
    fun `yarn card display name combines available brand and yarn fields`() {
        val fallback: (Long) -> String = { id -> "Yarn $id" }

        assertEquals(
            "Novita Nalle",
            YarnCard(id = 1L, brand = "Novita", yarnName = "Nalle").displayName(fallback),
        )
        assertEquals("Nalle", YarnCard(id = 2L, yarnName = "Nalle").displayName(fallback))
        assertEquals("Novita", YarnCard(id = 3L, brand = "Novita").displayName(fallback))
        assertEquals("Yarn 4", YarnCard(id = 4L, brand = " ", yarnName = "").displayName(fallback))
    }

    @Test
    fun `saved pattern source and compatibility fields keep legacy fallbacks`() {
        assertEquals(SavedPatternSource.Ravelry, SavedPatternSource.fromPersistedValue("RAVELRY"))
        assertEquals(SavedPatternSource.LocalFile, SavedPatternSource.fromPersistedValue("LOCAL_FILE"))
        assertEquals(SavedPatternSource.Unknown, SavedPatternSource.fromPersistedValue("missing"))

        assertEquals(42, savedPattern(ravelryPatternId = 42).ravelryId)
        assertEquals(0, savedPattern(ravelryPatternId = null).ravelryId)
        assertEquals("content://pattern.pdf", savedPattern(localPdfUri = "content://pattern.pdf").patternUrl)
        assertEquals(
            "https://example.com/canonical",
            savedPattern(
                canonicalUrl = "https://example.com/canonical",
                originalUrl = "https://example.com/original",
            ).patternUrl,
        )
        assertEquals(
            "https://example.com/original",
            savedPattern(originalUrl = "https://example.com/original").patternUrl,
        )
        assertEquals(
            "https://example.com/canonical",
            savedPattern(localPdfUri = " ", canonicalUrl = "https://example.com/canonical").patternUrl,
        )
    }

    @Test
    fun `persisted enum values fall back to current defaults`() {
        assertEquals(ProjectSortOrder.UPDATED, ProjectSortOrder.fromPersistedValue("updated"))
        assertEquals(ProjectSortOrder.NAME, ProjectSortOrder.fromPersistedValue("name"))
        assertEquals(ProjectSortOrder.CREATED, ProjectSortOrder.fromPersistedValue("created"))
        assertEquals(ProjectSortOrder.DEFAULT, ProjectSortOrder.fromPersistedValue(null))
        assertEquals(ProjectSortOrder.DEFAULT, ProjectSortOrder.fromPersistedValue("unknown"))

        assertEquals(CraftType.KNITTING, CraftType.fromPersistedValue("KNITTING"))
        assertEquals(CraftType.CROCHET, CraftType.fromPersistedValue("CROCHET"))
        assertEquals(CraftType.KNITTING, CraftType.fromPersistedValue("unknown"))
        assertEquals(MainCounterLabelType.ROWS, CraftType.KNITTING.defaultMainCounterLabelType())
        assertEquals(MainCounterLabelType.ROUNDS, CraftType.CROCHET.defaultMainCounterLabelType())

        assertEquals(MainCounterLabelType.REPEATS, MainCounterLabelType.fromPersistedValue("REPEATS"))
        assertEquals(MainCounterLabelType.ROWS, MainCounterLabelType.fromPersistedValue(null))

        assertEquals(ProjectCounterType.COUNT_UP, ProjectCounterType.fromPersistedValue("unknown"))
    }

    @Test
    fun `main counter custom labels are sanitized before resolving type`() {
        val longLabel = "x".repeat(MAIN_COUNTER_CUSTOM_LABEL_MAX_LENGTH + 3)

        assertEquals("Rounds", sanitizeMainCounterCustomLabel("  Rounds  "))
        assertEquals("x".repeat(MAIN_COUNTER_CUSTOM_LABEL_MAX_LENGTH), sanitizeMainCounterCustomLabel(longLabel))
        assertNull(sanitizeMainCounterCustomLabel("   "))
        assertNull(sanitizeMainCounterCustomLabel(null))

        assertEquals(
            MainCounterLabelType.CUSTOM,
            resolvedMainCounterLabelType(CraftType.KNITTING, MainCounterLabelType.CUSTOM, "Rows"),
        )
        assertEquals(
            MainCounterLabelType.ROUNDS,
            resolvedMainCounterLabelType(CraftType.CROCHET, MainCounterLabelType.CUSTOM, " "),
        )
        assertEquals(
            MainCounterLabelType.REPEATS,
            resolvedMainCounterLabelType(CraftType.CROCHET, MainCounterLabelType.REPEATS, null),
        )
    }

    @Test
    fun `yarn card statuses normalize unsupported values to stash`() {
        assertTrue(YarnCardStatus.isSupported(YarnCardStatus.IN_STASH))
        assertTrue(YarnCardStatus.isSupported(YarnCardStatus.IN_USE))
        assertTrue(YarnCardStatus.isSupported(YarnCardStatus.FINISHED))
        assertFalse(YarnCardStatus.isSupported("ARCHIVED"))

        assertEquals(YarnCardStatus.IN_USE, YarnCardStatus.normalize(YarnCardStatus.IN_USE))
        assertEquals(YarnCardStatus.IN_STASH, YarnCardStatus.normalize("ARCHIVED"))
    }

    private fun savedPattern(
        ravelryPatternId: Int? = null,
        originalUrl: String = "",
        canonicalUrl: String = "",
        localPdfUri: String? = null,
    ): SavedPattern =
        SavedPattern(
            source = SavedPatternSource.Ravelry,
            ravelryPatternId = ravelryPatternId,
            name = "Pattern",
            designerName = "Designer",
            originalUrl = originalUrl,
            canonicalUrl = canonicalUrl,
            localPdfUri = localPdfUri,
        )
}

package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectDocumentTest {
    @Test
    fun `label validation trims and accepts the 50 character boundary`() {
        val boundary = "x".repeat(PROJECT_DOCUMENT_LABEL_MAX_LENGTH)

        assertEquals(
            ProjectDocumentLabelValidation.Valid("Sleeve chart"),
            validateProjectDocumentLabel("  Sleeve chart  "),
        )
        assertEquals(
            ProjectDocumentLabelValidation.Valid(boundary),
            validateProjectDocumentLabel(boundary),
        )
    }

    @Test
    fun `label validation rejects empty whitespace and over-limit values`() {
        assertEquals(ProjectDocumentLabelValidation.Empty, validateProjectDocumentLabel(""))
        assertEquals(ProjectDocumentLabelValidation.Empty, validateProjectDocumentLabel(" \n "))
        assertEquals(
            ProjectDocumentLabelValidation.TooLong,
            validateProjectDocumentLabel("x".repeat(PROJECT_DOCUMENT_LABEL_MAX_LENGTH + 1)),
        )
    }

    @Test
    fun `duplicate labels remain valid`() {
        assertTrue(validateProjectDocumentLabel("Chart") is ProjectDocumentLabelValidation.Valid)
        assertTrue(validateProjectDocumentLabel("Chart") is ProjectDocumentLabelValidation.Valid)
    }

    @Test
    fun `persisted values are sanitized without changing stable identity`() {
        val document =
            projectDocument(
                id = 8,
                sortOrder = -4,
                currentPage = -2,
                readingLineYFraction = -1f,
                verticalReadingGuideXFraction = 4f,
            ).sanitized()

        assertEquals(8L, document.id)
        assertEquals("document-8", document.documentKey)
        assertEquals(0, document.sortOrder)
        assertEquals(0, document.currentPage)
        assertEquals(READING_LINE_MIN_Y_FRACTION, document.readingLineYFraction)
        assertEquals(READING_LINE_MAX_Y_FRACTION, document.verticalReadingGuideXFraction)
    }

    @Test
    fun `ordering and primary fallback use sort order then stable id`() {
        val documents =
            listOf(
                projectDocument(id = 9, sortOrder = 1),
                projectDocument(id = 7, sortOrder = 1),
                projectDocument(id = 11, sortOrder = 0),
            )

        assertEquals(listOf(11L, 7L, 9L), documents.inDocumentOrder().map { it.id })
        assertEquals(11L, documents.primaryFallback()?.id)
    }

    @Test
    fun `opening decision rejects an unavailable document`() {
        assertTrue(projectDocument(id = 1).canOpen(isFileAvailable = true))
        assertFalse(projectDocument(id = 1).canOpen(isFileAvailable = false))
    }

    private fun projectDocument(
        id: Long,
        sortOrder: Int = 0,
        currentPage: Int = 0,
        readingLineYFraction: Float = DEFAULT_READING_LINE_Y_FRACTION,
        verticalReadingGuideXFraction: Float = DEFAULT_READING_GUIDE_FRACTION,
    ): ProjectDocument =
        ProjectDocument(
            id = id,
            projectId = 3,
            savedPatternId = null,
            documentKey = "document-$id",
            label = "Pattern $id",
            localPdfUri = "content://patterns/$id.pdf",
            sortOrder = sortOrder,
            isPrimary = false,
            currentPage = currentPage,
            rowMapping = null,
            readingLineEnabled = false,
            readingLineYFraction = readingLineYFraction,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = verticalReadingGuideXFraction,
            createdAt = 1_000,
            updatedAt = 2_000,
        )
}

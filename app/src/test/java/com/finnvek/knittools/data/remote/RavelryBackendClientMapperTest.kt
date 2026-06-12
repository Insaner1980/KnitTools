package com.finnvek.knittools.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RavelryBackendClientMapperTest {
    @Test
    fun `maps sanitized backend search response to existing Android search model`() {
        val response =
            RavelryBackendMappers.searchResponseFrom(
                mapOf(
                    "patterns" to
                        listOf(
                            mapOf(
                                "ravelryPatternId" to 42,
                                "title" to "Cozy Hat",
                                "designerName" to "Ada Designer",
                                "thumbnailUrl" to "https://images.example/hat.jpg",
                                "canonicalUrl" to "https://www.ravelry.com/patterns/library/cozy-hat",
                                "availability" to "free",
                                "pdf_url" to "https://private.example/pattern.pdf",
                            ),
                        ),
                    "pagination" to
                        mapOf(
                            "page" to 2,
                            "pageCount" to 5,
                            "resultCount" to 84,
                        ),
                ),
            )

        val pattern = response.patterns.single()

        assertEquals(1, response.patterns.size)
        assertEquals(42, pattern.id)
        assertEquals("Cozy Hat", pattern.name)
        assertEquals("Ada Designer", pattern.designer?.name)
        assertEquals("https://images.example/hat.jpg", pattern.firstPhoto?.mediumUrl)
        assertEquals("cozy-hat", pattern.permalink)
        assertEquals(true, pattern.free)
        assertEquals(2, response.paginator?.page)
        assertEquals(5, response.paginator?.pageCount)
        assertEquals(84, response.paginator?.results)
        assertFalse(response.toString().contains("pdf_url"))
    }

    @Test
    fun `maps sanitized imported pattern to minimal Android detail model`() {
        val detail =
            RavelryBackendMappers.patternDetailFrom(
                mapOf(
                    "ravelryPatternId" to 42,
                    "title" to "Cozy Hat",
                    "designerName" to "Ada Designer",
                    "thumbnailUrl" to "https://images.example/hat.jpg",
                    "canonicalUrl" to "https://www.ravelry.com/patterns/library/cozy-hat",
                    "availability" to "paid",
                    "originalUrl" to "https://www.ravelry.com/patterns/library/cozy-hat?utm_source=share",
                    "pdf_url" to "https://private.example/pattern.pdf",
                ),
            )

        assertEquals(42, detail.id)
        assertEquals("Cozy Hat", detail.name)
        assertEquals("Ada Designer", detail.designer?.name)
        assertEquals("https://images.example/hat.jpg", detail.mainPhotoUrl)
        assertEquals("cozy-hat", detail.permalink)
        assertEquals(false, detail.free)
        assertFalse(detail.toString().contains("pdf_url"))
    }
}

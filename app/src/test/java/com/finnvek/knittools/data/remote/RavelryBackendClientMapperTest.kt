package com.finnvek.knittools.data.remote

import com.finnvek.knittools.domain.model.PatternAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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
        assertEquals(PatternAvailability.Free, pattern.availability)
        assertEquals(2, response.paginator?.page)
        assertEquals(5, response.paginator?.pageCount)
        assertEquals(84, response.paginator?.results)
        assertFalse(response.toString().contains("pdf_url"))
    }

    @Test
    fun `maps unknown availability without treating it as paid`() {
        val response =
            RavelryBackendMappers.searchResponseFrom(
                mapOf(
                    "patterns" to
                        listOf(
                            mapOf(
                                "ravelryPatternId" to 43,
                                "title" to "Mystery Mitts",
                                "designerName" to "",
                                "canonicalUrl" to "https://www.ravelry.com/patterns/library/mystery-mitts",
                                "availability" to "unknown",
                            ),
                        ),
                ),
            )

        val pattern = response.patterns.single()

        assertEquals(PatternAvailability.Unknown, pattern.availability)
        assertEquals(false, pattern.free)
    }

    @Test
    fun `maps malformed transport availability to unknown`() {
        val response =
            RavelryBackendMappers.searchResponseFrom(
                mapOf(
                    "patterns" to
                        listOf(
                            mapOf(
                                "ravelryPatternId" to 44,
                                "title" to "Malformed Mitts",
                                "availability" to "subscription",
                            ),
                        ),
                ),
            )

        val pattern = response.patterns.single()

        assertEquals(PatternAvailability.Unknown, pattern.availability)
        assertEquals(false, pattern.free)
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
        assertEquals(PatternAvailability.Paid, detail.availability)
        assertEquals("https://www.ravelry.com/patterns/library/cozy-hat", detail.canonicalUrl)
        assertEquals("https://www.ravelry.com/patterns/library/cozy-hat?utm_source=share", detail.originalUrl)
        assertFalse(detail.toString().contains("pdf_url"))
    }

    @Test
    fun `rejects imported pattern detail when backend omits ravelry pattern id`() {
        val error =
            assertThrows(RavelryHttpException::class.java) {
                RavelryBackendMappers.patternDetailFrom(
                    mapOf(
                        "title" to "Cozy Hat",
                        "designerName" to "Ada Designer",
                        "canonicalUrl" to "https://www.ravelry.com/patterns/library/cozy-hat",
                        "availability" to "free",
                    ),
                )
            }

        assertEquals("Missing ravelryPatternId in Ravelry pattern detail response", error.message)
    }

    @Test
    fun `rejects non-positive fractional and overflowing detail ids`() {
        listOf(0, -1, 42.5, Int.MAX_VALUE.toLong() + 1L).forEach { invalidId ->
            assertThrows(RavelryHttpException::class.java) {
                RavelryBackendMappers.patternDetailFrom(
                    mapOf(
                        "ravelryPatternId" to invalidId,
                        "title" to "Invalid ID",
                    ),
                )
            }
        }
    }

    @Test
    fun `drops search results with invalid ids without coercion`() {
        val response =
            RavelryBackendMappers.searchResponseFrom(
                mapOf(
                    "patterns" to
                        listOf(
                            mapOf("ravelryPatternId" to 0, "title" to "Zero"),
                            mapOf("ravelryPatternId" to 42.5, "title" to "Fractional"),
                            mapOf("ravelryPatternId" to 43, "title" to "Valid"),
                        ),
                ),
            )

        assertEquals(listOf(43), response.patterns.map { it.id })
    }
}

package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPatternTextTest {
    @Test
    fun `title is trimmed and preserves capitalization`() {
        val result = validateWebPatternTitle("  Cozy Cardigan  ")

        assertEquals("Cozy Cardigan", (result as WebPatternTitleValidation.Valid).value)
    }

    @Test
    fun `title rejects blank unsafe characters and values over 120 units`() {
        listOf(
            "   ",
            "Line\nbreak",
            "Tabbed\tname",
            "Control\u0000name",
            "Line\u2028separator",
            "Paragraph\u2029separator",
            "Bidi\u202Ename",
            "a".repeat(WEB_PATTERN_TEXT_MAX_LENGTH + 1),
        ).forEach { value ->
            assertTrue(validateWebPatternTitle(value) is WebPatternTitleValidation.Invalid)
        }
    }

    @Test
    fun `designer is optional trimmed and uses the same safe boundary`() {
        assertEquals("", validDesigner("   "))
        assertEquals("Jane Doe", validDesigner("  Jane Doe  "))
        listOf(
            "Line\nbreak",
            "Tabbed\tname",
            "Control\u0000name",
            "Line\u2028separator",
            "Paragraph\u2029separator",
            "Bidi\u2066name",
            "a".repeat(WEB_PATTERN_TEXT_MAX_LENGTH + 1),
        ).forEach { value ->
            assertTrue(validateWebPatternDesigner(value) is WebPatternDesignerValidation.Invalid)
        }
    }

    @Test
    fun `title and designer accept exactly 120 units`() {
        val boundary = "a".repeat(WEB_PATTERN_TEXT_MAX_LENGTH)

        assertEquals(boundary, (validateWebPatternTitle(boundary) as WebPatternTitleValidation.Valid).value)
        assertEquals(boundary, (validateWebPatternDesigner(boundary) as WebPatternDesignerValidation.Valid).value)
    }

    @Test
    fun `WEB_LINK has a stable persisted value`() {
        assertEquals("WEB_LINK", SavedPatternSource.WebLink.persistedValue)
        assertEquals(SavedPatternSource.WebLink, SavedPatternSource.fromPersistedValue("WEB_LINK"))
    }

    @Test
    fun `web source and compatible legacy OTHER expose one validated URL seam`() {
        val web =
            pattern(
                source = SavedPatternSource.WebLink,
                originalUrl = "https://Example.com/Pattern",
            )
        val legacy =
            pattern(
                source = SavedPatternSource.Other,
                originalUrl = "https://example.com/legacy",
            )

        assertTrue(web.isWebPatternCompatible)
        assertEquals("example.com", web.webPatternUrlOrNull?.host)
        assertTrue(legacy.isWebPatternCompatible)
        assertEquals("https://example.com/legacy", legacy.webPatternUrlOrNull?.originalUrl)
    }

    @Test
    fun `legacy boundary rejects local invalid and Ravelry OTHER records`() {
        val local =
            pattern(
                source = SavedPatternSource.Other,
                originalUrl = "https://example.com/pattern",
                localPdfUri = "content://pattern.pdf",
            )
        val invalid = pattern(source = SavedPatternSource.Other, originalUrl = "not a URL")
        val ravelry =
            pattern(
                source = SavedPatternSource.Other,
                originalUrl = "https://www.ravelry.com/patterns/library/cozy-hat",
            )

        listOf(local, invalid, ravelry).forEach { savedPattern ->
            assertFalse(savedPattern.isWebPatternCompatible)
            assertNull(savedPattern.webPatternUrlOrNull)
        }
    }

    @Test
    fun `share parser accepts a plain URL and ordinary surrounding punctuation`() {
        val plain = parseWebPatternSharedText("https://example.com/Pattern?Size=XL#Notes", null)
        val surrounded =
            parseWebPatternSharedText(
                "Try this pattern (https://example.com/cozy-cardigan).",
                "  Cozy Cardigan  ",
            )

        assertEquals(
            "https://example.com/Pattern?Size=XL#Notes",
            (plain as WebPatternShareParseResult.WebLink).url.originalUrl,
        )
        surrounded as WebPatternShareParseResult.WebLink
        assertEquals("https://example.com/cozy-cardigan", surrounded.url.originalUrl)
        assertEquals("Cozy Cardigan", surrounded.titleSuggestion)
    }

    @Test
    fun `share punctuation trimming preserves unambiguous URL content`() {
        val plainPath = parseWebPatternSharedText("https://example.com/pattern!", null)
        val queryAndFragment =
            parseWebPatternSharedText(
                "Try https://example.com/pattern?note=important!#part!",
                null,
            )
        val balancedPath =
            parseWebPatternSharedText(
                "Try https://example.com/pattern_(knit).",
                null,
            )

        assertEquals(
            "https://example.com/pattern!",
            (plainPath as WebPatternShareParseResult.WebLink).url.originalUrl,
        )
        assertEquals(
            "https://example.com/pattern?note=important!#part!",
            (queryAndFragment as WebPatternShareParseResult.WebLink).url.originalUrl,
        )
        assertEquals(
            "https://example.com/pattern_(knit)",
            (balancedPath as WebPatternShareParseResult.WebLink).url.originalUrl,
        )
    }

    @Test
    fun `share parser deduplicates repeated canonical URL occurrences`() {
        val result =
            parseWebPatternSharedText(
                "https://EXAMPLE.com:443/pattern and https://example.com/pattern",
                null,
            )

        assertTrue(result is WebPatternShareParseResult.WebLink)
    }

    @Test
    fun `share parser rejects multiple distinct URLs including mixed Ravelry`() {
        assertTrue(
            parseWebPatternSharedText(
                "https://example.com/one https://example.com/two",
                null,
            ) is WebPatternShareParseResult.Ambiguous,
        )
        assertTrue(
            parseWebPatternSharedText(
                "https://www.ravelry.com/patterns/library/cozy-hat https://example.com/other",
                null,
            ) is WebPatternShareParseResult.Ambiguous,
        )
    }

    @Test
    fun `share parser preserves Ravelry precedence`() {
        val result =
            parseWebPatternSharedText(
                "Pattern: https://www.ravelry.com/patterns/library/cozy-hat?buy=1",
                "Cozy Hat",
            )

        result as WebPatternShareParseResult.Ravelry
        assertEquals("https://www.ravelry.com/patterns/library/cozy-hat?buy=1", result.url.originalUrl)
        assertEquals("Cozy Hat", result.titleSuggestion)
    }

    @Test
    fun `share parser distinguishes empty invalid and oversized input`() {
        assertTrue(parseWebPatternSharedText("  ", null) is WebPatternShareParseResult.Empty)
        assertTrue(
            parseWebPatternSharedText("ftp://example.com/pattern", null) is WebPatternShareParseResult.Invalid,
        )
        assertTrue(
            parseWebPatternSharedText("https://example.com/%GG", null) is WebPatternShareParseResult.Invalid,
        )
        assertTrue(
            parseWebPatternSharedText("a".repeat(WEB_PATTERN_SHARED_TEXT_MAX_LENGTH + 1), null) is
                WebPatternShareParseResult.TooLong,
        )
    }

    @Test
    fun `unsafe blank and overlong subjects yield no title suggestion`() {
        val blank = sharedWebLink(subject = "   ")
        val unsafe = sharedWebLink(subject = "Bad\nTitle")
        val overlong = sharedWebLink(subject = "a".repeat(WEB_PATTERN_TEXT_MAX_LENGTH + 1))

        assertEquals("", blank.titleSuggestion)
        assertEquals("", unsafe.titleSuggestion)
        assertEquals("", overlong.titleSuggestion)
    }

    private fun validDesigner(value: String): String =
        (validateWebPatternDesigner(value) as WebPatternDesignerValidation.Valid).value

    private fun sharedWebLink(subject: String): WebPatternShareParseResult.WebLink =
        parseWebPatternSharedText("https://example.com/pattern", subject) as WebPatternShareParseResult.WebLink

    private fun pattern(
        source: SavedPatternSource,
        originalUrl: String,
        localPdfUri: String? = null,
    ): SavedPattern =
        SavedPattern(
            source = source,
            name = "Pattern",
            designerName = "",
            originalUrl = originalUrl,
            canonicalUrl = originalUrl,
            localPdfUri = localPdfUri,
        )
}

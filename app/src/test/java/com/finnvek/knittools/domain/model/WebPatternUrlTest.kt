package com.finnvek.knittools.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPatternUrlTest {
    @Test
    fun `accepts http and https and preserves trimmed original url`() {
        val https = validUrl("  https://example.com/pattern  ")
        val http = validUrl("http://example.com/pattern")

        assertEquals("https://example.com/pattern", https.originalUrl)
        assertTrue(https.isSecure)
        assertFalse(http.isSecure)
    }

    @Test
    fun `canonical form lowercases scheme and IDNA host only`() {
        val value = validUrl("HTTPS://Münich.Example/Pattern/Ä?Size=XL#Notes")

        assertEquals("xn--mnich-kva.example", value.host)
        assertEquals(
            "https://xn--mnich-kva.example/Pattern/Ä?Size=XL#Notes",
            value.canonicalUrl,
        )
    }

    @Test
    fun `already encoded IDNA host uses the same canonical form`() {
        assertEquals(
            validUrl("https://Münich.Example/pattern").canonicalUrl,
            validUrl("https://XN--MNICH-KVA.EXAMPLE/pattern").canonicalUrl,
        )
    }

    @Test
    fun `canonical form removes default ports and normalizes empty root path`() {
        assertEquals("http://example.com/", validUrl("http://example.com:80").canonicalUrl)
        assertEquals("https://example.com/", validUrl("https://example.com:443/").canonicalUrl)
        assertEquals("https://example.com:8443/", validUrl("https://example.com:8443").canonicalUrl)
        assertEquals(
            validUrl("HTTPS://EXAMPLE.COM:443").canonicalUrl,
            validUrl("https://example.com/").canonicalUrl,
        )
    }

    @Test
    fun `canonical form preserves non-root path query fragment and percent encoding`() {
        val value = validUrl("https://example.com/Pattern/%C3%84/?Size=XL#Part-2")

        assertEquals(
            "https://example.com/Pattern/%C3%84/?Size=XL#Part-2",
            value.canonicalUrl,
        )
    }

    @Test
    fun `case-sensitive paths queries and non-root trailing slashes stay distinct`() {
        val upperPath = validUrl("https://example.com/Pattern?Size=XL").canonicalUrl
        val lowerPath = validUrl("https://example.com/pattern?size=XL").canonicalUrl
        val trailingSlash = validUrl("https://example.com/Pattern/?Size=XL").canonicalUrl

        assertTrue(upperPath != lowerPath)
        assertTrue(upperPath != trailingSlash)
    }

    @Test
    fun `recognizes only existing Ravelry pattern URL ownership`() {
        assertTrue(validUrl("https://www.ravelry.com/patterns/library/cozy-hat").isRavelryPattern)
        assertTrue(validUrl("https://ravelry.com/patterns/library/cozy-hat?buy=1").isRavelryPattern)
        assertTrue(validUrl("https://www.ravelry.com/patterns/%6cibrary/cozy-hat").isRavelryPattern)
        val encodedPath = validUrl("https://www.ravelry.com/patterns%2Flibrary/cozy-hat")
        assertTrue(encodedPath.isRavelryPattern)
        assertEquals(
            "https://www.ravelry.com/patterns%2Flibrary/cozy-hat",
            encodedPath.canonicalUrl,
        )
        assertFalse(validUrl("https://www.ravelry.com/patterns/search").isRavelryPattern)
        assertFalse(validUrl("http://www.ravelry.com/patterns/library/cozy-hat").isRavelryPattern)
    }

    @Test
    fun `rejects missing relative and unsupported schemes`() {
        listOf(
            "example.com/pattern",
            "/patterns/cardigan",
            "javascript:alert(1)",
            "data:text/plain,test",
            "file:///pattern.pdf",
            "content://patterns/1",
            "intent://pattern",
            "ftp://example.com/pattern",
            "knittools://pattern",
            "https:///pattern",
            "https://:443/pattern",
        ).forEach(::assertInvalid)
    }

    @Test
    fun `rejects credentials malformed ports escapes and whitespace`() {
        listOf(
            "https://user@example.com/pattern",
            "https://user:password" + "@example.com/pattern",
            "https://example.com:0/pattern",
            "https://example.com:65536/pattern",
            "https://example.com:/pattern",
            "https://example.com/%",
            "https://example.com/%2",
            "https://example.com/%GG",
            "https://example.com/a b",
            "https://example.com/a\tb",
            "https://example.com/a\nb",
            "https://example.com/a\u0000b",
            "https://example.com/a\u2028b",
            "https://example.com/a\u2029b",
            "https://example.com/a\u202Eb",
        ).forEach(::assertInvalid)
    }

    @Test
    fun `rejects invalid local and private hosts without network lookup`() {
        listOf(
            "https://localhost/pattern",
            "https://printer.local/pattern",
            "https://localhost./pattern",
            "https://printer.local./pattern",
            "https://127.0.0.1/pattern",
            "https://0177.0.0.1/pattern",
            "https://0x7f.0.0.1/pattern",
            "https://2130706433/pattern",
            "https://0.0.0.0/pattern",
            "https://10.0.0.1/pattern",
            "https://100.64.0.1/pattern",
            "https://169.254.1.1/pattern",
            "https://172.16.0.1/pattern",
            "https://192.168.1.1/pattern",
            "https://192.0.2.1/pattern",
            "https://198.51.100.1/pattern",
            "https://203.0.113.1/pattern",
            "https://224.0.0.1/pattern",
            "https://240.0.0.1/pattern",
            "https://[::]/pattern",
            "https://[::1]/pattern",
            "https://[fe80::1]/pattern",
            "https://[fc00::1]/pattern",
            "https://[ff02::1]/pattern",
            "https://[2001:db8::1]/pattern",
            "https://[2001:2::1]/pattern",
            "https://[4000::1]/pattern",
        ).forEach(::assertInvalid)
    }

    @Test
    fun `accepts deterministic public host and IP literals`() {
        assertEquals("example.com", validUrl("https://example.com/pattern").host)
        assertEquals("8.8.8.8", validUrl("https://8.8.8.8/pattern").host)
        assertEquals(
            "https://[2606:4700:4700:0:0:0:0:1111]/pattern",
            validUrl("https://[2606:4700:4700::1111]/pattern").canonicalUrl,
        )
    }

    @Test
    fun `rejects invalid IDNA hostname and overlong URL`() {
        assertInvalid("https://bad_host.example/pattern")
        assertInvalid("https://example.com./pattern")
        assertInvalid("https://example.com/" + "a".repeat(2_100))
    }

    private fun validUrl(input: String): WebPatternUrl {
        val result = validateWebPatternUrl(input)
        assertTrue("Expected valid URL but got $result for $input", result is WebPatternUrlValidation.Valid)
        return (result as WebPatternUrlValidation.Valid).value
    }

    private fun assertInvalid(input: String) {
        assertTrue(
            "Expected invalid URL for $input",
            validateWebPatternUrl(input) is WebPatternUrlValidation.Invalid,
        )
    }
}

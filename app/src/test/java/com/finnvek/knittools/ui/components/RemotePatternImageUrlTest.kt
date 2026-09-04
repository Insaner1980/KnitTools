package com.finnvek.knittools.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemotePatternImageUrlTest {
    @Test
    fun `null blank and unsupported URLs are rejected`() {
        listOf(
            null,
            "",
            "   ",
            "http://images.example.test/pattern.png",
            "file:///tmp/pattern.png",
            "content://images/pattern.png",
            "data:image/png;base64,AAAA",
            "//images.example.test/pattern.png",
            "pattern.png",
            "https://user:password" + "@images.example.test/pattern.png",
            "https://images.example.test/a b.png",
            "https://images.example.test/" + "a".repeat(2_100),
        ).forEach { url ->
            assertNull(normalizeRemotePatternImageUrl(url))
        }
    }

    @Test
    fun `HTTPS URL is trimmed and preserved`() {
        assertEquals(
            "https://images.example.test/pattern.png?size=large",
            normalizeRemotePatternImageUrl(
                "  https://images.example.test/pattern.png?size=large  ",
            ),
        )
    }

    @Test
    fun `malformed HTTPS URLs and missing hosts are rejected`() {
        listOf(
            "https:///pattern.png",
            "https://?image=pattern.png",
            "https://images.example.test/%zz",
            "https://bad_host.example.test/pattern.png",
            "https://images.example.test:65536/pattern.png",
        ).forEach { url ->
            assertNull(normalizeRemotePatternImageUrl(url))
        }
    }

    @Test
    fun `HTTPS scheme matching is case insensitive`() {
        assertEquals(
            "HTTPS://images.example.test/pattern.png",
            normalizeRemotePatternImageUrl("HTTPS://images.example.test/pattern.png"),
        )
    }
}

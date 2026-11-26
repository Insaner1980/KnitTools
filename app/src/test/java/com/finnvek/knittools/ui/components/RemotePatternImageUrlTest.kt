package com.finnvek.knittools.ui.components

import com.finnvek.knittools.domain.model.normalizeRemotePatternImageUrl
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
            "https://images4-f.ravelrycache.com/pattern.png?size=large",
            normalizeRemotePatternImageUrl(
                "  https://images4-f.ravelrycache.com/pattern.png?size=large  ",
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
            "https://images.example.test/pattern.png",
            "https://127.0.0.1/image",
            "https://[::1]/image",
            "https://printer.local/image",
            "https://images4-f.ravelrycache.com.example.org/image",
            "https://images4-f.ravelrycache.com:8443/image",
            "https://user@images4-f.ravelrycache.com/image",
            "https://images4-f.ravelrycache.com@localhost/image",
            "https://images4-f.ravelrycache.com:/image",
        ).forEach { url ->
            assertNull(normalizeRemotePatternImageUrl(url))
        }
    }

    @Test
    fun `HTTPS scheme matching is case insensitive`() {
        assertEquals(
            "HTTPS://IMAGES4-H.RAVELRYCACHE.COM:443/pattern.png",
            normalizeRemotePatternImageUrl("HTTPS://IMAGES4-H.RAVELRYCACHE.COM:443/pattern.png"),
        )
    }
}

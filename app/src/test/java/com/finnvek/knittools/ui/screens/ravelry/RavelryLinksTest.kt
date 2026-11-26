package com.finnvek.knittools.ui.screens.ravelry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryLinksTest {
    @Test
    fun `restoration accepts eligible pages but rejects checkout redirects credentials and oversized metadata`() {
        val page = "https://www.ravelry.com/patterns/library/cozy-hat"
        assertEquals(page, restorableRavelryPageOrNull(page))
        val subdomain = "https://forums.ravelry.com/topics/knitting"
        assertEquals(subdomain, eligibleRavelryPageOrNull(subdomain))
        assertEquals(subdomain, restorableRavelryPageOrNull(subdomain))
        assertNull(restorableRavelryPageOrNull("https://forums.ravelry.com.evil.example/topics"))
        listOf(
            null,
            "not a url",
            "http://www.ravelry.com/patterns/search",
            "https://www.ravelry.com/carts/123/checkout",
            "https://checkout.ravelry.com/",
            "https://www.paypal.com/checkoutnow",
            "https://downloads.example-cdn.com/pattern.pdf",
            "intent://www.ravelry.com/",
            "https://user:password@www.ravelry.com/",
            "$page?q=${"x".repeat(2048)}",
        ).forEach { assertNull(it, restorableRavelryPageOrNull(it)) }
        val longPage = "$page?q=${"x".repeat(2048)}"
        assertEquals(longPage, eligibleRavelryPageOrNull(longPage))
        // Live redirects remain eligible for the download listener, not saved page restoration.
        assertEquals(
            RavelryNavigation.InApp,
            ravelryNavigation("https", "downloads.example-cdn.com", "/pattern.pdf", true, true),
        )
    }

    @Test
    fun `ravelry page URL accepts https ravelry pattern page`() {
        assertEquals(
            "https://www.ravelry.com/patterns/library/cozy-hat",
            ravelryPageUrlOrNull(" https://www.ravelry.com/patterns/library/cozy-hat "),
        )
        assertEquals("https://ravelry.com/patterns/search", ravelryPageUrlOrNull("https://ravelry.com/patterns/search"))
    }

    @Test
    fun `ravelry page URL rejects non-https scheme`() {
        assertNull(ravelryPageUrlOrNull("http://www.ravelry.com/patterns/library/cozy-hat"))
    }

    @Test
    fun `ravelry page URL rejects host confusion`() {
        assertNull(ravelryPageUrlOrNull("https://www.ravelry.com.evil.example/patterns/library/cozy-hat"))
        assertNull(ravelryPageUrlOrNull("https://www.ravelry.com@evil.example/patterns/library/cozy-hat"))
    }

    @Test
    fun `start url excludes subdomains that live navigation allows`() {
        assertNull(ravelryPageUrlOrNull("https://forums.ravelry.com/topics/knitting"))
        assertEquals(
            RavelryNavigation.InApp,
            ravelryNavigation("https", "forums.ravelry.com", "/topics/knitting", true, false),
        )
    }

    @Test
    fun `ravelry page URL rejects malformed input`() {
        assertNull(ravelryPageUrlOrNull("not a url"))
    }

    @Test
    fun `ravelry checkout and payment providers open outside the app`() {
        assertTrue(isRavelryPurchaseUrl("www.ravelry.com", "/stores/dreareneeknits/products/450988/checkout"))
        assertTrue(isRavelryPurchaseUrl("www.ravelry.com", "/carts/123/checkout"))
        assertTrue(isRavelryPurchaseUrl("www.ravelry.com", "/stores/shop/products/1114520/purchase_gift"))
        assertTrue(isRavelryPurchaseUrl("checkout.ravelry.com", "/"))
        assertTrue(isRavelryPurchaseUrl("www.paypal.com", "/checkoutnow"))
        assertTrue(isRavelryPurchaseUrl("checkout.stripe.com", "/pay/cs_test"))
    }

    @Test
    fun `payment redirect recognizes fully qualified provider hosts`() {
        assertEquals(
            RavelryNavigation.Purchase,
            ravelryNavigation("https", "www.paypal.com.", "/checkoutnow", true, true),
        )
        assertEquals(
            RavelryNavigation.InApp,
            ravelryNavigation("https", "www.paypal.com.example.", "/checkoutnow", true, true),
        )
    }

    @Test
    fun `browsing and downloads stay in the app`() {
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/patterns/library/the-shift"))
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/patterns/library/819716/buy"))
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/carts"))
        // Lahjan esikatselu on vielä lomakkeen täyttöä; vasta purchase_gift ostaa.
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/stores/shop/products/1114520/preview_gift"))
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/patterns/library/checkout-cowl"))
        assertFalse(isRavelryPurchaseUrl("notpaypal.com.evil.example", "/"))
        assertFalse(isRavelryPurchaseUrl(null, "/checkout"))
    }

    @Test
    fun `navigation distinguishes direct links from redirects and subframes`() {
        fun nav(
            host: String,
            path: String = "/",
            scheme: String = "https",
            mainFrame: Boolean = true,
            redirect: Boolean = false,
        ) = ravelryNavigation(scheme, host, path, mainFrame, redirect)

        assertEquals(RavelryNavigation.InApp, nav("www.ravelry.com", "/patterns/library/the-shift"))
        assertEquals(RavelryNavigation.InApp, nav("www.ravelry.com", "/account/login"))
        assertEquals(RavelryNavigation.Purchase, nav("www.ravelry.com", "/stores/shop/products/1/checkout"))
        assertEquals(RavelryNavigation.External, nav("designer-blog.example", "/free-hat"))
        assertEquals(RavelryNavigation.External, nav("ravelry.com.evil.example"))
        // Latauspalvelimen uudelleenohjaus ja upotettu kehys jatkuvat, jottei PDF-lataus katkea.
        assertEquals(RavelryNavigation.InApp, nav("downloads.example-cdn.com", "/pattern.pdf", redirect = true))
        assertEquals(RavelryNavigation.InApp, nav("frames.example", mainFrame = false))
        assertEquals(RavelryNavigation.Blocked, nav("www.ravelry.com", scheme = "http"))
        assertEquals(RavelryNavigation.Blocked, nav("www.ravelry.com", scheme = "intent"))
    }

    @Test
    fun `shared checkout link is recognised before the browser loads it`() {
        assertTrue(isRavelryPurchasePage("https://www.ravelry.com/stores/shop/products/1/checkout"))
        assertFalse(isRavelryPurchasePage("https://www.ravelry.com/patterns/library/the-shift"))
        assertFalse(isRavelryPurchasePage("not a url"))
    }
}

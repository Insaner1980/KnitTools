package com.finnvek.knittools.ui.screens.ravelry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryLinksTest {
    @Test
    fun `ravelry page URL accepts https ravelry pattern page`() {
        assertEquals(
            "https://www.ravelry.com/patterns/library/cozy-hat",
            ravelryPageUrlOrNull(" https://www.ravelry.com/patterns/library/cozy-hat "),
        )
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
    fun `ravelry page URL rejects malformed input`() {
        assertNull(ravelryPageUrlOrNull("not a url"))
    }

    @Test
    fun `ravelry checkout and payment providers open outside the app`() {
        assertTrue(isRavelryPurchaseUrl("www.ravelry.com", "/stores/dreareneeknits/products/450988/checkout"))
        assertTrue(isRavelryPurchaseUrl("www.ravelry.com", "/carts/123/checkout"))
        assertTrue(isRavelryPurchaseUrl("checkout.ravelry.com", "/"))
        assertTrue(isRavelryPurchaseUrl("www.paypal.com", "/checkoutnow"))
        assertTrue(isRavelryPurchaseUrl("checkout.stripe.com", "/pay/cs_test"))
    }

    @Test
    fun `browsing and downloads stay in the app`() {
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/patterns/library/the-shift"))
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/patterns/library/819716/buy"))
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/carts"))
        assertFalse(isRavelryPurchaseUrl("www.ravelry.com", "/patterns/library/checkout-cowl"))
        assertFalse(isRavelryPurchaseUrl("notpaypal.com.evil.example", "/"))
        assertFalse(isRavelryPurchaseUrl(null, "/checkout"))
    }

    @Test
    fun `in-app browser shows only ravelry pages`() {
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
}

package com.finnvek.knittools.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationArgumentSafetyTest {
    @Test
    fun `gauge keeps the legacy route and only adds positive optional project ids`() {
        assertEquals("gauge", Screen.Gauge.route)
        assertEquals("gauge", Screen.Gauge.createRoute(null))
        assertEquals("gauge", Screen.Gauge.createRoute(0))
        assertEquals("gauge", Screen.Gauge.createRoute(-1))
        assertEquals("gauge?projectId=42", Screen.Gauge.createRoute(42))
    }

    @Test
    fun `positive long route id is accepted`() {
        assertEquals(42L, 42L.toPositiveRouteIdOrNull())
    }

    @Test
    fun `zero and negative long route ids are rejected`() {
        assertNull(0L.toPositiveRouteIdOrNull())
        assertNull((-1L).toPositiveRouteIdOrNull())
    }

    @Test
    fun `positive int route id is accepted`() {
        assertEquals(42, 42.toPositiveRouteIdOrNull())
    }

    @Test
    fun `zero and negative int route ids are rejected`() {
        assertNull(0.toPositiveRouteIdOrNull())
        assertNull((-1).toPositiveRouteIdOrNull())
    }
}

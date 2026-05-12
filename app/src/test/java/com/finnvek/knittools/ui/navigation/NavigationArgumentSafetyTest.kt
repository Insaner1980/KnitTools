package com.finnvek.knittools.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationArgumentSafetyTest {
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

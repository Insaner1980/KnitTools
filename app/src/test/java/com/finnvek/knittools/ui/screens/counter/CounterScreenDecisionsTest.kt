package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Test

class CounterScreenDecisionsTest {
    @Test
    fun `feature request opens feature when access is granted`() {
        val calls = mutableListOf<String>()

        requestCounterFeature(
            hasAccess = true,
            onOpenFeature = { calls += "feature" },
            onOpenUpgrade = { calls += "upgrade" },
        )

        assertEquals(listOf("feature"), calls)
    }

    @Test
    fun `feature request opens upgrade when access is denied`() {
        val calls = mutableListOf<String>()

        requestCounterFeature(
            hasAccess = false,
            onOpenFeature = { calls += "feature" },
            onOpenUpgrade = { calls += "upgrade" },
        )

        assertEquals(listOf("upgrade"), calls)
    }

    @Test
    fun `stitch tracking toggle asks for stitch count when enabling without stitches`() {
        val calls = mutableListOf<String>()

        handleStitchTrackingToggle(
            enabled = true,
            stitchCount = null,
            onRequestStitchCount = { calls += "count" },
            onSetStitchTrackingEnabled = { calls += "set:$it" },
        )

        assertEquals(listOf("count"), calls)
    }

    @Test
    fun `stitch tracking toggle updates setting when count exists or tracking is disabled`() {
        val calls = mutableListOf<String>()

        handleStitchTrackingToggle(
            enabled = true,
            stitchCount = 12,
            onRequestStitchCount = { calls += "count" },
            onSetStitchTrackingEnabled = { calls += "set:$it" },
        )
        handleStitchTrackingToggle(
            enabled = false,
            stitchCount = null,
            onRequestStitchCount = { calls += "count" },
            onSetStitchTrackingEnabled = { calls += "set:$it" },
        )

        assertEquals(listOf("set:true", "set:false"), calls)
    }
}

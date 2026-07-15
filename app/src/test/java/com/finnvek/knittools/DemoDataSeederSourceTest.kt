package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DemoDataSeederSourceTest {
    @Test
    fun `debug seed contains varied projects and insights history`() {
        val source = File("src/debugShared/kotlin/com/finnvek/knittools/DemoDataSeeder.kt").readText()

        listOf(
            "Forest Cardigan",
            "Sunday Socks",
            "Linen Market Bag",
            "Sunrise Baby Blanket",
            "Cable Beanie",
            "ProjectCounterType.REPEATING",
            "ProjectCounterType.SHAPING",
            "SessionEntity(",
            "YarnCardEntity(",
            "SavedPatternEntity(",
        ).forEach { expected -> assertTrue(source.contains(expected)) }
    }

    @Test
    fun `release seed remains a no-op`() {
        val source = File("src/release/java/com/finnvek/knittools/DemoDataSeeder.kt").readText()

        assertTrue(source.contains(") = Unit"))
        assertFalse(source.contains("CounterProjectEntity"))
    }
}

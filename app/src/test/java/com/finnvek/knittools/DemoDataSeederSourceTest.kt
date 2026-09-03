package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DemoDataSeederSourceTest {
    @Test
    fun `debug seed contains varied projects and insights history`() {
        val source = debugSeedWriterSource()

        listOf(
            "Forest Cardigan",
            "Sunday Socks",
            "Linen Market Bag",
            "Sunrise Baby Blanket",
            "Cable Beanie",
            "ProjectCounterType.REPEATING",
            "ProjectCounterType.SHAPING",
            "SessionEntity(",
            "YarnCard(",
            "SavedPatternEntity(",
        ).forEach { expected -> assertTrue(source.contains(expected)) }
        assertTrue(source.contains("source = SavedPatternSource.WebLink.persistedValue"))
        assertFalse(source.contains("source = SavedPatternSource.Other.persistedValue"))
    }

    @Test
    fun `debug seed keeps Room writes behind data layer transaction boundary`() {
        val facadeSource = File("src/debugShared/kotlin/com/finnvek/knittools/DemoDataSeeder.kt").readText()
        val writerSource = debugSeedWriterSource()

        assertTrue(facadeSource.contains("DebugDemoDataSeeder.seedIfNeeded"))
        assertTrue(facadeSource.contains("projectCounterRepository.get().addCounter"))
        assertTrue(facadeSource.contains("yarnCardRepository.get().saveCardInCurrentTransaction"))
        assertFalse(facadeSource.contains("CounterProjectEntity"))
        assertFalse(facadeSource.contains("counterProjectDao"))
        assertTrue(writerSource.contains("transactionRunner.get().run"))
        assertTrue(writerSource.contains("addCounter("))
        assertTrue(writerSource.contains("saveYarnCard("))
        assertFalse(writerSource.contains("com.finnvek.knittools.repository"))
        assertFalse(writerSource.contains("projectCounterDao()"))
        assertFalse(writerSource.contains("yarnCardDao()"))
        assertFalse(writerSource.contains("updateYarnCardIds"))
        assertFalse(writerSource.contains("withTransaction"))
    }

    @Test
    fun `release seed remains a no-op`() {
        val source = File("src/release/java/com/finnvek/knittools/DemoDataSeeder.kt").readText()

        assertTrue(source.contains(") = Unit"))
        assertFalse(source.contains("CounterProjectEntity"))
    }

    private fun debugSeedWriterSource(): String =
        File("src/debugShared/kotlin/com/finnvek/knittools/data/local/DebugDemoDataSeeder.kt").readText()
}

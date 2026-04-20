package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.ChartSymbolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartSymbolDataTest {
    @Test
    fun `all symbols have non-blank fields`() {
        ChartSymbolData.symbols.forEach { symbol ->
            assertTrue("id tyhjä: $symbol", symbol.id.isNotBlank())
            assertTrue("nameResId puuttuu: $symbol", symbol.nameResId != 0)
            assertTrue("descriptionResId puuttuu: $symbol", symbol.descriptionResId != 0)
        }
    }

    @Test
    fun `no duplicate symbol ids`() {
        val ids = ChartSymbolData.symbols.map { it.id }
        assertEquals("Duplikaatti-id:t löytyi", ids.size, ids.distinct().size)
    }

    @Test
    fun `byCategory returns all categories`() {
        val grouped = ChartSymbolData.byCategory()
        ChartSymbolCategory.entries.forEach { category ->
            assertTrue("Kategoria $category puuttuu", grouped.containsKey(category))
        }
    }

    @Test
    fun `byCategory contains all symbols`() {
        val grouped = ChartSymbolData.byCategory()
        val totalFromGroups = grouped.values.sumOf { it.size }
        assertEquals(ChartSymbolData.symbols.size, totalFromGroups)
    }

    @Test
    fun `basic category has expected symbols`() {
        val basic = ChartSymbolData.byCategory()[ChartSymbolCategory.BASIC]!!
        val ids = basic.map { it.id }
        assertTrue("knit puuttuu", "knit" in ids)
        assertTrue("purl puuttuu", "purl" in ids)
        assertTrue("yarn_over puuttuu", "yarn_over" in ids)
    }

    @Test
    fun `decreases category has k2tog and ssk`() {
        val decreases = ChartSymbolData.byCategory()[ChartSymbolCategory.DECREASES]!!
        val ids = decreases.map { it.id }
        assertTrue("k2tog puuttuu", "k2tog" in ids)
        assertTrue("ssk puuttuu", "ssk" in ids)
    }
}

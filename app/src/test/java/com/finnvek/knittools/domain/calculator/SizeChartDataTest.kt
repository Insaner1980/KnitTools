package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SizeLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeChartDataTest {
    @Test
    fun `all categories return non-empty entries`() {
        SizeChartData.Category.entries.forEach { category ->
            val entries = SizeChartData.entries(category)
            assertTrue("$category: ei yhtaan entry", entries.isNotEmpty())
        }
    }

    @Test
    fun `all categories return non-empty headers`() {
        SizeChartData.Category.entries.forEach { category ->
            val headers = SizeChartData.headers(category)
            assertTrue("$category: ei yhtaan headeria", headers.isNotEmpty())
            assertEquals(
                "$category: Size-header puuttuu",
                R.string.size_col_size,
                headers.first(),
            )
        }
    }

    @Test
    fun `entry measurements match header count minus size column`() {
        SizeChartData.Category.entries.forEach { category ->
            val expectedMeasurements = SizeChartData.headers(category).size - 1 // Size-sarake ei ole mittaus
            SizeChartData.entries(category).forEach { entry ->
                assertEquals(
                    "$category / ${entry.sizeLabel}: mittausten maara ei tasmaa headereihin",
                    expectedMeasurements,
                    entry.measurements.size,
                )
            }
        }
    }

    @Test
    fun `all measurements have positive cm values`() {
        SizeChartData.Category.entries.forEach { category ->
            SizeChartData.entries(category).forEach { entry ->
                entry.measurements.forEach { measurement ->
                    assertTrue(
                        "$category / ${entry.sizeLabel}: cm-arvo ei positiivinen (${measurement.cm})",
                        measurement.cm > 0,
                    )
                }
            }
        }
    }

    @Test
    fun `all measurements have positive inch values`() {
        SizeChartData.Category.entries.forEach { category ->
            SizeChartData.entries(category).forEach { entry ->
                entry.measurements.forEach { measurement ->
                    assertTrue(
                        "$category / ${entry.sizeLabel}: inch-arvo ei positiivinen (${measurement.inches})",
                        measurement.inches > 0,
                    )
                }
            }
        }
    }

    @Test
    fun `inch values are approximately cm divided by 2_54`() {
        SizeChartData.Category.entries.forEach { category ->
            SizeChartData.entries(category).forEach { entry ->
                entry.measurements.forEach { measurement ->
                    val expected = measurement.cm / 2.54
                    assertEquals(
                        "$category / ${entry.sizeLabel}: inch-muunnos ei tasmaa",
                        expected,
                        measurement.inches,
                        0.01,
                    )
                }
            }
        }
    }

    @Test
    fun `no duplicate size labels within a category`() {
        SizeChartData.Category.entries.forEach { category ->
            val sizes = SizeChartData.entries(category).map { it.sizeLabel }
            assertEquals(
                "$category: duplikaattikokoja",
                sizes.size,
                sizes.distinct().size,
            )
        }
    }

    @Test
    fun `women sizes include XS through 5XL`() {
        val sizes =
            SizeChartData
                .entries(SizeChartData.Category.WOMEN)
                .mapNotNull { (it.sizeLabel as? SizeLabel.Literal)?.text }
        assertTrue("XS puuttuu", sizes.any { it.contains("XS") })
        assertTrue("5XL puuttuu", sizes.any { it.contains("5XL") })
    }

    @Test
    fun `baby sizes are in chronological order`() {
        val sizes = SizeChartData.entries(SizeChartData.Category.BABY).map { it.sizeLabel }
        assertEquals(SizeLabel.Resource(R.string.size_row_preemie), sizes.first())
        assertEquals(SizeLabel.Resource(R.string.size_row_age_18_24m), sizes.last())
    }
}

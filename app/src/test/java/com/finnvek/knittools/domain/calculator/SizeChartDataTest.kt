package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.SizeLabel
import com.finnvek.knittools.domain.model.SizeMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SizeChartDataTest {
    @Test
    fun `corrected source categories and cells retain CYC body measurements`() {
        fun values(
            category: SizeChartData.Category,
            row: Int,
        ) = SizeChartData.entries(category)[row].measurements.map { it.cm }

        assertEquals(listOf(5, 8, 9, 8, 7, 9), SizeChartData.Category.entries.map { SizeChartData.entries(it).size })
        assertEquals(listOf(40.5, 45.5, 48.0), values(SizeChartData.Category.BABY, 0))
        assertEquals(listOf(45.5, 50.5, 50.5), values(SizeChartData.Category.BABY, 2))
        assertEquals(listOf(82.5, 69.5, 90.0, 42.0), values(SizeChartData.Category.CHILD_YOUTH, 7))
        assertEquals(listOf(18.0, 16.0), values(SizeChartData.Category.HAND, 3))
        assertEquals(listOf(23.5, 21.5), values(SizeChartData.Category.HAND, 8))
        assertEquals(
            listOf(23.0 to 30.5, 35.5 to 40.5, 40.5 to 46.0, 45.5 to 51.0, 51.0 to 56.0, 53.0 to 58.5, 56.0 to 61.0),
            SizeChartData.entries(SizeChartData.Category.HEAD).map {
                it.measurements.single().let { m ->
                    m.cm to m.maxCm
                }
            },
        )
        assertEquals(
            listOf(
                152.0,
                158.0,
            ),
            SizeChartData.entries(SizeChartData.Category.WOMEN).last().measurements.first().let {
                listOf(it.cm, it.maxCm)
            },
        )
        assertEquals(
            listOf(
                86.0,
                91.5,
            ),
            SizeChartData.entries(SizeChartData.Category.MEN).first().measurements.first().let {
                listOf(it.cm, it.maxCm)
            },
        )
        val hands = SizeChartData.entries(SizeChartData.Category.HAND)
        assertEquals(R.string.category_women, (hands[3].sizeLabel as SizeLabel.Resource).groupResId)
        assertEquals(R.string.category_men, (hands[6].sizeLabel as SizeLabel.Resource).groupResId)
    }

    @Test
    fun `range formatter retains both endpoints in either unit and locale`() {
        val range =
            SizeChartData
                .entries(SizeChartData.Category.HEAD)
                .first()
                .measurements
                .single()
        assertEquals("23–30.5", SizeChartData.formatMeasurementValue(range, false, Locale.US))
        assertEquals("23–30,5", SizeChartData.formatMeasurementValue(range, false, Locale.GERMANY))
        assertEquals("9.1–12.0", SizeChartData.formatMeasurementValue(range, true, Locale.US))
        SizeChartData.Category.entries.flatMap(SizeChartData::entries).flatMap { it.measurements }.forEach {
            assertTrue(it.maxCm >= it.cm)
            assertEquals(it.maxCm / 2.54, it.maxInches, 0.000001)
        }
    }

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
    fun `all size labels are resource backed`() {
        SizeChartData.Category.entries.forEach { category ->
            SizeChartData.entries(category).forEach { entry ->
                assertTrue(
                    "$category / ${entry.sizeLabel}: label ohittaa resurssit",
                    entry.sizeLabel is SizeLabel.Resource,
                )
            }
        }
    }

    @Test
    fun `women sizes include XS through 5XL`() {
        val sizes =
            SizeChartData
                .entries(SizeChartData.Category.WOMEN)
                .map { it.sizeLabel }
        assertTrue("XS puuttuu", SizeLabel.Resource(R.string.size_row_women_xs_2_4) in sizes)
        assertTrue("5XL puuttuu", SizeLabel.Resource(R.string.size_row_women_5xl_34_36) in sizes)
    }

    @Test
    fun `baby sizes are in chronological order`() {
        val sizes = SizeChartData.entries(SizeChartData.Category.BABY).map { it.sizeLabel }
        assertEquals(SizeLabel.Resource(R.string.size_row_age_0_3m), sizes.first())
        assertEquals(SizeLabel.Resource(R.string.size_row_age_18_24m), sizes.last())
    }

    @Test
    fun `measurement formatter localizes the numeric value`() {
        val measurement = SizeMeasurement(cm = 10.0, inches = 3.937)

        assertEquals(
            "10",
            SizeChartData.formatMeasurementValue(
                measurement,
                useImperial = false,
                locale = Locale.US,
            ),
        )
        assertEquals(
            "3.9",
            SizeChartData.formatMeasurementValue(
                measurement,
                useImperial = true,
                locale = Locale.US,
            ),
        )
        assertEquals(
            "3,9",
            SizeChartData.formatMeasurementValue(
                measurement,
                useImperial = true,
                locale = Locale.forLanguageTag("fi-FI"),
            ),
        )
    }
}

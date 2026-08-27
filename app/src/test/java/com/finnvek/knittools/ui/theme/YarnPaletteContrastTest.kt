package com.finnvek.knittools.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Lankavärit ovat kaavion pylväitä, projektilistan väripisteitä ja osuuspalkkeja
 * eli graafisia elementtejä, joilta WCAG 1.4.11 vaatii 3:1 kontrastin taustaan.
 * Kermatausta pudotti alkuperäisen paletin vaaleat sävyt 1,86–2,67:1 tasolle.
 */
class YarnPaletteContrastTest {
    @Test
    fun `every dark theme yarn colour clears the graphical minimum`() {
        assertPaletteClearsMinimum(YarnColors, Background, "dark")
    }

    @Test
    fun `every light theme yarn colour clears the graphical minimum`() {
        assertPaletteClearsMinimum(LightYarnColors, LightBackground, "light")
    }

    @Test
    fun `both palettes hold the same eight slots so an id keeps its identity`() {
        assertEquals(YarnColors.size, LightYarnColors.size)
    }

    @Test
    fun `light palette colours stay distinguishable from each other`() {
        val distinct = LightYarnColors.distinct()

        assertEquals(LightYarnColors.size, distinct.size)
    }

    private fun assertPaletteClearsMinimum(
        palette: List<Color>,
        background: Color,
        themeName: String,
    ) {
        palette.forEachIndexed { index, color ->
            val ratio = contrastRatio(color, background)
            assertTrue(
                "$themeName yarn colour $index is only ${"%.2f".format(ratio)}:1 against the background",
                ratio >= MINIMUM_GRAPHICAL_CONTRAST,
            )
        }
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private companion object {
        const val MINIMUM_GRAPHICAL_CONTRAST = 3.0
    }
}

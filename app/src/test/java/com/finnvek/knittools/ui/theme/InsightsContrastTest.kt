package com.finnvek.knittools.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kaavion apuviivat ovat ei-tekstigrafiikkaa, jota tarvitaan sisällön
 * ymmärtämiseen (asteikko), joten niitä koskee WCAG 2.2:n 3:1 vaatimus.
 */
class InsightsContrastTest {
    @Test
    fun `chart gridlines clear the non-text contrast minimum in both themes`() {
        val dark = contrastRatio(TextPrimary, Background, InsightsDimens.ChartGridlineAlpha)
        val light = contrastRatio(LightTextPrimary, LightBackground, InsightsDimens.ChartGridlineAlpha)

        assertTrue("Dark gridline contrast was $dark", dark >= 3.0)
        assertTrue("Light gridline contrast was $light", light >= 3.0)
    }

    @Test
    fun `muted body text clears the text contrast minimum in both themes`() {
        val dark = contrastRatio(TextMuted, Background, alpha = 1f)
        val light = contrastRatio(LightTextMuted, LightBackground, alpha = 1f)

        assertTrue("Dark muted text contrast was $dark", dark >= 4.5)
        assertTrue("Light muted text contrast was $light", light >= 4.5)
    }

    @Test
    fun `project fabric selection outline clears the graphical minimum`() {
        val dark = contrastRatio(TextPrimary, Background, alpha = 1f)
        val light = contrastRatio(LightTextPrimary, LightBackground, alpha = 1f)

        assertTrue("Dark selection outline contrast was $dark", dark >= 3.0)
        assertTrue("Light selection outline contrast was $light", light >= 3.0)
    }
}

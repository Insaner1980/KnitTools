package com.finnvek.knittools.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.ChartSymbolData
import com.finnvek.knittools.ui.components.care.CareSymbol
import com.finnvek.knittools.ui.components.care.CareSymbolIcon
import com.finnvek.knittools.ui.screens.chartsymbols.ChartSymbolScreen
import com.finnvek.knittools.ui.screens.chartsymbols.chartSymbolWidthUnits
import com.finnvek.knittools.ui.screens.chartsymbols.drawSymbol
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class SharedAuditSymbolsRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun careSymbolsLight() = care(false)

    @Test fun careSymbolsDark() = care(true)

    @Test fun chartSymbolsLight() = charts(false)

    @Test fun chartSymbolsDark() = charts(true)

    @Test fun correctedChartSymbolsWithEnglishDescriptions() {
        rule.setContent { KnitToolsTheme { Surface { ChartSymbolScreen(onBack = {}) } } }
        for ((name, description) in listOf(
            R.string.sym_p2tog_name to R.string.sym_p2tog_desc,
            R.string.sym_kfb_name to R.string.sym_kfb_desc,
        )) {
            val text = rule.activity.getString(description)
            rule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
            rule.onNodeWithText(rule.activity.getString(name)).assertIsDisplayed()
            rule.onNodeWithText(text).assertIsDisplayed()
            save("corrected-${rule.activity.getString(name)}")
        }
    }

    private fun care(dark: Boolean) {
        rule.setContent {
            KnitToolsTheme(isDarkTheme = dark) {
                Surface {
                    Column {
                        CareSymbol.entries.chunked(4).forEach { group ->
                            Row {
                                group.forEach { symbol ->
                                    Column(Modifier.padding(6.dp).size(78.dp, 72.dp)) {
                                        CareSymbolIcon(symbol)
                                        Text(
                                            rule.activity.getString(symbol.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        CareSymbol.entries.forEach {
            rule.onNodeWithContentDescription(rule.activity.getString(it.labelRes)).assertIsDisplayed()
        }
        save("care-$dark")
    }

    private fun charts(dark: Boolean) {
        rule.setContent {
            KnitToolsTheme(isDarkTheme = dark) {
                Surface {
                    Column {
                        ChartSymbolData.symbols.chunked(3).forEach { group ->
                            Row {
                                group.forEach { symbol ->
                                    Column(Modifier.padding(5.dp).size(110.dp, 56.dp)) {
                                        val tint = MaterialTheme.colorScheme.onSurface
                                        Canvas(
                                            Modifier
                                                .size(
                                                    (32 * chartSymbolWidthUnits(symbol.id)).dp,
                                                    32.dp,
                                                ).testTag(symbol.id),
                                        ) {
                                            drawSymbol(symbol.id, tint)
                                        }
                                        Text(symbol.id, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ChartSymbolData.symbols.forEach { rule.onNodeWithTag(it.id).assertIsDisplayed() }

        fun bitmap(id: String) = rule.onNodeWithTag(id).captureToImage().asAndroidBitmap()
        assertFalse(bitmap("knit_tbl").sameAs(bitmap("purl_tbl")))
        assertFalse(bitmap("m1l").sameAs(bitmap("m1r")))
        assertFalse(bitmap("m1l").sameAs(bitmap("lifted_inc_left")))
        assertFalse(bitmap("m1r").sameAs(bitmap("lifted_inc_right")))
        assertFalse(bitmap("lifted_inc_left").sameAs(bitmap("lifted_inc_right")))
        assertFalse(bitmap("k3tog").sameAs(bitmap("sk2p")))
        assertFalse(bitmap("cable_2_2_left").sameAs(bitmap("cable_2_2_right")))
        assertEquals(bitmap("cable_2_2_left").width * 3, bitmap("cable_3_3_left").width * 2)
        val purlDecrease = bitmap("p2tog")
        assertTrue(purlDecrease.hasInkNear(0.35f, 0.35f))
        assertTrue(purlDecrease.hasInkNear(0.5f, 0.5f))
        assertFalse(purlDecrease.hasInkNear(0.425f, 0.425f))
        val frontAndBack = bitmap("kfb")
        assertTrue(frontAndBack.hasInkNear(0.55f, 0.7f))
        assertFalse(frontAndBack.hasInkNear(0.35f, 0.7f))
        assertFalse(frontAndBack.hasInkNear(0.7f, 0.7f))
        save("charts-$dark")
    }

    private fun Bitmap.hasInkNear(
        x: Float,
        y: Float,
    ): Boolean {
        val background = getPixel(0, 0)
        val centerX = (width * x).toInt()
        val centerY = (height * y).toInt()
        return (centerX - 1..centerX + 1).any { px ->
            (centerY - 1..centerY + 1).any { py -> getPixel(px, py) != background }
        }
    }

    private fun save(name: String) {
        File(rule.activity.getExternalFilesDir(null), "shared-$name.png").outputStream().use {
            rule
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

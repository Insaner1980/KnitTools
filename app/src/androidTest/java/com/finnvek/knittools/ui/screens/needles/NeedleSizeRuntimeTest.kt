package com.finnvek.knittools.ui.screens.needles

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class NeedleSizeRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun correctedReferenceIsVisibleInLightTheme() = verify(dark = false)

    @Test fun correctedReferenceIsVisibleInDarkTheme() = verify(dark = true)

    private fun verify(dark: Boolean) {
        rule.setContent { KnitToolsTheme(isDarkTheme = dark) { NeedleSizeScreen(onBack = {}) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("17")
        val locale = rule.activity.resources.configuration.locales[0]
        val decimal =
            java.text.DecimalFormatSymbols
                .getInstance(locale)
                .decimalSeparator
        rule.onNodeWithText("12${decimal}5").performScrollTo().assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextReplacement("12,5")
        rule.onNodeWithText("17").performScrollTo().assertIsDisplayed()
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        File(
            rule.activity.getExternalFilesDir(null),
            "needle-${if (dark) "dark" else "light"}.png",
        ).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

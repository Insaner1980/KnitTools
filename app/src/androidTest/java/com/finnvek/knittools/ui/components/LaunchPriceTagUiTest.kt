package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat

class LaunchPriceTagUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.localizedTestContext("en-US")
    private val offer =
        SelectedOneTimeOffer(
            formattedPrice = "US$9.99",
            offerToken = "test",
            priceAmountMicros = 9_990_000L,
            priceCurrencyCode = "USD",
            launchDetails = LaunchOfferDetails(0L, 1_800_000_000_000L, 14_990_000L),
        )

    @Test
    fun pricesRemainWholeWhenTheCardCannotFitBothOnOneLine() {
        val layout = mutableStateOf(240.dp to 1f)
        val dark = mutableStateOf(false)
        var regularLineHeight = 0f
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalDensity provides Density(density, layout.value.second),
            ) {
                KnitToolsTheme(isDarkTheme = dark.value) {
                    val textMeasurer = rememberTextMeasurer()
                    regularLineHeight =
                        with(LocalDensity.current) {
                            textMeasurer.measure("$14.99", style = MaterialTheme.typography.titleLarge).size.height +
                                ComponentDimens.CompactSpacing.toPx()
                        }
                    Box(Modifier.width(layout.value.first)) { LaunchPriceTag(offer) }
                }
            }
        }
        val regularDescription = context.getString(R.string.price_tag_regular_a11y, "$14.99")
        listOf(false, true).forEach { isDark ->
            listOf(240.dp to 1f, 360.dp to 1f, 300.dp to 1.5f).forEach { size ->
                compose.runOnIdle {
                    dark.value = isDark
                    layout.value = size
                }
                val current = compose.onNodeWithText(offer.formattedPrice)
                val textLayouts = mutableListOf<TextLayoutResult>()
                current.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayouts) }
                val priceLayout = textLayouts.single()
                assertEquals("Current price wraps at $size", 1, priceLayout.lineCount)
                // Kappale voi säilyä mittausrajojen levyisenä: tarkistetaan varsinaisen tekstirivin reunat.
                assertTrue(
                    "Price line exceeds its bounds at $size: " +
                        "left=${priceLayout.getLineLeft(0)}, right=${priceLayout.getLineRight(0)}, " +
                        "width=${priceLayout.size.width}",
                    priceLayout.getLineLeft(0) >= -1f && priceLayout.getLineRight(0) <= priceLayout.size.width + 1f,
                )
                val regular = compose.onNodeWithContentDescription(regularDescription).fetchSemanticsNode().boundsInRoot
                assertTrue("Regular price has no horizontal space at $size", regular.width > 0f)
                assertTrue("Regular price wraps at $size", regular.height <= regularLineHeight + 1f)
                val currentBounds = current.fetchSemanticsNode().boundsInRoot
                assertTrue(regular.left >= currentBounds.right || regular.top >= currentBounds.bottom)
            }
        }
    }

    @Test
    fun launchDeadlineIncludesTheLocalTimeAndZone() {
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
            ) {
                KnitToolsTheme { LaunchPriceTag(offer) }
            }
        }
        val deadline = Date(1_800_000_000_000L)
        val timePattern = if (AndroidDateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val time = SimpleDateFormat(timePattern, Locale.US).format(deadline)
        val timeWithSeconds = SimpleDateFormat("$timePattern:ss", Locale.US).format(deadline)
        val zone = SimpleDateFormat("z", Locale.US).format(deadline)
        compose.onNodeWithText(time, substring = true).assertExists()
        compose.onNodeWithText(timeWithSeconds, substring = true).assertDoesNotExist()
        compose.onNodeWithText(zone, substring = true).assertExists()
    }
}

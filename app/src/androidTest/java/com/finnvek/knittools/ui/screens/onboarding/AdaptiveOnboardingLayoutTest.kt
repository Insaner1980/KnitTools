package com.finnvek.knittools.ui.screens.onboarding

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.OnboardingStep
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.FontScaleThresholds
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class AdaptiveOnboardingLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun shortToolboxExposesFirstToolsAndPriceActionsSurviveContentResize() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.localizedTestContext("en")
        val size = mutableStateOf(DpSize(360.dp, 640.dp))
        val draft = mutableStateOf(OnboardingDraft("adaptive", step = OnboardingStep.Toolbox, name = "Retained draft"))
        var trials = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
            ) {
                KnitToolsTheme {
                    Box(Modifier.size(size.value)) {
                        OnboardingScreen(
                            draft.value,
                            OnboardingPriceState(ready = true),
                            busy = false,
                            messageRes = null,
                            actions = OnboardingActions(startTrial = { trials++ }),
                        )
                    }
                }
            }
        }
        val next = rule.onNodeWithTag("onboarding_next")
        // Ilman vieritystä näkyvät työkalut ovat tavallisen fonttikoon tavoite. Yli 1,3:n kertoimella sovellus
        // vaihtaa yksipalstaiseen asetteluun ja sisältö vierittyy; saavutettavuus tarkistetaan alempana.
        if (context.resources.configuration.fontScale <= FontScaleThresholds.COMPACT) {
            for (name in listOf("Counter", "Pattern")) {
                val tile = rule.onNodeWithTag("onboarding_tool_$name")
                tile.assertIsDisplayed()
                assertTrue(tile.getUnclippedBoundsInRoot().bottom <= next.getUnclippedBoundsInRoot().top)
            }
        }

        fun capture(name: String) {
            val directory = File(context.getExternalFilesDir(null), "adaptive-layout").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use {
                rule
                    .onNodeWithTag(
                        "onboarding",
                    ).captureToImage()
                    .asAndroidBitmap()
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        capture("toolbox-360-640")
        rule.runOnIdle { size.value = DpSize(800.dp, 800.dp) }
        val toolboxTitle = context.getString(R.string.journey_toolbox_title)
        val content =
            rule.onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                    hasAnyDescendant(hasText(toolboxTitle)),
            )
        val initialHeight = content.getUnclippedBoundsInRoot().let { it.bottom - it.top }
        for (height in listOf(400.dp, 800.dp + 479.dp - initialHeight, 800.dp + 480.dp - initialHeight)) {
            rule.runOnIdle { size.value = DpSize(800.dp, height) }
            rule.onNodeWithText(toolboxTitle).performScrollTo()
            val bounds = content.getUnclippedBoundsInRoot()
            println("ADAPTIVE_ONBOARDING host=${size.value} content=$bounds")
            capture("toolbox-height-${height.value.toInt()}")
            next.assertIsDisplayed()
            for (tool in listOf("Counter", "Pattern", "more")) {
                val tile = rule.onNodeWithTag("onboarding_tool_$tool").performScrollTo()
                tile.assertIsDisplayed()
                assertTrue(tile.getUnclippedBoundsInRoot().bottom <= next.getUnclippedBoundsInRoot().top)
                val scrolled = content.getUnclippedBoundsInRoot()
                val action = next.getUnclippedBoundsInRoot()
                assertEquals((action.left + action.right).value, (scrolled.left + scrolled.right).value, 1f)
            }
        }
        rule.runOnIdle { size.value = DpSize(360.dp, 640.dp) }
        rule.runOnIdle { draft.value = draft.value.copy(step = OnboardingStep.Price) }
        rule.waitForIdle()
        capture("price-360-640")
        for (viewport in listOf(DpSize(800.dp, 400.dp), DpSize(800.dp, 1280.dp), DpSize(360.dp, 640.dp))) {
            rule.runOnIdle { size.value = viewport }
            val actual = rule.onNodeWithTag("onboarding").getUnclippedBoundsInRoot()
            assertEquals(viewport.width.value, (actual.right - actual.left).value, 1f)
            assertEquals(viewport.height.value, (actual.bottom - actual.top).value, 1f)
            if (viewport.height == 400.dp) capture("price-800-400")
            rule
                .onNodeWithTag("onboarding_start_trial")
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            rule.onNodeWithText(context.getString(R.string.journey_keep_content)).performScrollTo().assertIsDisplayed()
            rule.onNodeWithTag("onboarding_continue_free").performScrollTo().assertIsDisplayed()
            if (viewport.height == 400.dp) capture("price-retention-800-400")
            assertEquals("Retained draft", draft.value.name)
        }
        assertEquals(3, trials)
    }

    /**
     * Myyntitila oikealla julkaisutarjouksella: hintalappu ostopainikkeineen sekä kokeilu ja ilmainen
     * jatkaminen pysyvät käytettävinä pienessä puhelimessa, matalassa vaakaikkunassa ja tabletilla.
     */
    @Test
    fun launchPriceTagAndPriceActionsStayUsableOnEverySize() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.localizedTestContext("en")
        val now = System.currentTimeMillis()
        val offer =
            SelectedOneTimeOffer(
                formattedPrice = "€4.99",
                offerToken = "adaptive-launch",
                priceAmountMicros = 4_990_000L,
                priceCurrencyCode = "EUR",
                launchDetails =
                    LaunchOfferDetails(
                        startsAtMillis = now - 86_400_000L,
                        endsAtMillis = now + 20L * 86_400_000L,
                        fullPriceMicros = 7_490_000L,
                    ),
            )
        val size = mutableStateOf(DpSize(360.dp, 640.dp))
        val trialAvailable = mutableStateOf(true)
        var purchases = 0
        var trials = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
            ) {
                KnitToolsTheme {
                    Box(Modifier.size(size.value)) {
                        OnboardingScreen(
                            OnboardingDraft("adaptive", step = OnboardingStep.Price, name = "Retained draft"),
                            OnboardingPriceState(
                                ready = true,
                                trialUnavailable = !trialAvailable.value,
                                offer = offer,
                                productStatus = BillingProductStatus.Available,
                            ),
                            busy = false,
                            messageRes = null,
                            actions =
                                OnboardingActions(
                                    startTrial = { trials++ },
                                    purchase = { purchases++ },
                                ),
                        )
                    }
                }
            }
        }
        val viewports = listOf(DpSize(360.dp, 640.dp), DpSize(800.dp, 400.dp), DpSize(800.dp, 1280.dp))
        for (trial in listOf(true, false)) {
            rule.runOnIdle { trialAvailable.value = trial }
            for (viewport in viewports) {
                rule.runOnIdle { size.value = viewport }
                rule.waitForIdle()
                val mode = if (trial) "trial" else "purchase"
                captureOnboarding(
                    context,
                    "sales-$mode-${viewport.width.value.toInt()}-${viewport.height.value.toInt()}",
                )
                rule
                    .onAllNodesWithText("€4.99", substring = true)
                    .onFirst()
                    .performScrollTo()
                    .assertIsDisplayed()
                if (trial) {
                    // Kokeilussa osto on hintalapun sisällä ja kokeilu kiinteässä alapalkissa.
                    rule
                        .onNodeWithTag("onboarding_buy_inline")
                        .performScrollTo()
                        .assertIsDisplayed()
                        .performClick()
                    rule.onNodeWithTag("onboarding_start_trial").assertIsDisplayed().performClick()
                } else {
                    rule.onNodeWithTag("onboarding_buy_primary").assertIsDisplayed().performClick()
                }
                rule.onNodeWithTag("onboarding_continue_free").assertIsDisplayed()
            }
        }
        assertEquals(viewports.size, trials)
        assertEquals(viewports.size * 2, purchases)
    }

    private fun captureOnboarding(
        context: Context,
        name: String,
    ) {
        val directory = File(context.getExternalFilesDir(null), "adaptive-layout").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            rule
                .onNodeWithTag("onboarding")
                .captureToImage()
                .asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

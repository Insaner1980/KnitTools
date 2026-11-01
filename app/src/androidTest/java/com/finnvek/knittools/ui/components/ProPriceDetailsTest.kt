package com.finnvek.knittools.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.BillingUserMessage
import com.finnvek.knittools.billing.LaunchOfferDetails
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProPriceDetailsTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun launchAndNormalQuotesNeverInventMissingFuturePrices() {
        val offer = mutableStateOf(SelectedOneTimeOffer("US$9.99", "test", 9_990_000, "USD"))
        compose.setContent {
            KnitToolsTheme { ProPriceDetails(offer.value, BillingProductStatus.Available, true, {}) }
        }
        compose.onNodeWithText(context.getString(R.string.pro_current_price, "US$9.99")).assertExists()
        compose.onNodeWithText(context.getString(R.string.one_time_purchase)).assertExists()
        compose.onNodeWithText(context.getString(R.string.pro_trial_does_not_reserve_price)).assertDoesNotExist()
        compose.runOnIdle {
            offer.value = offer.value.copy(launchDetails = LaunchOfferDetails(1, 1_800_000_000_000, null))
        }
        compose.onNodeWithText(context.getString(R.string.pro_launch_price, "US$9.99")).assertExists()
        compose.onNodeWithText(context.getString(R.string.pro_trial_does_not_reserve_price)).assertExists()
        compose.onNodeWithText("14.99", substring = true).assertDoesNotExist()
        compose.runOnIdle {
            offer.value =
                offer.value.copy(launchDetails = offer.value.launchDetails?.copy(fullPriceMicros = 14_990_000))
        }
        compose
            .onNodeWithText(
                context.getString(
                    R.string.pro_price_after_launch,
                    formatOfferFullPrice(offer.value, context.resources.configuration.locales[0]),
                ),
            ).assertExists()
    }

    @Test
    fun unavailablePriceOffersRetryAndKeepsTrialExplanation() {
        val status = mutableStateOf<BillingProductStatus>(BillingProductStatus.Loading)
        var retries = 0
        compose.setContent { KnitToolsTheme { ProPriceDetails(null, status.value, true, { retries++ }) } }
        compose.onNodeWithText(context.getString(R.string.loading_price)).assertExists()
        compose.onNodeWithText(context.getString(R.string.one_time_purchase)).assertExists()
        compose.runOnIdle { status.value = BillingProductStatus.Unavailable(BillingUserMessage.PURCHASE_NETWORK_ERROR) }
        compose.onNodeWithText(context.getString(R.string.pro_price_unavailable_trial)).assertExists()
        compose.onNodeWithText(context.getString(R.string.one_time_purchase)).assertExists()
        compose.onNodeWithText("9.99", substring = true).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(1, retries)
    }
}

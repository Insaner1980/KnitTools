package com.finnvek.knittools.ui.screens.onboarding

import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.pro.ProStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingPricePolicyTest {
    private val offer = SelectedOneTimeOffer(formattedPrice = "9,99 €", offerToken = "test-offer")

    @Test fun `unloaded access never promises a trial or purchase`() {
        ProStatus.entries.forEach {
            assertEquals(
                OnboardingPriceMode.Loading,
                onboardingPriceMode(false, it, false, BillingProductStatus.Available, offer),
            )
        }
    }

    @Test fun `offline trial does not wait for prices`() {
        assertEquals(
            OnboardingPriceMode.Trial,
            onboardingPriceMode(true, ProStatus.TRIAL_NOT_STARTED, false, BillingProductStatus.Loading, null),
        )
    }

    @Test fun `used and ineligible trials promote confirmed purchase but skip completed missing prices`() {
        listOf(ProStatus.TRIAL_EXPIRED to false, ProStatus.TRIAL_NOT_STARTED to true).forEach { (status, unavailable) ->
            assertEquals(
                OnboardingPriceMode.Purchase,
                onboardingPriceMode(true, status, unavailable, BillingProductStatus.Available, offer),
            )
            assertEquals(
                OnboardingPriceMode.Loading,
                onboardingPriceMode(true, status, unavailable, BillingProductStatus.Loading, null),
            )
            assertEquals(
                OnboardingPriceMode.Skip,
                onboardingPriceMode(true, status, unavailable, BillingProductStatus.Available, null),
            )
            assertEquals(
                OnboardingPriceMode.Skip,
                onboardingPriceMode(
                    true,
                    status,
                    unavailable,
                    BillingProductStatus.Unavailable(
                        com.finnvek.knittools.billing.BillingUserMessage.PURCHASE_UNAVAILABLE,
                    ),
                    null,
                ),
            )
        }
    }

    @Test fun `purchased users skip and active trials continue without restarting`() {
        assertEquals(
            OnboardingPriceMode.Skip,
            onboardingPriceMode(true, ProStatus.PRO_PURCHASED, false, BillingProductStatus.Loading, null),
        )
        assertEquals(
            OnboardingPriceMode.Continue,
            onboardingPriceMode(true, ProStatus.TRIAL_ACTIVE, false, BillingProductStatus.Loading, null),
        )
    }
}

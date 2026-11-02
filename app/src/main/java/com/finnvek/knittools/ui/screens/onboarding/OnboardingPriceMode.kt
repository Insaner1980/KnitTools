package com.finnvek.knittools.ui.screens.onboarding

import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.pro.ProStatus

enum class OnboardingPriceMode { Loading, Trial, Purchase, Continue, Skip }

fun onboardingPriceMode(
    ready: Boolean,
    status: ProStatus,
    trialUnavailable: Boolean,
    productStatus: BillingProductStatus,
    offer: SelectedOneTimeOffer?,
): OnboardingPriceMode =
    when {
        !ready -> OnboardingPriceMode.Loading
        status == ProStatus.PRO_PURCHASED -> OnboardingPriceMode.Skip
        status == ProStatus.TRIAL_ACTIVE -> OnboardingPriceMode.Continue
        status == ProStatus.TRIAL_NOT_STARTED && !trialUnavailable -> OnboardingPriceMode.Trial
        productStatus == BillingProductStatus.Available && offer != null -> OnboardingPriceMode.Purchase
        productStatus == BillingProductStatus.Loading -> OnboardingPriceMode.Loading
        else -> OnboardingPriceMode.Skip
    }

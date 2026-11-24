package com.finnvek.knittools.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingProductStatus
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.domain.model.OnboardingDraft
import com.finnvek.knittools.domain.model.proGroupOrder
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.ui.components.CancelButton
import com.finnvek.knittools.ui.components.CraftIllustration
import com.finnvek.knittools.ui.components.LaunchPriceTag
import com.finnvek.knittools.ui.screens.pro.ProAddsSection
import com.finnvek.knittools.ui.screens.pro.ProFreeForeverSection
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.FontScaleThresholds
import com.finnvek.knittools.ui.theme.IllustrationDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens

data class OnboardingPriceState(
    val ready: Boolean = false,
    val pro: ProState = ProState(),
    val trialUnavailable: Boolean = false,
    val offer: SelectedOneTimeOffer? = null,
    val productStatus: BillingProductStatus = BillingProductStatus.Loading,
    val purchaseBusy: Boolean = false,
    val pending: Boolean = false,
) {
    val mode: OnboardingPriceMode
        get() = onboardingPriceMode(ready, pro.status, trialUnavailable, productStatus, offer)
    val confirmedOffer: SelectedOneTimeOffer?
        get() = offer.takeIf { ready && productStatus == BillingProductStatus.Available }
}

/**
 * Hintavaiheen kiinteät toiminnot. [sideBySide] matalalla näytöllä: ilmainen jatkaminen vasemmalla ja
 * päätoiminto oikealla samalla rivillä, jotta kiinteä alue ei vie puolta näytöstä.
 */
@Composable
internal fun JourneyPriceActions(
    price: OnboardingPriceState,
    busy: Boolean,
    actions: OnboardingActions,
    sideBySide: Boolean = false,
) {
    val showContinueFree = price.mode != OnboardingPriceMode.Continue
    if (sideBySide && showContinueFree && price.mode != OnboardingPriceMode.Skip) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { JourneyContinueFreeAction(busy, actions) }
            Box(Modifier.weight(1f)) { JourneyPricePrimaryAction(price, busy, actions) }
        }
    } else {
        JourneyPricePrimaryAction(price, busy, actions)
        if (showContinueFree) JourneyContinueFreeAction(busy, actions)
    }
}

@Composable
private fun JourneyContinueFreeAction(
    busy: Boolean,
    actions: OnboardingActions,
) {
    CancelButton(
        onClick = actions.finish,
        text = stringResource(R.string.journey_continue_free),
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().testTag("onboarding_continue_free"),
    )
}

@Composable
private fun JourneyPricePrimaryAction(
    price: OnboardingPriceState,
    busy: Boolean,
    actions: OnboardingActions,
) {
    when (price.mode) {
        OnboardingPriceMode.Trial -> {
            val label = stringResource(R.string.journey_start_trial)
            val modifier = Modifier.testTag("onboarding_start_trial")
            if (price.confirmedOffer == null) {
                JourneyPrimaryButton(label, !busy, actions.startTrial, modifier)
            } else {
                OutlinedButton(
                    onClick = actions.startTrial,
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = modifier.fillMaxWidth().heightIn(min = ComponentDimens.FormSheetActionMinHeight),
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        OnboardingPriceMode.Purchase -> {
            JourneyPrimaryButton(
                stringResource(R.string.journey_buy_price, price.confirmedOffer?.formattedPrice.orEmpty()),
                !busy && !price.purchaseBusy && !price.pending,
                actions.purchase,
                Modifier.testTag("onboarding_buy_primary"),
            )
        }

        OnboardingPriceMode.Continue -> {
            JourneyPrimaryButton(
                stringResource(R.string.journey_continue_app),
                !busy,
                actions.finish,
            )
        }

        OnboardingPriceMode.Loading -> {
            JourneyPrimaryButton(
                stringResource(R.string.journey_loading),
                false,
                {},
                Modifier.testTag("onboarding_loading_action"),
            )
        }

        OnboardingPriceMode.Skip -> {
            Unit
        }
    }
}

@Composable
internal fun JourneyPriceContent(
    draft: OnboardingDraft,
    price: OnboardingPriceState,
    busy: Boolean,
    onPurchase: () -> Unit,
) {
    if (!price.ready || price.mode == OnboardingPriceMode.Skip) {
        JourneyTitle(stringResource(R.string.journey_continue_title))
        Text(stringResource(R.string.journey_loading), style = MaterialTheme.typography.bodyLarge)
        return
    }
    journeyIllustrationHeight(IllustrationDimens.CompactHeight)?.let { height ->
        CraftIllustration(R.drawable.onboarding_pro_hero, height = height)
    }
    JourneyTitle(
        stringResource(
            if (price.mode ==
                OnboardingPriceMode.Trial
            ) {
                R.string.journey_trial_title
            } else {
                R.string.journey_continue_title
            },
        ),
    )
    if (price.mode == OnboardingPriceMode.Trial) {
        Text(stringResource(R.string.journey_trial_terms), style = MaterialTheme.typography.bodyLarge)
    }
    OnboardingPreview.Notice()
    // Hintalappu heti otsikon alla, jotta hinta ja julkaisualennus näkyvät ilman vieritystä.
    price.confirmedOffer?.let { offer ->
        LaunchPriceTag(
            offer = offer,
            // Ilman kokeilua osto on alapalkin päätoiminto, joten lapussa ei ole toista ostopainiketta.
            onBuy = onPurchase.takeIf { price.mode != OnboardingPriceMode.Purchase },
            buyEnabled = !busy && !price.pending,
            buyInFlight = price.purchaseBusy,
            buyTestTag = "onboarding_buy_inline",
        )
    }
    ProAddsSection(order = proGroupOrder(draft.needs))
    if (price.mode == OnboardingPriceMode.Trial) {
        Text(stringResource(R.string.journey_timeline_title), style = MaterialTheme.typography.titleLarge)
        Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing)) {
            JourneyTimelineRow(stringResource(R.string.journey_today), stringResource(R.string.journey_today_body))
            JourneyTimelineRow(
                stringResource(R.string.journey_three_days),
                stringResource(R.string.journey_three_days_body),
            )
            JourneyTimelineRow(
                stringResource(R.string.journey_day_fourteen),
                stringResource(R.string.journey_day_fourteen_body),
            )
        }
        Text(stringResource(R.string.journey_keep_content), style = MaterialTheme.typography.bodyLarge)
    }
    if (price.mode == OnboardingPriceMode.Continue) {
        Text(
            pluralStringResource(
                R.plurals.pro_status_trial_days,
                price.pro.trialDaysRemaining,
                price.pro.trialDaysRemaining,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    if (price.pending) {
        Text(
            stringResource(R.string.billing_purchase_pending),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    ProFreeForeverSection()
}

@Composable
private fun JourneyTimelineRow(
    label: String,
    body: String,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < OnboardingDimens.JourneyStackedBreakpoint ||
            LocalDensity.current.fontScale > FontScaleThresholds.COMPACT
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        } else {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(2f))
                }
                HorizontalDivider(
                    Modifier.padding(top = ComponentDimens.ContentSpacing),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

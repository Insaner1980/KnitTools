package com.finnvek.knittools.ui.screens.pro

import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyFacts
import kotlinx.coroutines.delay

data class TrialJourneyPresentation(
    val status: ProStatus = ProStatus.TRIAL_NOT_STARTED,
    val ready: Boolean = false,
    val facts: TrialJourneyFacts? = null,
    val pending: Boolean = false,
    val fromOnboarding: Boolean = false,
)

@Composable
internal fun trialPersonalLine(
    status: ProStatus,
    facts: TrialJourneyFacts?,
): String? {
    if (facts == null) return null
    if (status == ProStatus.TRIAL_EXPIRED) {
        trialSummaryText(facts, afterTrial = true)?.let { return it }
    }
    return facts.activeProjectCount.takeIf { it > 0 }?.let {
        pluralStringResource(R.plurals.onboarding_pro_active_projects, it, it)
    }
}

@Composable
internal fun trialSummaryText(
    facts: TrialJourneyFacts?,
    afterTrial: Boolean,
): String? {
    if (facts == null) return null
    val parts =
        listOfNotNull(
            facts.projectsAdded.takeIf { it > 0 }?.let {
                pluralStringResource(
                    R.plurals.trial_summary_projects,
                    it,
                    it,
                )
            },
            facts.countersAdded.takeIf { it > 0 }?.let {
                pluralStringResource(
                    R.plurals.trial_summary_counters,
                    it,
                    it,
                )
            },
            facts.yarnCardsAdded.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.trial_summary_yarn, it, it) },
            facts.photosAdded.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.trial_summary_photos, it, it) },
        )
    if (parts.isEmpty()) return null
    val locale = LocalResources.current.configuration.locales[0]
    val formatter = remember(locale) { ListFormatter.getInstance(locale) }
    return stringResource(
        if (afterTrial) R.string.trial_summary_during else R.string.trial_summary_so_far,
        formatter.format(parts),
    )
}

/** Oma sheet ei sulkeudu siihen, että sen ikkuna vie pääikkunan fokuksen. */
@Composable
internal fun rememberTrialJourneyVisible(
    presentation: TrialJourneyPresentation,
    eligible: Boolean,
    actionVersion: Int = 0,
    onConsume: () -> Unit,
): Boolean {
    // Actions temporarily hide the sheet; only an explicit dismissal consumes the journey.
    var visible by rememberSaveable(actionVersion) { mutableStateOf(false) }
    val consume by rememberUpdatedState(onConsume)
    val activeWithFacts = presentation.status == ProStatus.TRIAL_ACTIVE && presentation.facts != null
    val finished = presentation.status == ProStatus.TRIAL_EXPIRED || presentation.status == ProStatus.PRO_PURCHASED
    LaunchedEffect(presentation.pending, presentation.ready, finished) {
        if (!presentation.pending) visible = false
        if (presentation.pending && presentation.ready && finished) {
            visible = false
            consume()
        }
    }
    LaunchedEffect(presentation.pending, eligible, activeWithFacts, visible, actionVersion) {
        if (presentation.pending && eligible && activeWithFacts && !visible) {
            // Alkuperäisen lomakkeen tai ulkoisen toiminnon annetaan avautua ensin.
            delay(TRIAL_JOURNEY_SETTLE_MILLIS)
            visible = true
        }
    }
    return visible && presentation.pending && presentation.ready && activeWithFacts
}

private const val TRIAL_JOURNEY_SETTLE_MILLIS = 2_000L

/** Keeps a requested chart unobstructed until its navigation entry is left. */
@Composable
internal fun rememberTrialJourneyDeferredEntry(currentEntryId: String?): MutableState<String?> {
    val deferredEntry = rememberSaveable { mutableStateOf<String?>(null) }
    var deferredEntryObserved by rememberSaveable(deferredEntry.value) { mutableStateOf(false) }
    LaunchedEffect(currentEntryId, deferredEntry.value) {
        if (currentEntryId == null) return@LaunchedEffect
        if (currentEntryId == deferredEntry.value) {
            deferredEntryObserved = true
        } else if (deferredEntryObserved) {
            deferredEntry.value = null
        }
    }
    return deferredEntry
}

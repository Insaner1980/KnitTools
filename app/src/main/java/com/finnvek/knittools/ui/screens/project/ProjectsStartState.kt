package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.FabricKind
import com.finnvek.knittools.ui.components.FabricSwatch
import com.finnvek.knittools.ui.components.LabeledCounterImageButton
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.WelcomeExampleCard
import com.finnvek.knittools.ui.components.proMarkerDescription
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.OnboardingDimens
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/** Projektittoman käyttäjän aloitus, joka on osa projektisivun tavallista vieritettävää sisältöä. */
@Composable
fun ProjectsStartState(
    compact: Boolean,
    lastCraftType: CraftType,
    onCreateProject: () -> Unit,
    onDismiss: () -> Unit,
    onTools: () -> Unit,
    onDemoUsed: () -> Unit,
    modifier: Modifier = Modifier,
    proState: ProState = ProState(),
    proAccessReady: Boolean = false,
    onOpenPro: () -> Unit = {},
) {
    if (compact) {
        CompactProjectsStartState(lastCraftType, modifier)
    } else {
        FullProjectsStartState(
            onCreateProject,
            onDismiss,
            onTools,
            onDemoUsed,
            modifier,
            proState,
            proAccessReady,
            onOpenPro,
        )
    }
}

@Composable
// CPD-OFF: Täyden aloitussisällön sopimus vastaa julkista isäntää; kompakti sisältö käyttää vain osan arvoista.
private fun FullProjectsStartState(
    onCreateProject: () -> Unit,
    onDismiss: () -> Unit,
    onTools: () -> Unit,
    onDemoUsed: () -> Unit,
    modifier: Modifier = Modifier,
    proState: ProState = ProState(),
    proAccessReady: Boolean = false,
    onOpenPro: () -> Unit = {},
) {
    // CPD-ON
    val createProjectAccess =
        proActionAccess(proState, proAccessReady, ProFeature.UNLIMITED_PROJECTS, needsEntitlement = false)
    val newProjectLabel = stringResource(R.string.new_project)
    val createProjectDescription = proMarkerDescription(createProjectAccess)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(ProjectListDimens.FooterActionTouchSize).align(Alignment.End),
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.welcome_dismiss_a11y))
        }
        WelcomeExampleCard(onDemoUsed)
        Text(
            text = stringResource(R.string.welcome_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.welcome_trust),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.knitToolsColors.emptyStateText,
        )
        LabeledCounterImageButton(
            imageRes = R.drawable.counter_plus_button,
            label = newProjectLabel,
            visualSize = ProjectListDimens.CreateButtonVisualSize,
            onClick = onCreateProject,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            badgeContentDescription = createProjectDescription,
            badge = createProjectDescription?.let { { ProMarker(createProjectAccess) } },
        )
        if (proAccessReady && proState.status != ProStatus.PRO_PURCHASED) {
            Text(
                stringResource(
                    if (proState.status == ProStatus.TRIAL_NOT_STARTED) {
                        R.string.welcome_free_pro_trial
                    } else {
                        R.string.welcome_free_pro_general
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenPro, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.welcome_learn_pro))
            }
        }
        TextButton(onClick = onTools, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.welcome_tools_only))
        }
    }
}

@Composable
private fun CompactProjectsStartState(
    lastCraftType: CraftType,
    modifier: Modifier = Modifier,
) {
    val isCrochet = lastCraftType == CraftType.CROCHET
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = ComponentDimens.LargeContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.LargeContentPadding, Alignment.CenterVertically),
    ) {
        FabricSwatch(
            kind = if (isCrochet) FabricKind.CROCHET else FabricKind.KNIT,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(OnboardingDimens.CompactSwatchSize).clip(MaterialTheme.shapes.large),
        )
        Text(
            text =
                stringResource(
                    if (isCrochet) R.string.start_compact_title_crochet else R.string.start_compact_title_knitting,
                ),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.start_compact_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.knitToolsColors.emptyStateText,
            textAlign = TextAlign.Center,
        )
    }
}

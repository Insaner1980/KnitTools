package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.ProjectSetupItem
import com.finnvek.knittools.domain.calculator.ProjectSetupProgress
import com.finnvek.knittools.domain.calculator.projectSetupProgress
import com.finnvek.knittools.domain.model.isWebPatternCompatible
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.ui.components.CrossStitchGlyph
import com.finnvek.knittools.ui.components.OverviewLinkRow
import com.finnvek.knittools.ui.components.OverviewSectionHeader
import com.finnvek.knittools.ui.components.RowMenuAction
import com.finnvek.knittools.ui.components.RowOverflowMenu
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.tips.LocalTipRouteKey
import com.finnvek.knittools.ui.tips.LocalTipSession

data class ProjectSetupVisit(
    val projectId: Long,
    val routeEntryGeneration: Long,
)

internal data class ProjectSetupActions(
    val onItemClick: (ProjectSetupItem) -> Unit,
    val onHide: () -> Unit,
    val onCelebrate: (ProjectSetupVisit) -> Unit,
)

@Composable
internal fun ProjectSetupRoute(
    state: CounterUiState,
    viewModel: CounterViewModel,
    onItemClick: (ProjectSetupItem) -> Unit,
    onBlockingSurfaceChanged: (Boolean) -> Unit,
) {
    val session = LocalTipSession.current ?: return
    val sessionState by session.state.collectAsStateWithLifecycle()
    val onboarding by viewModel.onboardingState.collectAsStateWithLifecycle(initialValue = null)
    val celebratedVisit by viewModel.projectSetupCelebration.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow
        .collectAsState()
    val routeKey = LocalTipRouteKey.current
    val visit =
        state.projectId?.takeIf { sessionState.routeKey == routeKey }?.let {
            ProjectSetupVisit(it, sessionState.routeEntryGeneration)
        }
    ProjectSetupSection(
        state = state,
        onboarding = onboarding,
        visit = visit,
        celebratedVisit = celebratedVisit,
        canCelebrate = lifecycle == Lifecycle.State.RESUMED,
        actions =
            ProjectSetupActions(
                onItemClick,
                viewModel::hideProjectSetup,
                { viewModel.celebrateProjectSetup(it.projectId, it.routeEntryGeneration) },
            ),
        onBlockingSurfaceChanged = onBlockingSurfaceChanged,
    )
}

@Composable
internal fun ProjectSetupSection(
    state: CounterUiState,
    onboarding: OnboardingState?,
    visit: ProjectSetupVisit?,
    celebratedVisit: ProjectSetupVisit?,
    actions: ProjectSetupActions,
    canCelebrate: Boolean,
    onBlockingSurfaceChanged: (Boolean) -> Unit = {},
) {
    val projectId = state.projectId ?: return
    val currentVisit = visit?.takeIf { it.projectId == projectId } ?: return
    if (!state.startupReady || onboarding == null || onboarding.initializedVersion == 0) return
    if (onboarding.starterProjectId != projectId || state.isCompleted || onboarding.projectSetupHidden) return
    if (onboarding.projectSetupCelebrated && celebratedVisit != currentVisit) return
    val progress =
        projectSetupProgress(
            state.targetRows,
            state.projectDocuments.isNotEmpty() || state.linkedPattern?.isWebPatternCompatible == true,
            state.projectYarnNotes.isNotEmpty() || state.linkedYarns.isNotEmpty(),
            state.hasSessions,
        )
    LaunchedEffect(currentVisit, progress.doneCount, canCelebrate, onboarding.projectSetupCelebrated) {
        if (canCelebrate && progress.doneCount == ProjectSetupItem.entries.size && !onboarding.projectSetupCelebrated) {
            actions.onCelebrate(currentVisit)
        }
    }
    ProjectSetupList(
        progress = progress,
        actions = actions,
        onBlockingSurfaceChanged = onBlockingSurfaceChanged,
    )
}

@Composable
private fun ProjectSetupList(
    progress: ProjectSetupProgress,
    actions: ProjectSetupActions,
    onBlockingSurfaceChanged: (Boolean) -> Unit,
) {
    Column {
        OverviewSectionHeader(
            title = R.string.project_setup_title,
            trailingContent = {
                RowOverflowMenu(
                    actions = listOf(RowMenuAction(stringResource(R.string.project_setup_hide), actions.onHide)),
                    onExpandedChange = onBlockingSurfaceChanged,
                )
            },
        )
        Text(
            stringResource(R.string.project_setup_optional_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = ProjectOverviewDimens.ContentGap),
        )
        progress.items.forEach { item ->
            OverviewLinkRow(
                title = stringResource(item.title(progress.isDone(item))),
                subtitle = null,
                glyph = item.glyph(),
                onClick = { actions.onItemClick(item) },
                done = progress.isDone(item),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private fun ProjectSetupItem.title(done: Boolean): Int =
    when (this) {
        ProjectSetupItem.TARGET ->
            if (done) R.string.project_setup_target_done else R.string.project_setup_target
        ProjectSetupItem.PATTERN -> if (done) R.string.project_setup_pattern_done else R.string.project_setup_pattern
        ProjectSetupItem.YARN -> if (done) R.string.project_setup_yarn_done else R.string.project_setup_yarn
        ProjectSetupItem.WORK_TIME -> if (done) R.string.project_setup_session_done else R.string.project_setup_session
    }

private fun ProjectSetupItem.glyph(): CrossStitchGlyph =
    when (this) {
        ProjectSetupItem.TARGET -> CrossStitchGlyph.COUNTER
        ProjectSetupItem.PATTERN -> CrossStitchGlyph.PATTERN
        ProjectSetupItem.YARN -> CrossStitchGlyph.YARN
        ProjectSetupItem.WORK_TIME -> CrossStitchGlyph.CLOCK
    }

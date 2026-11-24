package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.pro.ProActionAccess
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.proActionAccess
import com.finnvek.knittools.ui.components.ProMarker
import com.finnvek.knittools.ui.components.ProjectThumbnail
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.components.rememberExpandedModalSheetState
import com.finnvek.knittools.ui.layout.AdaptiveModalWindow
import com.finnvek.knittools.ui.layout.ContentWidthPolicy
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import kotlinx.coroutines.flow.first

data class ProjectActionsSheetCallbacks(
    val onDismiss: () -> Unit,
    val onOpenOverview: () -> Unit,
    val onOpenCountersList: () -> Unit,
    val onOpenAddCounter: () -> Unit,
    val onToggleStitchTracking: (Boolean) -> Unit,
    val onOpenStitchCount: () -> Unit,
    val onStartWorkSession: () -> Unit,
    val onStopWorkSession: () -> Unit,
    val onShowResetDialog: () -> Unit,
    val onMeasurements: () -> Unit = {},
    val onOpenCounterHistory: () -> Unit = {},
    val onOpenSecondary: () -> Unit = {},
    val onPinWidget: () -> Unit = {},
    val onToggleKeepScreenAwake: (Boolean) -> Unit = {},
)

data class ProjectActionsSheetState(
    val projectId: Long,
    val projectName: String,
    val craftType: CraftType,
    val photoUri: String?,
    val projectCounterCount: Int,
    val stitchTrackingEnabled: Boolean,
    val stitchCount: Int?,
    val proState: ProState,
    val proAccessReady: Boolean,
    val secondaryCounterUsed: Boolean,
    val isWorkSessionActiveForProject: Boolean,
    val isCompleted: Boolean,
    val keepScreenAwake: Boolean = false,
    val focusWorkSession: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectActionsBottomSheet(
    state: ProjectActionsSheetState,
    callbacks: ProjectActionsSheetCallbacks,
) {
    val sheetState = rememberExpandedModalSheetState()
    ModalBottomSheet(
        onDismissRequest = callbacks.onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = ContentWidthPolicy.Form.maxWidth,
        // Sama pinta ja oletusmuoto kuin muissa sheeteissä. Otsikkona toimii projektikortti
        // (ProjectOverviewLink), joten erillistä SheetTitlea ei toisteta sen yläpuolelle.
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AdaptiveModalWindow()
        val scrollState = rememberScrollState()
        LaunchedEffect(state.focusWorkSession) {
            if (state.focusWorkSession) {
                val end = snapshotFlow { scrollState.maxValue }.first { it != Int.MAX_VALUE }
                scrollState.scrollTo(end)
            }
        }
        Column(modifier = Modifier.verticalScroll(scrollState).padding(bottom = 18.dp)) {
            ProjectOverviewLink(state, callbacks)
            CounterToolActions(state, callbacks)
            SectionDivider()
            if (!state.isCompleted) {
                ProjectActionsSection(title = stringResource(R.string.project_actions_section_work_session)) {
                    WorkSessionActionRow(state, callbacks)
                }
            }
        }
    }
}

@Composable
private fun ProjectOverviewLink(
    state: ProjectActionsSheetState,
    callbacks: ProjectActionsSheetCallbacks,
) {
    Surface(
        modifier = Modifier.padding(horizontal = CounterDimens.ProjectActionsHorizontalPadding).fillMaxWidth(),
        shape = RoundedCornerShape(ProjectOverviewDimens.SheetCornerRadius),
        color = MaterialTheme.knitToolsColors.actionContainer,
    ) {
        Row(
            Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.project_card_open_overview_action),
                    onClick = callbacks.onOpenOverview,
                ).padding(ProjectOverviewDimens.PillHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ProjectOverviewDimens.PillHorizontalPadding),
        ) {
            ProjectThumbnail(state.projectId, state.craftType, state.photoUri, ProjectOverviewDimens.YarnSwatchSize)
            Column(Modifier.weight(1f)) {
                Text(state.projectName, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.project_actions_overview_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun CounterToolActions(
    state: ProjectActionsSheetState,
    callbacks: ProjectActionsSheetCallbacks,
) {
    val counterAccess = proActionAccess(state.proState, state.proAccessReady, ProFeature.MULTIPLE_COUNTERS)
    val secondaryAccess =
        proActionAccess(
            state.proState,
            state.proAccessReady,
            ProFeature.SECONDARY_COUNTER,
            alreadyUnlocked = state.secondaryCounterUsed,
        )
    val widgetAccess = proActionAccess(state.proState, state.proAccessReady, ProFeature.WIDGET)
    ProjectActionsSection(title = stringResource(R.string.project_actions_section_counter_tools)) {
        ActionRow(
            label = stringResource(R.string.counters),
            trailingCount = state.projectCounterCount + 1,
            onClick = callbacks.onOpenCountersList,
        )
        if (!state.isCompleted) {
            ActionRow(
                label = stringResource(R.string.add_counter),
                onClick = callbacks.onOpenAddCounter,
                showChevron = false,
                enabled =
                    counterAccess != ProActionAccess.Unknown || state.proState.hasFeature(ProFeature.MULTIPLE_COUNTERS),
                badge = { ProMarker(counterAccess) },
            )
        }
        if (!state.isCompleted && secondaryAccess == ProActionAccess.Locked) {
            ActionRow(
                label = stringResource(R.string.pro_repeat_counter_row),
                onClick = callbacks.onOpenSecondary,
                showChevron = false,
                badge = { ProMarker(secondaryAccess) },
            )
        }
        ActionRow(
            label = stringResource(R.string.widget_add_row),
            onClick = callbacks.onPinWidget,
            showChevron = false,
            enabled = widgetAccess != ProActionAccess.Unknown,
            badge = { ProMarker(widgetAccess) },
        )
        ActionRow(
            label = stringResource(R.string.counter_history_title),
            onClick = callbacks.onOpenCounterHistory,
        )
        if (!state.isCompleted) {
            ActionRow(
                label = stringResource(R.string.stitches_per_row),
                trailingCount = state.stitchCount?.takeIf { it > 0 },
                onClick = callbacks.onOpenStitchCount,
            )
            SwitchRow(
                label = stringResource(R.string.track_stitches),
                checked = state.stitchTrackingEnabled,
                onCheckedChange = callbacks.onToggleStitchTracking,
            )
        }
        SwitchRow(
            label = stringResource(R.string.keep_screen_awake),
            checked = state.keepScreenAwake,
            onCheckedChange = callbacks.onToggleKeepScreenAwake,
        )
        ActionRow(
            label = stringResource(R.string.measurement_title),
            onClick = callbacks.onMeasurements,
        )
        if (!state.isCompleted) {
            ActionRow(
                label = stringResource(R.string.reset_counter),
                onClick = callbacks.onShowResetDialog,
                showChevron = false,
            )
        }
    }
}

@Composable
private fun WorkSessionActionRow(
    state: ProjectActionsSheetState,
    callbacks: ProjectActionsSheetCallbacks,
) {
    ActionRow(
        label =
            stringResource(
                if (state.isWorkSessionActiveForProject) {
                    R.string.work_session_stop
                } else {
                    R.string.work_session_start
                },
            ),
        onClick =
            if (state.isWorkSessionActiveForProject) {
                callbacks.onStopWorkSession
            } else {
                callbacks.onStartWorkSession
            },
        showChevron = false,
    )
}

@Composable
private fun ProjectActionsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    SectionLabel(
        text = title,
        modifier =
            Modifier.padding(
                start = CounterDimens.ProjectActionsHorizontalPadding,
                end = CounterDimens.ProjectActionsHorizontalPadding,
                top = CounterDimens.ProjectActionsSectionTopPadding,
                bottom = CounterDimens.ProjectActionsSectionBottomPadding,
            ),
    )
    content()
}

@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    trailingCount: Int? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    badge: @Composable () -> Unit = {},
) {
    val locale = rememberCurrentLocale()
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = CounterDimens.ProjectActionsHorizontalPadding,
                    vertical = CounterDimens.ProjectActionsRowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionRowBody(label = label)
        if (trailingCount != null) {
            Text(
                text = formatIntegerForDisplay(trailingCount.toLong(), locale),
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        badge()
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = mutedColor,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(
                    horizontal = CounterDimens.ProjectActionsHorizontalPadding,
                    vertical = CounterDimens.ProjectActionsRowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionRowBody(label = label)
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
// Ilman yleisiä Material-ikoneita kuten asetuksissa: Counters ja Measurements jakoivat saman listaikonin.
private fun RowScope.ActionRowBody(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier =
            Modifier.padding(
                horizontal = CounterDimens.ProjectActionsHorizontalPadding,
                vertical = CounterDimens.ProjectActionsDividerVerticalPadding,
            ),
    )
}

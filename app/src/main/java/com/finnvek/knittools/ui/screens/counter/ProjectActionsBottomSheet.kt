package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.ui.components.ProBadge
import com.finnvek.knittools.ui.components.ProjectThumbnail
import com.finnvek.knittools.ui.components.SectionLabel
import com.finnvek.knittools.ui.components.rememberCurrentLocale
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.ProjectOverviewDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

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
)

data class ProjectActionsSheetState(
    val projectId: Long,
    val projectName: String,
    val craftType: CraftType,
    val photoUri: String?,
    val projectCounterCount: Int,
    val stitchTrackingEnabled: Boolean,
    val stitchCount: Int?,
    val proStatus: ProStatus,
    val isWorkSessionActiveForProject: Boolean,
    val isCompleted: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectActionsBottomSheet(
    state: ProjectActionsSheetState,
    callbacks: ProjectActionsSheetCallbacks,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = callbacks.onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 18.dp)) {
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
    ProjectActionsSection(title = stringResource(R.string.project_actions_section_counter_tools)) {
        ActionRow(
            icon = Icons.Outlined.FormatListNumbered,
            label = stringResource(R.string.counters),
            trailingCount = state.projectCounterCount + 1,
            onClick = callbacks.onOpenCountersList,
        )
        if (!state.isCompleted) {
            ActionRow(
                icon = Icons.Outlined.AddCircle,
                label = stringResource(R.string.add_counter),
                onClick = callbacks.onOpenAddCounter,
                showChevron = false,
                proStatus = state.proStatus,
            )
        }
        ActionRow(
            icon = Icons.Outlined.History,
            label = stringResource(R.string.counter_history_title),
            onClick = callbacks.onOpenCounterHistory,
        )
        if (!state.isCompleted) {
            ActionRow(
                icon = Icons.Outlined.Numbers,
                label = stringResource(R.string.stitches_per_row),
                trailingCount = state.stitchCount?.takeIf { it > 0 },
                onClick = callbacks.onOpenStitchCount,
            )
            SwitchRow(
                icon = Icons.Outlined.Numbers,
                label = stringResource(R.string.track_stitches),
                checked = state.stitchTrackingEnabled,
                onCheckedChange = callbacks.onToggleStitchTracking,
            )
        }
        ActionRow(
            icon = Icons.Outlined.FormatListNumbered,
            label = stringResource(R.string.measurement_title),
            onClick = callbacks.onMeasurements,
        )
        if (!state.isCompleted) {
            ActionRow(
                icon = Icons.Outlined.Restore,
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
        icon =
            if (state.isWorkSessionActiveForProject) {
                Icons.Outlined.StopCircle
            } else {
                Icons.Outlined.PlayArrow
            },
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
@Suppress("kotlin:S107") // Yhteinen toimintorivi pitää valinnaiset ulkoasu- ja tilaparametrit näkyvinä.
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingCount: Int? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    isDanger: Boolean = false,
    proStatus: ProStatus? = null,
) {
    val locale = rememberCurrentLocale()
    val contentColor =
        when {
            isDanger -> MaterialTheme.colorScheme.error
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    val mutedColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        }
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
        ActionRowBody(
            icon = icon,
            label = label,
            contentColor = contentColor,
        )
        if (trailingCount != null) {
            Text(
                text = formatIntegerForDisplay(trailingCount.toLong(), locale),
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        proStatus?.let { ProBadge(status = it, modifier = Modifier.padding(end = 8.dp)) }
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
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(
                    horizontal = CounterDimens.ProjectActionsHorizontalPadding,
                    vertical = CounterDimens.ProjectActionsRowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionRowBody(
            icon = icon,
            label = label,
            contentColor = contentColor,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
private fun RowScope.ActionRowBody(
    icon: ImageVector,
    label: String,
    contentColor: Color,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = contentColor,
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = contentColor,
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

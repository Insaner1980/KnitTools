package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnvek.knittools.R

data class ProjectActionsSheetCallbacks(
    val onDismiss: () -> Unit,
    val onOpenYarnManagement: () -> Unit,
    val onOpenNotes: () -> Unit,
    val onOpenSummary: () -> Unit,
    val onOpenPhotos: () -> Unit,
    val onOpenCountersList: () -> Unit,
    val onOpenAddCounter: () -> Unit,
    val onToggleStitchTracking: (Boolean) -> Unit,
    val onOpenSessionHistory: () -> Unit,
    val onStartRename: () -> Unit,
    val onShowResetDialog: () -> Unit,
    val onShowCompleteDialog: () -> Unit,
    val onShowDeleteDialog: () -> Unit,
)

data class ProjectActionsSheetState(
    val linkedYarnCount: Int,
    val projectCounterCount: Int,
    val stitchTrackingEnabled: Boolean,
    val isPro: Boolean,
    val isAiAvailable: Boolean,
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
        Column(modifier = Modifier.padding(bottom = 18.dp)) {
            ProjectActionsSection(title = stringResource(R.string.project_actions_section_this_project)) {
                ActionRow(
                    icon = Icons.Outlined.Inventory2,
                    label = stringResource(R.string.project_actions_yarn),
                    trailingCount = state.linkedYarnCount.takeIf { it > 0 },
                    onClick = callbacks.onOpenYarnManagement,
                )
                ActionRow(
                    icon = Icons.Outlined.EditNote,
                    label = stringResource(R.string.notes),
                    onClick = callbacks.onOpenNotes,
                )
                ActionRow(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    label = stringResource(R.string.project_actions_ai_summary),
                    onClick = callbacks.onOpenSummary,
                    enabled = state.isPro && state.isAiAvailable,
                )
                ActionRow(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.project_actions_photos),
                    onClick = callbacks.onOpenPhotos,
                )
            }

            SectionDivider()

            ProjectActionsSection(title = stringResource(R.string.project_actions_section_counters)) {
                ActionRow(
                    icon = Icons.Outlined.FormatListNumbered,
                    label = stringResource(R.string.counters),
                    trailingCount = (state.projectCounterCount + 1).takeIf { it > 0 },
                    onClick = callbacks.onOpenCountersList,
                )
                ActionRow(
                    icon = Icons.Outlined.AddCircle,
                    label = stringResource(R.string.add_counter),
                    onClick = callbacks.onOpenAddCounter,
                    showChevron = false,
                )
                SwitchRow(
                    icon = Icons.Outlined.Numbers,
                    label = stringResource(R.string.track_stitches),
                    checked = state.stitchTrackingEnabled,
                    onCheckedChange = callbacks.onToggleStitchTracking,
                )
            }

            SectionDivider()

            ProjectActionsSection(title = stringResource(R.string.project_actions_section_project_actions)) {
                ActionRow(
                    icon = Icons.Outlined.History,
                    label = stringResource(R.string.session_history_title),
                    onClick = callbacks.onOpenSessionHistory,
                )
                ActionRow(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.rename_project),
                    onClick = callbacks.onStartRename,
                    showChevron = false,
                )
                ActionRow(
                    icon = Icons.Outlined.Restore,
                    label = stringResource(R.string.reset_counter),
                    onClick = callbacks.onShowResetDialog,
                    showChevron = false,
                )
                ActionRow(
                    icon = Icons.Outlined.CheckCircle,
                    label = stringResource(R.string.complete_project),
                    onClick = callbacks.onShowCompleteDialog,
                    showChevron = false,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionRow(
                    icon = Icons.Outlined.DeleteOutline,
                    label = stringResource(R.string.delete_project),
                    onClick = callbacks.onShowDeleteDialog,
                    showChevron = false,
                    isDanger = true,
                )
            }
        }
    }
}

@Composable
private fun ProjectActionsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 8.dp),
    )
    content()
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingCount: Int? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    isDanger: Boolean = false,
) {
    val contentColor = when {
        isDanger -> MaterialTheme.colorScheme.error
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val mutedColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        if (trailingCount != null) {
            Text(
                text = trailingCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
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
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
    )
}

package com.finnvek.knittools.ui.screens.counter

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R

data class CounterQuickAction(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun CounterQuickActions(
    state: CounterUiState,
    actions: CounterWorkspaceActions,
    modifier: Modifier = Modifier,
) {
    val quickActions =
        listOf(
            CounterQuickAction(
                labelRes =
                    if (state.patternUri != null) {
                        R.string.quick_action_open_pattern
                    } else {
                        R.string.quick_action_attach_pattern
                    },
                icon = Icons.Outlined.Description,
                onClick =
                    if (state.patternUri != null) {
                        actions.onOpenPattern
                    } else {
                        actions.onShowPatternPicker
                    },
            ),
            CounterQuickAction(
                labelRes =
                    if (firstProjectNoteLine(state.notes) != null) {
                        R.string.quick_action_notes
                    } else {
                        R.string.quick_action_add_note
                    },
                icon = Icons.Outlined.EditNote,
                onClick = actions.onOpenNotes,
            ),
            CounterQuickAction(
                labelRes =
                    if (state.linkedYarns.isNotEmpty() || state.projectYarnNotes.isNotEmpty()) {
                        R.string.quick_action_yarn
                    } else {
                        R.string.quick_action_add_yarn
                    },
                icon = Icons.Outlined.Inventory2,
                onClick = actions.onOpenYarn,
            ),
            CounterQuickAction(
                labelRes =
                    if (state.latestPhotos.isNotEmpty()) {
                        R.string.quick_action_photos
                    } else {
                        R.string.quick_action_add_photo
                    },
                icon = Icons.Outlined.PhotoLibrary,
                onClick = actions.onOpenPhotos,
            ),
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterQuickActionTile(action = quickActions[0], modifier = Modifier.weight(1f))
            CounterQuickActionTile(action = quickActions[1], modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterQuickActionTile(action = quickActions[2], modifier = Modifier.weight(1f))
            CounterQuickActionTile(action = quickActions[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CounterQuickActionTile(
    action: CounterQuickAction,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .heightIn(min = 74.dp)
                .clickable(onClick = action.onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(action.labelRes),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

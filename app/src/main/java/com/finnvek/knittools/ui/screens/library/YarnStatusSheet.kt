package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.YarnCardStatus
import com.finnvek.knittools.ui.components.localizedUppercase
import com.finnvek.knittools.ui.theme.knitToolsColors

data class YarnStatusUi(
    val key: String,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarnStatusSheet(
    selectedStatus: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.status_label).localizedUppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            yarnStatusOptions().forEach { option ->
                val selected = option.key == selectedStatus
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = option.containerColor,
                                shape = RoundedCornerShape(18.dp),
                            ).selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option.key) },
                            ).heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = option.contentColor,
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = option.contentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun yarnStatusUi(status: String): YarnStatusUi =
    when (status) {
        YarnCardStatus.IN_USE -> {
            YarnStatusUi(
                key = status,
                label = stringResource(R.string.status_in_use),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }

        YarnCardStatus.FINISHED -> {
            YarnStatusUi(
                key = status,
                label = stringResource(R.string.status_finished),
                containerColor = MaterialTheme.knitToolsColors.onSurfaceMuted.copy(alpha = 0.14f),
                contentColor = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }

        else -> {
            YarnStatusUi(
                key = YarnCardStatus.IN_STASH,
                label = stringResource(R.string.status_in_stash),
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.secondary,
            )
        }
    }

@Composable
fun yarnStatusOptions(): List<YarnStatusUi> =
    listOf(
        yarnStatusUi(YarnCardStatus.IN_STASH),
        yarnStatusUi(YarnCardStatus.IN_USE),
        yarnStatusUi(YarnCardStatus.FINISHED),
    )

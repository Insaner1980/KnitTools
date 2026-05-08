package com.finnvek.knittools.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.knitToolsColors

data class YarnStatusUi(
    val key: String,
    val label: String,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
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
                text = stringResource(R.string.status_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            yarnStatusOptions().forEach { option ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = option.containerColor,
                                shape = RoundedCornerShape(18.dp),
                            ).clickable { onSelect(option.key) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = option.contentColor,
                    )
                    if (option.key == selectedStatus) {
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
        "IN_USE" -> {
            YarnStatusUi(
                key = status,
                label = stringResource(R.string.status_in_use),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }

        "FINISHED" -> {
            YarnStatusUi(
                key = status,
                label = stringResource(R.string.status_finished),
                containerColor = MaterialTheme.knitToolsColors.onSurfaceMuted.copy(alpha = 0.14f),
                contentColor = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }

        else -> {
            YarnStatusUi(
                key = "IN_STASH",
                label = stringResource(R.string.status_in_stash),
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.secondary,
            )
        }
    }

@Composable
fun yarnStatusOptions(): List<YarnStatusUi> =
    listOf(
        yarnStatusUi("IN_STASH"),
        yarnStatusUi("IN_USE"),
        yarnStatusUi("FINISHED"),
    )

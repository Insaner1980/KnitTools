package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionItem(
    startedAt: Long,
    durationMinutes: Int,
    startRow: Int,
    endRow: Int,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { SimpleDateFormat("MMM d, HH:mm", locale) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSessionDate(startedAt, dateFormat),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDuration(durationMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                onDelete?.let { delete ->
                    IconButton(onClick = delete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.session_row_range, startRow, endRow),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.knitToolsColors.onSurfaceMuted,
            )
        }
    }
}

private fun formatSessionDate(
    timestamp: Long,
    dateFormat: SimpleDateFormat,
): String = dateFormat.format(Date(timestamp))

@Composable
private fun formatDuration(minutes: Int): String =
    when {
        minutes < 60 -> stringResource(R.string.time_spent_minutes_format, minutes)
        else -> stringResource(R.string.session_duration_format, minutes / 60, minutes % 60)
    }

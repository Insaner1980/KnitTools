package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSessionDate(startedAt),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDuration(durationMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

private fun formatSessionDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Composable
private fun formatDuration(minutes: Int): String =
    when {
        minutes < 60 -> stringResource(R.string.time_spent_minutes_format, minutes)
        else -> stringResource(R.string.session_duration_format, minutes / 60, minutes % 60)
    }

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SessionItem(
    startedAt: Long,
    durationMinutes: Int,
    startRow: Int,
    endRow: Int,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val dateFormat = rememberLocaleDateFormat("MMMd", includeTime = true)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = ComponentDimens.FlatElevation,
        shadowElevation = ComponentDimens.FlatElevation,
    ) {
        Column(
            modifier = Modifier.padding(ComponentDimens.ContentPadding),
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
            Spacer(modifier = Modifier.height(ComponentDimens.CompactSpacing))
            Text(
                // "Rows 18 → 18" luki virheenä. Ilman edistystä riittää yksi rivinumero.
                text =
                    if (endRow > startRow) {
                        stringResource(R.string.session_row_range, startRow, endRow)
                    } else {
                        stringResource(R.string.session_row_single, startRow)
                    },
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

/**
 * Sama kestomuoto kuin muualla sovelluksessa. Oma apuri näytti alle tunnin istunnot
 * muodossa "36m" ja yli tunnin muodossa "1t 5min" — kaksi eri minuuttilyhennettä
 * peräkkäisillä riveillä samassa listassa.
 */
@Composable
private fun formatDuration(minutes: Int): String = durationText(DurationDisplayFormatter.fromMinutes(minutes))

package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // Hiusviivarivi kuten projektinäkymän osiot. Poisto on rivin valikossa: punainen roskakori
    // jokaisella rivillä oli helppo osua vahingossa ja korosti peruuttamatonta toimintoa.
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = ComponentDimens.StandardSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatSessionDate(startedAt, dateFormat),
                    style = MaterialTheme.typography.titleMedium,
                )
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
            Text(
                text = formatDuration(durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            onDelete?.let { SessionItemMenu(onDelete = it) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SessionItemMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                },
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

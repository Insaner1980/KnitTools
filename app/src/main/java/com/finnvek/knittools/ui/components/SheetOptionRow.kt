package com.finnvek.knittools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Sheetin vaihtoehto korttina: otsikko, valinnainen kuvaus ja oikean reunan merkki (esim. PRO).
 * Kaavan lisäys oli ääriviivapillereitä ja langan valinta kortteja, vaikka molemmat ovat samaa
 * "valitse miten jatketaan" -listaa.
 */
@Composable
fun SheetOptionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ComponentDimens.SheetOptionMinHeight)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.knitToolsColors.cardContainer,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = ComponentDimens.SheetOptionHorizontalPadding,
                    vertical = ComponentDimens.SheetOptionVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ComponentDimens.StandardSpacing),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ComponentDimens.CompactSpacing),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                body?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            badge?.invoke()
        }
    }
}

private const val DISABLED_ALPHA = 0.5f

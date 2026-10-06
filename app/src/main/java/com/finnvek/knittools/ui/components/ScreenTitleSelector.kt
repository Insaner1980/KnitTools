package com.finnvek.knittools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.finnvek.knittools.ui.theme.ComponentDimens

/**
 * Näytön otsikko, joka on samalla valitsin: valittu näkymä (Insightsin aikaväli, Projectsin kansio)
 * ja alasnuoli. Alanavigaatio kertoo jo missä ollaan, joten otsikko kertoo mitä katsotaan.
 * Kutsuja liittää omat semantiikkansa (laajenna/supista, kuvaus) [modifier]-parametrilla.
 */
@Composable
fun ScreenTitleSelector(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .clip(ComponentDimens.TitleSelectorShape)
                .clickable(enabled = enabled, role = Role.DropdownList, onClick = onClick)
                .padding(
                    horizontal = ComponentDimens.TitleSelectorHorizontalPadding,
                    vertical = ComponentDimens.TitleSelectorVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        DropdownIndicator(modifier = Modifier.size(ComponentDimens.TitleSelectorIndicatorSize))
    }
}

@Composable
internal fun DropdownIndicator(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.ArrowDropDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

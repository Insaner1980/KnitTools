package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.finnvek.knittools.ui.theme.ComponentDimens
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Laskimen syötteet yhdellä korttipinnalla. Vaalean teeman kenttä on korttia vaaleampi; suoraan
 * kermataustalla se erottui vain 1,1:1, joten laskimen kentät ovat aina tällaisen kortin sisällä.
 * [title] on ryhmän SectionLabel-otsikko, kun laskimessa on useita syöteryhmiä.
 */
@Composable
fun ToolInputCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.knitToolsColors.cardContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(ComponentDimens.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.ContentSpacing),
        ) {
            title?.let { SectionLabel(text = it) }
            content()
        }
    }
}

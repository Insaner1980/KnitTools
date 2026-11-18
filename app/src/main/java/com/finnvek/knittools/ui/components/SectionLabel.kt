package com.finnvek.knittools.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.finnvek.knittools.ui.theme.knitToolsColors

/**
 * Sovelluksen ainoa osio-otsikko: pienet versaalit `sectionLabel`-värillä (Libraryn tyyli).
 * Näkymät eivät tee omia otsikkoversioitaan; `UiConsistencySourceTest` valvoo tätä.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    SectionLabelText(text = text.localizedUppercase(), modifier = modifier)
}

/**
 * Projektin nimi osiomerkkinä: sama tyyli kuin [SectionLabel], mutta nimi säilyttää
 * käyttäjän kirjoitusasun. Projektin nimeä ei koskaan muuteta versaaleiksi.
 */
@Composable
fun ProjectNameLabel(
    name: String,
    modifier: Modifier = Modifier,
) {
    SectionLabelText(text = name, modifier = modifier)
}

@Composable
private fun SectionLabelText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.knitToolsColors.sectionLabel,
        modifier = modifier.semantics { heading() },
    )
}
